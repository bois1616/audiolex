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
