package de.hexenwoche.audiolex.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Verified fix (backlog M1): on a Galaxy A53, standard L/R-interleaved
 * stereo PCM played out of the wrong ear, confirmed independently over
 * both a Bluetooth hearing aid and a wired USB-C headset. swapStereoChannels
 * corrects this before AudioTrack playback (see AudioSink.android.kt).
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
