package de.hexenwoche.audiolex.core.audio

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Round-trip test: our own writer (used by the desktop paplay sink) against our own reader. */
class WavFileWriterTest {

    @Test
    fun writeThenDecodeRoundTripsMono() {
        val original = PcmBuffer(shortArrayOf(0, 100, -100, Short.MAX_VALUE, Short.MIN_VALUE), 22050, 1)
        val file = File.createTempFile("wav-writer-test-", ".wav")
        try {
            writeWavFile(original, file)
            val decoded = WavFile.decode(file.readBytes())

            assertEquals(original.sampleRate, decoded.sampleRate)
            assertEquals(original.channels, decoded.channels)
            assertEquals(original.samples.toList(), decoded.samples.toList())
        } finally {
            file.delete()
        }
    }

    @Test
    fun writeThenDecodeRoundTripsStereo() {
        val original = PcmBuffer(shortArrayOf(100, -100, 200, -200), 48000, 2)
        val file = File.createTempFile("wav-writer-test-", ".wav")
        try {
            writeWavFile(original, file)
            val decoded = WavFile.decode(file.readBytes())

            assertEquals(2, decoded.channels)
            assertEquals(original.samples.toList(), decoded.samples.toList())
        } finally {
            file.delete()
        }
    }

    @Test
    fun writtenFileHasExpectedByteSize() {
        val samples = ShortArray(100) { it.toShort() }
        val original = PcmBuffer(samples, 22050, 1)
        val file = File.createTempFile("wav-writer-test-", ".wav")
        try {
            writeWavFile(original, file)
            // 44-byte canonical header + 2 bytes per sample
            assertTrue(file.length() == 44L + samples.size * 2)
        } finally {
            file.delete()
        }
    }
}
