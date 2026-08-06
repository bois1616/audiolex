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

    @Test
    fun rejectsChunkSizeExceedingFileBounds() {
        val wav = buildWav(sampleRate = 22050, channels = 1, bitsPerSample = 16, samples = shortArrayOf(1, 2))
        // Corrupt the "data" chunk's size field (last 4 bytes before the payload)
        // to claim far more data than the file actually contains.
        val dataSizeOffset = wav.size - 4 - 4
        wav[dataSizeOffset] = 0xFF.toByte()
        wav[dataSizeOffset + 1] = 0xFF.toByte()
        wav[dataSizeOffset + 2] = 0x7F.toByte()
        wav[dataSizeOffset + 3] = 0x00.toByte()

        assertFailsWith<IllegalArgumentException> { WavFile.decode(wav) }
    }

    @Test
    fun rejectsNegativeChunkSize() {
        val wav = buildWav(sampleRate = 22050, channels = 1, bitsPerSample = 16, samples = shortArrayOf(1, 2))
        val dataSizeOffset = wav.size - 4 - 4
        wav[dataSizeOffset] = 0xFF.toByte()
        wav[dataSizeOffset + 1] = 0xFF.toByte()
        wav[dataSizeOffset + 2] = 0xFF.toByte()
        wav[dataSizeOffset + 3] = 0xFF.toByte()

        assertFailsWith<IllegalArgumentException> { WavFile.decode(wav) }
    }

    @Test
    fun encodeThenDecodeRoundTripsMono() {
        val original = PcmBuffer(shortArrayOf(0, 100, -100, Short.MAX_VALUE, Short.MIN_VALUE), 22050, 1)
        val decoded = WavFile.decode(WavFile.encode(original))

        assertEquals(original.sampleRate, decoded.sampleRate)
        assertEquals(original.channels, decoded.channels)
        assertEquals(original.samples.toList(), decoded.samples.toList())
    }

    @Test
    fun encodeThenDecodeRoundTripsStereo() {
        val original = PcmBuffer(shortArrayOf(100, -100, 200, -200), 48000, 2)
        val decoded = WavFile.decode(WavFile.encode(original))

        assertEquals(2, decoded.channels)
        assertEquals(original.samples.toList(), decoded.samples.toList())
    }

    @Test
    fun encodedByteSizeIsCanonicalHeaderPlusData() {
        val samples = ShortArray(100) { it.toShort() }
        val bytes = WavFile.encode(PcmBuffer(samples, 22050, 1))
        // 44-byte canonical header + 2 bytes per sample, same shape decode() expects.
        assertEquals(44 + samples.size * 2, bytes.size)
    }

    @Test
    fun encodedBytesCarryRiffWaveFmtDataMagicAtExpectedOffsets() {
        val bytes = WavFile.encode(PcmBuffer(shortArrayOf(1, 2, 3), 22050, 1))
        fun ascii(offset: Int, length: Int) =
            buildString { for (i in 0 until length) append(bytes[offset + i].toInt().toChar()) }

        assertEquals("RIFF", ascii(0, 4))
        assertEquals("WAVE", ascii(8, 4))
        assertEquals("fmt ", ascii(12, 4))
        assertEquals("data", ascii(36, 4))
    }

    @Test
    fun rejectsTruncatedFmtChunk() {
        val bytes = ArrayList<Byte>()
        fun ascii(s: String) = s.forEach { bytes.add(it.code.toByte()) }
        fun le32(v: Int) {
            bytes.add((v and 0xFF).toByte())
            bytes.add(((v shr 8) and 0xFF).toByte())
            bytes.add(((v shr 16) and 0xFF).toByte())
            bytes.add(((v shr 24) and 0xFF).toByte())
        }
        ascii("RIFF")
        le32(0)
        ascii("WAVE")
        ascii("fmt ")
        le32(4) // too small: real fmt chunk needs at least 16 bytes
        bytes.add(1); bytes.add(0); bytes.add(1); bytes.add(0)

        assertFailsWith<IllegalArgumentException> { WavFile.decode(bytes.toByteArray()) }
    }
}
