package de.hexenwoche.audiolex.core.audio

import java.io.File
import java.io.RandomAccessFile
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

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
    override fun play(buffer: PcmBuffer) {
        if (paplayAvailable) {
            playViaPaplay(buffer)
        } else {
            playViaJavaSound(buffer)
        }
    }

    private fun playViaPaplay(buffer: PcmBuffer) {
        val tempFile = File.createTempFile("audiolex-", ".wav")
        try {
            writeWavFile(buffer, tempFile)
            val exitCode = ProcessBuilder("paplay", tempFile.absolutePath)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
                .waitFor()
            check(exitCode == 0) { "paplay exited with code $exitCode" }
        } finally {
            tempFile.delete()
        }
    }

    private fun playViaJavaSound(buffer: PcmBuffer) {
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
            line.write(bytes, 0, bytes.size)
            line.drain()
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

/** Minimal canonical PCM16 WAV writer -- the mirror of [WavFile.decode]. */
internal fun writeWavFile(buffer: PcmBuffer, file: File) {
    val dataSize = buffer.samples.size * 2
    val blockAlign = buffer.channels * 2
    val byteRate = buffer.sampleRate * blockAlign

    RandomAccessFile(file, "rw").use { out ->
        fun writeAscii(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
        fun writeLe32(v: Int) {
            out.write(v and 0xFF)
            out.write((v shr 8) and 0xFF)
            out.write((v shr 16) and 0xFF)
            out.write((v shr 24) and 0xFF)
        }
        fun writeLe16(v: Int) {
            out.write(v and 0xFF)
            out.write((v shr 8) and 0xFF)
        }

        writeAscii("RIFF")
        writeLe32(36 + dataSize)
        writeAscii("WAVE")

        writeAscii("fmt ")
        writeLe32(16)
        writeLe16(1) // PCM
        writeLe16(buffer.channels)
        writeLe32(buffer.sampleRate)
        writeLe32(byteRate)
        writeLe16(blockAlign)
        writeLe16(16) // bits per sample

        writeAscii("data")
        writeLe32(dataSize)
        for (sample in buffer.samples) {
            writeLe16(sample.toInt() and 0xFFFF)
        }
    }
}
