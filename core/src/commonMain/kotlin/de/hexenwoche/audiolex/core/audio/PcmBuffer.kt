package de.hexenwoche.audiolex.core.audio

/** Interleaved 16-bit PCM audio. */
class PcmBuffer(
    val samples: ShortArray,
    val sampleRate: Int,
    val channels: Int,
) {
    init {
        require(channels in 1..2) { "only mono or stereo supported, got $channels" }
        require(samples.size % channels == 0) { "sample count must be a multiple of channel count" }
    }

    val frameCount: Int get() = samples.size / channels
}

/**
 * Returns a copy with [millis] of leading silence prepended (zero samples),
 * same sample rate and channel layout. Used to absorb the first few
 * milliseconds a cold Bluetooth audio path drops while its codec/link wakes
 * up: without a silent lead-in those dropped millis eat the start of the
 * word itself ("Telefon" -> "fon" after a pause, Autor-Finding 2026-07-13).
 * The silence carries no word information, so losing it is inaudible, while
 * a warm path simply plays the (brief) silence and then the full word.
 * [millis] <= 0 returns the buffer unchanged.
 */
fun PcmBuffer.withLeadingSilence(millis: Int): PcmBuffer {
    if (millis <= 0) return this
    val silenceFrames = sampleRate * millis / 1000
    val silenceSamples = silenceFrames * channels
    val padded = ShortArray(silenceSamples + samples.size)
    samples.copyInto(padded, destinationOffset = silenceSamples)
    return PcmBuffer(padded, sampleRate, channels)
}

/**
 * Downmixes to mono by averaging channels frame-by-frame (Backlog M4
 * "Störgeräusch-Overlay", ADR-0010 point 4): a defensive guard, not a
 * feature -- the bundled noise loops are already mono at build time
 * (ffmpeg `-ac 1`), this only catches a scenario file that accidentally
 * ships stereo, so [mixWithNoise]'s channel-count check still passes
 * instead of throwing. Already-mono buffers are returned unchanged.
 */
fun PcmBuffer.toMono(): PcmBuffer {
    if (channels == 1) return this
    val out = ShortArray(frameCount)
    for (frame in 0 until frameCount) {
        var sum = 0
        for (ch in 0 until channels) sum += samples[frame * channels + ch]
        out[frame] = (sum / channels).toShort()
    }
    return PcmBuffer(out, sampleRate, channels = 1)
}

/**
 * Per-ear gain, the core control for unilateral hearing training:
 * left only, right only, or both — each with its own level (0.0–1.0+).
 */
data class StereoGain(val left: Float, val right: Float) {
    companion object {
        val BOTH = StereoGain(1f, 1f)
        val LEFT_ONLY = StereoGain(1f, 0f)
        val RIGHT_ONLY = StereoGain(0f, 1f)
    }
}
