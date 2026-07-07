package de.hexenwoche.audiolex.core.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

actual fun createAudioSink(): AudioSink = JavaSoundAudioSink()

/**
 * Desktop sink for development on WSL2/Linux (audio via WSLg/PulseAudio).
 * Approximation only — hearing-relevant verification happens on the device.
 */
private class JavaSoundAudioSink : AudioSink {
    override fun play(buffer: PcmBuffer) {
        val format = AudioFormat(
            buffer.sampleRate.toFloat(),
            /* sampleSizeInBits = */ 16,
            buffer.channels,
            /* signed = */ true,
            /* bigEndian = */ false,
        )
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
}
