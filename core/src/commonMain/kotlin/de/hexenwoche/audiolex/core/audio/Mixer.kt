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

/**
 * Joins [parts] into one buffer with [gapMillis] of silence between
 * consecutive parts -- never before the first or after the last, so the
 * result starts and ends exactly where the audio does.
 *
 * Built for the channel test (Autor-Auftrag 2026-08-27, F-Droid-Tester
 * chivalry): a single word is over in half a second, which is too short to
 * tell *which* ear it came from when the answer is the thing in question.
 * Three words in a row give the ear a few seconds to be sure. The gaps are
 * what keeps them three words instead of one slur.
 */
fun concatWithGaps(parts: List<PcmBuffer>, gapMillis: Int): PcmBuffer {
    require(parts.isNotEmpty()) { "nothing to concatenate" }
    val first = parts.first()
    require(parts.all { it.sampleRate == first.sampleRate }) { "sample rates differ" }
    require(parts.all { it.channels == first.channels }) { "channel counts differ" }

    val gapSamples = first.sampleRate * gapMillis.coerceAtLeast(0) / 1000 * first.channels
    val out = ShortArray(parts.sumOf { it.samples.size } + gapSamples * (parts.size - 1))
    var offset = 0
    for ((index, part) in parts.withIndex()) {
        if (index > 0) offset += gapSamples
        part.samples.copyInto(out, offset)
        offset += part.samples.size
    }
    return PcmBuffer(out, first.sampleRate, first.channels)
}

/**
 * Puts [left] into the left ear and [right] into the right ear *at the same
 * time*, then applies [gain] on top -- so [StereoGain.LEFT_ONLY] leaves
 * exactly zero on the right channel, and what the remaining ear hears is a
 * different sound than the silenced one would have carried.
 *
 * That difference is the point (backlog M1, channel-separation smoke test):
 * with one hearing aid on one ear, "which words do I hear" is a far clearer
 * signal than "is this louder or quieter than before". The shorter input is
 * padded with silence, so a mismatched pair still lines up frame for frame.
 */
fun perEarStereo(left: PcmBuffer, right: PcmBuffer, gain: StereoGain): PcmBuffer {
    require(left.sampleRate == right.sampleRate) { "sample rates differ" }
    require(left.channels == 1 && right.channels == 1) { "expected mono inputs" }

    val frameCount = maxOf(left.frameCount, right.frameCount)
    val out = ShortArray(frameCount * 2)
    for (frame in 0 until frameCount) {
        val leftSample = left.samples.getOrElse(frame) { 0 }
        val rightSample = right.samples.getOrElse(frame) { 0 }
        out[frame * 2] = (leftSample * gain.left).roundToInt().clampToShort()
        out[frame * 2 + 1] = (rightSample * gain.right).roundToInt().clampToShort()
    }
    return PcmBuffer(out, left.sampleRate, channels = 2)
}
