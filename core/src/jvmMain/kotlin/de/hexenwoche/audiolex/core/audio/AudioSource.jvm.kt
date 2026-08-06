package de.hexenwoche.audiolex.core.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

actual fun createAudioSource(): AudioSource = JavaSoundAudioSource()

/**
 * Desktop source for development. Unlike the playback sink (ADR-0003), there
 * is no shell-out fallback here: `paplay` plays a finished file, but capture
 * needs a live stream, and there is no equivalent "record a WAV via one
 * external command, then read it back" trick that fits this interface's
 * chunk-callback contract. javax.sound.sampled's ALSA backend is the one
 * that found no line at all for *playback* under WSL2
 * (`AudioSystem.getMixerInfo()` empty, see `AudioSink.jvm.kt`); the same
 * limitation is expected for capture. Rather than crash, an unsupported line
 * surfaces as a clear [IllegalStateException] -- Android is this feature's
 * actual target platform (ADR-0012), the desktop path is dev-target best
 * effort (ADR-0003).
 */
private class JavaSoundAudioSource : AudioSource {
    override suspend fun record(onChunk: (ShortArray) -> Unit) {
        val format = AudioFormat(RECORDING_SAMPLE_RATE.toFloat(), 16, RECORDING_CHANNELS, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        check(AudioSystem.isLineSupported(info)) {
            "no microphone input line available for $RECORDING_SAMPLE_RATE Hz mono PCM16 -- " +
                "expected gap under WSL2 (ADR-0003), Android is this feature's target platform"
        }
        val line = AudioSystem.getLine(info) as TargetDataLine
        try {
            line.open(format)
            line.start()
            val chunkBytes = ByteArray(maxOf(line.bufferSize / 4, 2048))
            try {
                while (true) {
                    // Same reasoning as the Android actual: line.read() is a
                    // plain blocking call, not a suspension point, so
                    // cancellation is only observed at this explicit check
                    // between reads -- bounded by one chunk's worth of audio.
                    currentCoroutineContext().ensureActive()
                    val bytesRead = line.read(chunkBytes, 0, chunkBytes.size)
                    if (bytesRead > 0) {
                        onChunk(chunkBytes.toPcm16LittleEndian(bytesRead))
                    }
                }
            } finally {
                line.stop()
            }
        } finally {
            line.close()
        }
    }

    override fun close() = Unit
}

/**
 * Decodes the first [byteCount] bytes of this little-endian PCM16 buffer
 * into samples -- the mirror of how [WavFile.decode] reads sample bytes,
 * just off a live line instead of a file. `internal`, not `private`, so
 * [de.hexenwoche.audiolex.core.audio.AudioSourceJvmTest] can exercise this
 * pure conversion directly without a real audio device.
 */
internal fun ByteArray.toPcm16LittleEndian(byteCount: Int): ShortArray {
    val samples = ShortArray(byteCount / 2)
    for (i in samples.indices) {
        val lo = this[i * 2].toInt() and 0xFF
        val hi = this[i * 2 + 1].toInt()
        samples[i] = ((hi shl 8) or lo).toShort()
    }
    return samples
}
