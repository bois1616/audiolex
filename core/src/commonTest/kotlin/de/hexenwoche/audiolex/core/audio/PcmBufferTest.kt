package de.hexenwoche.audiolex.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PcmBufferTest {

    private fun mono(vararg samples: Int, rate: Int = 48_000) =
        PcmBuffer(ShortArray(samples.size) { samples[it].toShort() }, rate, channels = 1)

    private fun stereo(vararg samples: Int, rate: Int = 48_000) =
        PcmBuffer(ShortArray(samples.size) { samples[it].toShort() }, rate, channels = 2)

    @Test
    fun leadingSilencePrependsZeroFramesForMono() {
        // 48_000 Hz, 10 ms -> 480 silent frames = 480 mono samples.
        val out = mono(100, -200, 300).withLeadingSilence(10)
        assertEquals(480 + 3, out.samples.size)
        assertEquals(List(480) { 0.toShort() }, out.samples.take(480))
        assertEquals(listOf<Short>(100, -200, 300), out.samples.drop(480))
    }

    @Test
    fun leadingSilenceScalesFramesByChannelCount() {
        // Stereo: 480 silent frames = 960 samples, original appended after.
        val out = stereo(1, 2, 3, 4).withLeadingSilence(10)
        assertEquals(960 + 4, out.samples.size)
        assertEquals(listOf<Short>(1, 2, 3, 4), out.samples.drop(960))
        assertEquals(2, out.channels)
    }

    @Test
    fun leadingSilenceKeepsRateAndChannels() {
        val out = mono(1, rate = 22_050).withLeadingSilence(5)
        assertEquals(22_050, out.sampleRate)
        assertEquals(1, out.channels)
        // 22_050 Hz, 5 ms -> 110 frames (integer division), + 1 original.
        assertEquals(110 + 1, out.samples.size)
    }

    @Test
    fun leadingSilenceOfZeroReturnsSameBuffer() {
        val buffer = mono(1, 2, 3)
        assertSame(buffer, buffer.withLeadingSilence(0))
    }

    @Test
    fun leadingSilenceOfNegativeReturnsSameBuffer() {
        val buffer = mono(1, 2, 3)
        assertSame(buffer, buffer.withLeadingSilence(-50))
    }

    @Test
    fun toMonoAveragesStereoChannelsPerFrame() {
        // Frame 1: (100, 300) -> 200; frame 2: (-100, -300) -> -200.
        val out = stereo(100, 300, -100, -300).toMono()
        assertEquals(1, out.channels)
        assertEquals(listOf<Short>(200, -200), out.samples.toList())
    }

    @Test
    fun toMonoOnAlreadyMonoBufferReturnsSameInstance() {
        val buffer = mono(1, 2, 3)
        assertSame(buffer, buffer.toMono())
    }
}
