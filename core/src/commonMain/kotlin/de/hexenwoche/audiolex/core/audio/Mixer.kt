package de.hexenwoche.audiolex.core.audio

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pure PCM operations, platform-free and unit-tested (ADR-0003).
 * Everything here is deterministic; the platform sinks only output the result.
 */

/** Expands mono to stereo (or scales stereo) applying per-ear gain. */
fun PcmBuffer.toStereoWithGain(gain: StereoGain): PcmBuffer {
    val out = ShortArray(frameCount * 2)
    for (frame in 0 until frameCount) {
        val left: Short
        val right: Short
        if (channels == 1) {
            left = samples[frame]
            right = samples[frame]
        } else {
            left = samples[frame * 2]
            right = samples[frame * 2 + 1]
        }
        out[frame * 2] = (left * gain.left).roundToInt().clampToShort()
        out[frame * 2 + 1] = (right * gain.right).roundToInt().clampToShort()
    }
    return PcmBuffer(out, sampleRate, channels = 2)
}

/**
 * Adds a looped noise track to the speech signal at the given noise gain.
 * Both buffers must share sample rate and channel count; the noise loop
 * wraps around if shorter than the speech.
 */
fun mixWithNoise(speech: PcmBuffer, noiseLoop: PcmBuffer, noiseGain: Float): PcmBuffer {
    require(speech.sampleRate == noiseLoop.sampleRate) { "sample rates differ" }
    require(speech.channels == noiseLoop.channels) { "channel counts differ" }
    require(noiseLoop.samples.isNotEmpty()) { "noise loop is empty" }

    val out = ShortArray(speech.samples.size)
    for (i in speech.samples.indices) {
        val noise = noiseLoop.samples[i % noiseLoop.samples.size] * noiseGain
        out[i] = (speech.samples[i] + noise).roundToInt().clampToShort()
    }
    return PcmBuffer(out, speech.sampleRate, speech.channels)
}

/** Root mean square of the signal, basis for SNR calculations. */
fun PcmBuffer.rms(): Double {
    if (samples.isEmpty()) return 0.0
    var sum = 0.0
    for (s in samples) sum += s.toDouble() * s.toDouble()
    return sqrt(sum / samples.size)
}

/**
 * A decoded noise loop paired with its RMS (Backlog "Code-Qualität": Noise-
 * RMS einmalig berechnen statt pro Wort). The loop's samples never change
 * after loading, so [rms] is computed once here -- at construction time,
 * right after decode -- instead of being recomputed on every playback from
 * the same never-mutated [buffer]. [noiseGainForSnr] callers use this [rms]
 * in place of calling `buffer.rms()` themselves; the value and the formula
 * are unchanged, only when it's computed.
 */
data class NoiseLoop(val buffer: PcmBuffer, val rms: Double) {
    constructor(buffer: PcmBuffer) : this(buffer, buffer.rms())
}

/**
 * Gain to apply to the noise track so that speech vs. scaled noise
 * reaches the requested signal-to-noise ratio in dB.
 * SNR = 20 * log10(speechRms / (noiseRms * gain)).
 */
fun noiseGainForSnr(speechRms: Double, noiseRms: Double, snrDb: Double): Float {
    require(speechRms > 0) { "speech signal is silent" }
    require(noiseRms > 0) { "noise signal is silent" }
    return (speechRms / (noiseRms * 10.0.pow(snrDb / 20.0))).toFloat()
}

private fun Int.clampToShort(): Short =
    coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
