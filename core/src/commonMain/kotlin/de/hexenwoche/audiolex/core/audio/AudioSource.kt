package de.hexenwoche.audiolex.core.audio

/**
 * Thin platform boundary (ADR-0003), the input-side mirror of [AudioSink]:
 * captures raw microphone audio, chunk by chunk, until cancelled. No
 * business logic below this interface -- assembling the chunks into a
 * finished [PcmBuffer] is the caller's job, the same layering as
 * [AudioSink.play] taking an already-built buffer rather than a stream.
 *
 * The recording format is fixed, not a parameter (ADR-0012 point 3):
 * [RECORDING_SAMPLE_RATE] Hz mono PCM16, the same format the TTS corpus
 * uses, because `mixWithNoise` requires matching sample rate and channel
 * count and resampling is explicitly out of scope (ADR-0010). Each `actual`
 * must verify at runtime that the platform actually delivered this format
 * (ADR-0012 Konsequenzen) instead of silently accepting a different one --
 * a mismatch is a failure to surface, not a case to resample or downgrade.
 */
interface AudioSource : AutoCloseable {
    /**
     * Captures audio, invoking [onChunk] with each newly captured slice of
     * PCM16 mono [RECORDING_SAMPLE_RATE] Hz samples, until the calling
     * coroutine is cancelled. Cooperatively cancellable: cancelling the
     * caller must stop the actual capture promptly (not just abandon the
     * suspension point) -- same contract as [AudioSink.play] (ADR-0003), so
     * a "Stopp" tap or leaving the recording screen cuts capture off
     * immediately instead of continuing to record in the background.
     */
    suspend fun record(onChunk: (ShortArray) -> Unit)

    override fun close()
}

/** Fixed recording format (ADR-0012 point 3) -- not configurable. */
const val RECORDING_SAMPLE_RATE = 22050
const val RECORDING_CHANNELS = 1

expect fun createAudioSource(): AudioSource
