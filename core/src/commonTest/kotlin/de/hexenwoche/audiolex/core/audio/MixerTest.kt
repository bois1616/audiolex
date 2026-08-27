package de.hexenwoche.audiolex.core.audio

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    // ---- Kanaltest: Wortfolge je Ohr ----

    @Test
    fun concatWithGapsPutsSilenceBetweenPartsButNotAroundThem() {
        val out = concatWithGaps(listOf(mono(100, 100, rate = 1000), mono(-100, rate = 1000)), gapMillis = 3)

        // 3 ms at 1000 Hz mono = 3 silent samples, once, in the middle.
        assertEquals(listOf<Short>(100, 100, 0, 0, 0, -100), out.samples.toList())
        assertEquals(1000, out.sampleRate)
        assertEquals(1, out.channels)
    }

    @Test
    fun concatWithGapsOfOnePartIsThatPart() {
        val out = concatWithGaps(listOf(mono(7, -7, rate = 1000)), gapMillis = 500)
        assertEquals(listOf<Short>(7, -7), out.samples.toList())
    }

    @Test
    fun concatWithGapsRejectsMismatchedSampleRates() {
        assertFailsWith<IllegalArgumentException> {
            concatWithGaps(listOf(mono(1, rate = 1000), mono(1, rate = 2000)), gapMillis = 0)
        }
    }

    @Test
    fun perEarStereoKeepsEachSequenceOnItsOwnEar() {
        val out = perEarStereo(mono(100, 200), mono(-100, -200), StereoGain.BOTH)

        assertEquals(2, out.channels)
        assertEquals(listOf<Short>(100, -100, 200, -200), out.samples.toList())
    }

    @Test
    fun perEarStereoLeftOnlyLeavesExactlyZeroOnTheRightChannel() {
        // The claim the channel test exists to prove: on the silenced ear the
        // app sends nothing at all, not merely something quieter. Anything
        // still audible there is downstream of this function.
        val out = perEarStereo(mono(30_000, -30_000), mono(30_000, -30_000), StereoGain.LEFT_ONLY)

        assertEquals(listOf<Short>(30_000, 0, -30_000, 0), out.samples.toList())
    }

    @Test
    fun perEarStereoPadsTheShorterSideWithSilence() {
        val out = perEarStereo(mono(100, 200, 300), mono(-100), StereoGain.BOTH)

        assertEquals(listOf<Short>(100, -100, 200, 0, 300, 0), out.samples.toList())
    }

    @Test
    fun perEarStereoRejectsStereoInput() {
        val stereo = PcmBuffer(shortArrayOf(1, 1), 48_000, channels = 2)
        assertFailsWith<IllegalArgumentException> {
            perEarStereo(stereo, mono(1), StereoGain.BOTH)
        }
    }
}
