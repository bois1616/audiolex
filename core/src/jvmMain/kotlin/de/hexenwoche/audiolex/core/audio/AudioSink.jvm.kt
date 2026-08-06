package de.hexenwoche.audiolex.core.audio

import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlinx.coroutines.runInterruptible

actual fun createAudioSink(): AudioSink = JavaSoundAudioSink()

/**
 * Desktop sink for development (WSLg/PulseAudio on WSL2, or a native
 * Linux/macOS desktop). Approximation only — hearing-relevant verification
 * happens on the device.
 *
 * Prefers shelling out to `paplay`: under WSL2, Java Sound's ALSA backend
 * finds no line at all (`AudioSystem.getMixerInfo()` is empty even though
 * PulseAudio itself works fine via WSLg's RDP audio bridge -- verified
 * manually with `paplay` against the real corpus files). Falls back to
 * javax.sound.sampled where `paplay` isn't on PATH (e.g. macOS, or a
 * desktop with a working ALSA setup).
 */
private class JavaSoundAudioSink : AudioSink {
    override suspend fun play(buffer: PcmBuffer) {
        if (paplayAvailable) {
            playViaPaplay(buffer)
        } else {
            playViaJavaSound(buffer)
        }
    }

    private suspend fun playViaPaplay(buffer: PcmBuffer) {
        val tempFile = File.createTempFile("audiolex-", ".wav")
        val process = try {
            writeWavFile(buffer, tempFile)
            ProcessBuilder("paplay", tempFile.absolutePath)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
        try {
            // runInterruptible moves the blocking waitFor() onto a thread
            // that reacts to coroutine cancellation; destroy() below then
            // actually kills the external process instead of leaving it to
            // finish playing in the background after we've "cancelled".
            val exitCode = runInterruptible { process.waitFor() }
            check(exitCode == 0) { "paplay exited with code $exitCode" }
        } finally {
            process.destroy()
            tempFile.delete()
        }
    }

    private suspend fun playViaJavaSound(buffer: PcmBuffer) {
        val format = AudioFormat(buffer.sampleRate.toFloat(), 16, buffer.channels, true, false)
        val bytes = ByteArray(buffer.samples.size * 2)
        buffer.samples.forEachIndexed { i, sample ->
            bytes[i * 2] = (sample.toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (sample.toInt() shr 8).toByte()
        }
        val line = AudioSystem.getSourceDataLine(format)
        try {
            line.open(format)
            line.start()
            // runInterruptible so cancelling the caller stops the line
            // below instead of waiting for drain() to finish on its own.
            runInterruptible {
                line.write(bytes, 0, bytes.size)
                line.drain()
            }
        } finally {
            line.close()
        }
    }

    override fun close() = Unit

    private companion object {
        val paplayAvailable: Boolean by lazy {
            try {
                ProcessBuilder("paplay", "--version")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .waitFor() == 0
            } catch (e: Exception) {
                false
            }
        }
    }
}

/**
 * Writes [buffer] to [file] as a WAV file. Thin JVM wrapper around
 * [WavFile.encode] (ADR-0012 point 3: the actual encoding moved to
 * `commonMain` so Android can reuse it too) -- kept as its own function so
 * this call site in [playViaPaplay] didn't have to change.
 */
internal fun writeWavFile(buffer: PcmBuffer, file: File) {
    file.writeBytes(WavFile.encode(buffer))
}
