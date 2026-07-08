package de.hexenwoche.audiolex.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * swapStereoChannels itself is not applied by default (see AudioSink.android.kt
 * doc: the earlier "fix" was based on contaminated evidence and the Galaxy
 * A53 was re-tested as correct without it). These tests just cover the
 * function's own swap arithmetic, kept as groundwork for a possible future
 * "Kanäle tauschen" setting.
 */
class AudioSinkAndroidTest {

    @Test
    fun swapsLeftAndRightSamples() {
        val original = PcmBuffer(shortArrayOf(100, -100, 200, -200), 22050, channels = 2)
        val swapped = original.swapStereoChannels()
        assertEquals(listOf<Short>(-100, 100, -200, 200), swapped.samples.toList())
    }

    @Test
    fun swapPreservesSampleRateAndChannelCount() {
        val original = PcmBuffer(shortArrayOf(1, 2), 48000, channels = 2)
        val swapped = original.swapStereoChannels()
        assertEquals(48000, swapped.sampleRate)
        assertEquals(2, swapped.channels)
    }

    @Test
    fun rejectsMonoInput() {
        val mono = PcmBuffer(shortArrayOf(1, 2, 3), 22050, channels = 1)
        assertFailsWith<IllegalArgumentException> { mono.swapStereoChannels() }
    }
}
