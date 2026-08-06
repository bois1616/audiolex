package de.hexenwoche.audiolex.core.audio

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MixerTest {

    private fun mono(vararg samples: Int, rate: Int = 48_000) =
        PcmBuffer(ShortArray(samples.size) { samples[it].toShort() }, rate, channels = 1)

    @Test
    fun monoExpandsToStereoDuplicatingSamples() {
        val out = mono(100, -200).toStereoWithGain(StereoGain.BOTH)
        assertEquals(2, out.channels)
        assertEquals(listOf<Short>(100, 100, -200, -200), out.samples.toList())
    }

    @Test
    fun leftOnlySilencesRightEar() {
        val out = mono(1000).toStereoWithGain(StereoGain.LEFT_ONLY)
        assertEquals(1000, out.samples[0].toInt())
        assertEquals(0, out.samples[1].toInt())
    }

    @Test
    fun perEarGainScalesIndependently() {
        val out = mono(1000).toStereoWithGain(StereoGain(left = 0.5f, right = 0.25f))
        assertEquals(500, out.samples[0].toInt())
        assertEquals(250, out.samples[1].toInt())
    }

    @Test
    fun gainClampsInsteadOfOverflowing() {
        val out = mono(30_000).toStereoWithGain(StereoGain(left = 2f, right = 2f))
        assertEquals(Short.MAX_VALUE, out.samples[0])
    }

    @Test
    fun noiseLoopsAndMixesAdditively() {
        val speech = mono(100, 100, 100, 100)
        val noise = mono(10, -10) // shorter than speech, must wrap
        val out = mixWithNoise(speech, noise, noiseGain = 1f)
        assertEquals(listOf<Short>(110, 90, 110, 90), out.samples.toList())
    }

    @Test
    fun mixClampsAtShortRange() {
        val speech = mono(32_000)
        val noise = mono(32_000)
        val out = mixWithNoise(speech, noise, noiseGain = 1f)
        assertEquals(Short.MAX_VALUE, out.samples[0])
    }

    @Test
    fun rmsOfConstantSignalIsItsAmplitude() {
        assertEquals(1000.0, mono(1000, -1000, 1000, -1000).rms(), absoluteTolerance = 0.001)
    }

    @Test
    fun zeroDbSnrMatchesNoiseToSpeechLevel() {
        val gain = noiseGainForSnr(speechRms = 1000.0, noiseRms = 500.0, snrDb = 0.0)
        assertEquals(2.0f, gain, absoluteTolerance = 0.001f)
    }

    @Test
    fun positiveSnrReducesNoiseGain() {
        // +20 dB SNR: noise must end up at a tenth of the speech RMS.
        val gain = noiseGainForSnr(speechRms = 1000.0, noiseRms = 1000.0, snrDb = 20.0)
        assertTrue(abs(gain - 0.1f) < 0.001f)
    }

    @Test
    fun noiseLoopPrecomputesSameRmsAsCallingRmsDirectly() {
        val buffer = mono(10, -10, 20, -20)
        val loop = NoiseLoop(buffer)
        assertEquals(buffer.rms(), loop.rms)
    }

    @Test
    fun mixingWithPrecomputedNoiseLoopRmsMatchesMixingWithLiveRms() {
        // Regression for "Noise-RMS einmalig berechnen statt pro Wort": a mix
        // using NoiseLoop's precomputed rms must be bit-identical to the old
        // path of calling noiseBuffer.rms() fresh at mix time -- same buffer,
        // same formula, only the *when* of the rms() call differs.
        val speech = mono(500, -500, 500, -500)
        val noise = mono(10, -10)
        val loop = NoiseLoop(noise)

        val gainFromLoop = noiseGainForSnr(speech.rms(), loop.rms, snrDb = 6.0)
        val gainLive = noiseGainForSnr(speech.rms(), noise.rms(), snrDb = 6.0)
        assertEquals(gainLive, gainFromLoop)

        val mixedFromLoop = mixWithNoise(speech, loop.buffer, gainFromLoop)
        val mixedLive = mixWithNoise(speech, noise, gainLive)
        assertEquals(mixedLive.samples.toList(), mixedFromLoop.samples.toList())
    }
}
