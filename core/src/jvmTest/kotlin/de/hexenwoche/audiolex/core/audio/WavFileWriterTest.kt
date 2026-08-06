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
    fun writeThenDecodeRoundTripsRecordingFormat() {
        // Guards the contract this batch actually cares about (ADR-0012
        // point 3): whatever AudioSource.record() produces must survive a
        // WAV round-trip in exactly the fixed recording format, not just
        // "some" mono/stereo combination -- tied to the same constants
        // AudioSource.kt exports so the two can't silently drift apart.
        val samples = ShortArray(500) { (it - 250).toShort() }
        val original = PcmBuffer(samples, RECORDING_SAMPLE_RATE, RECORDING_CHANNELS)
        val file = File.createTempFile("wav-writer-recording-format-", ".wav")
        try {
            writeWavFile(original, file)
            val decoded = WavFile.decode(file.readBytes())

            assertEquals(RECORDING_SAMPLE_RATE, decoded.sampleRate)
            assertEquals(RECORDING_CHANNELS, decoded.channels)
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
