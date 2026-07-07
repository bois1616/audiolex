package de.hexenwoche.audiolex.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WavFileTest {

    /** Builds a minimal canonical WAV: RIFF/WAVE, fmt chunk, then data chunk. */
    private fun buildWav(
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        samples: ShortArray,
        audioFormat: Int = 1,
        withPadChunk: Boolean = false,
    ): ByteArray {
        val blockAlign = channels * bitsPerSample / 8
        val byteRate = sampleRate * blockAlign
        val dataBytes = samples.size * 2

        val bytes = ArrayList<Byte>()
        fun ascii(s: String) = s.forEach { bytes.add(it.code.toByte()) }
        fun le16(v: Int) {
            bytes.add((v and 0xFF).toByte())
            bytes.add(((v shr 8) and 0xFF).toByte())
        }
        fun le32(v: Int) {
            bytes.add((v and 0xFF).toByte())
            bytes.add(((v shr 8) and 0xFF).toByte())
            bytes.add(((v shr 16) and 0xFF).toByte())
            bytes.add(((v shr 24) and 0xFF).toByte())
        }

        ascii("RIFF")
        le32(0) // placeholder, not validated by the reader
        ascii("WAVE")

        if (withPadChunk) {
            ascii("LIST")
            le32(4)
            ascii("INFO")
        }

        ascii("fmt ")
        le32(16)
        le16(audioFormat)
        le16(channels)
        le32(sampleRate)
        le32(byteRate)
        le16(blockAlign)
        le16(bitsPerSample)

        ascii("data")
        le32(dataBytes)
        for (s in samples) le16(s.toInt() and 0xFFFF)

        return bytes.toByteArray()
    }

    @Test
    fun decodesBasicMonoPcm16() {
        val samples = shortArrayOf(0, 100, -100, Short.MAX_VALUE, Short.MIN_VALUE)
        val wav = buildWav(sampleRate = 22050, channels = 1, bitsPerSample = 16, samples = samples)

        val buffer = WavFile.decode(wav)

        assertEquals(22050, buffer.sampleRate)
        assertEquals(1, buffer.channels)
        assertEquals(samples.toList(), buffer.samples.toList())
    }

    @Test
    fun decodesStereoPcm16() {
        val samples = shortArrayOf(100, -100, 200, -200)
        val wav = buildWav(sampleRate = 48000, channels = 2, bitsPerSample = 16, samples = samples)

        val buffer = WavFile.decode(wav)

        assertEquals(2, buffer.channels)
        assertEquals(2, buffer.frameCount)
        assertEquals(samples.toList(), buffer.samples.toList())
    }

    @Test
    fun skipsUnknownChunksBeforeData() {
        val samples = shortArrayOf(1, 2, 3, 4)
        val wav = buildWav(
            sampleRate = 16000, channels = 1, bitsPerSample = 16,
            samples = samples, withPadChunk = true,
        )

        val buffer = WavFile.decode(wav)

        assertEquals(16000, buffer.sampleRate)
        assertEquals(samples.toList(), buffer.samples.toList())
    }

    @Test
    fun rejectsNonPcmFormat() {
        val wav = buildWav(
            sampleRate = 22050, channels = 1, bitsPerSample = 16,
            samples = shortArrayOf(1, 2), audioFormat = 3, // IEEE float
        )
        assertFailsWith<IllegalArgumentException> { WavFile.decode(wav) }
    }

    @Test
    fun rejectsNon16BitDepth() {
        val wav = buildWav(sampleRate = 22050, channels = 1, bitsPerSample = 8, samples = shortArrayOf(1, 2))
        assertFailsWith<IllegalArgumentException> { WavFile.decode(wav) }
    }

    @Test
    fun rejectsMissingRiffHeader() {
        val bytes = "NOTRIFFHEADERDATAHERE".encodeToByteArray()
        assertFailsWith<IllegalArgumentException> { WavFile.decode(bytes) }
    }

    @Test
    fun rejectsTruncatedFile() {
        assertFailsWith<IllegalArgumentException> { WavFile.decode(ByteArray(4)) }
    }
}
