package de.hexenwoche.audiolex.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

actual fun createAudioSource(): AudioSource = AndroidAudioSource()

/**
 * Minimal [AudioRecord]-based source. `MediaRecorder.AudioSource.MIC` is the
 * plain, unprocessed microphone source (as opposed to e.g.
 * `VOICE_RECOGNITION`, which some devices bias with AGC/noise suppression
 * tuned for recognizers) -- appropriate for a corpus recording meant to be
 * mixed with a noise overlay later (ADR-0010), not pre-cleaned.
 *
 * Permission handling (RECORD_AUDIO) is the caller's responsibility (Backlog
 * AC3, the Compose UI layer) -- this class has no `Context` and can't check
 * or request it itself, same as [AndroidAudioSink] has no `Context` for its
 * playback attributes. The [SuppressLint] below documents that split rather
 * than hiding a real gap: an ungranted permission surfaces as a
 * `SecurityException` from the `AudioRecord` constructor, caught the same
 * way as any other recording failure by the caller.
 */
@SuppressLint("MissingPermission")
private class AndroidAudioSource : AudioSource {
    override suspend fun record(onChunk: (ShortArray) -> Unit) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            RECORDING_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minBufferSize > 0) {
            "device reports no valid buffer size for $RECORDING_SAMPLE_RATE Hz mono PCM16 " +
                "(AudioRecord.getMinBufferSize returned $minBufferSize)"
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            RECORDING_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2,
        )
        try {
            // A failed AudioRecord init is silent -- the constructor doesn't
            // throw, it just leaves the object in STATE_UNINITIALIZED. This
            // check is what this batch exists to add (ADR-0012).
            check(audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                "AudioRecord failed to initialize (state=${audioRecord.state})"
            }
            // The device may hand back a different configuration than
            // requested. Catching that here -- not resampling or silently
            // falling back -- is the whole point of this check (ADR-0012
            // Konsequenzen: recordings in another format can't be combined
            // with the noise overlay, ADR-0010).
            check(audioRecord.sampleRate == RECORDING_SAMPLE_RATE) {
                "device delivered ${audioRecord.sampleRate} Hz instead of the required " +
                    "$RECORDING_SAMPLE_RATE Hz -- refusing rather than resampling or " +
                    "accepting a format the noise overlay mixer can't use"
            }
            check(audioRecord.channelCount == RECORDING_CHANNELS) {
                "device delivered ${audioRecord.channelCount} channel(s) instead of the " +
                    "required $RECORDING_CHANNELS (mono)"
            }

            val buffer = ShortArray(minBufferSize)
            audioRecord.startRecording()
            check(audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                "AudioRecord.startRecording() did not enter RECORDSTATE_RECORDING"
            }
            try {
                while (true) {
                    // Checked before each blocking read so cancelling the
                    // caller stops capture within one buffer's worth of
                    // audio instead of continuing after the caller moved on
                    // -- AudioRecord.read() is a plain blocking call, not a
                    // coroutine suspension point, so cancellation isn't
                    // observed automatically the way it is in AudioSink.play
                    // (which suspends via delay()/runInterruptible).
                    currentCoroutineContext().ensureActive()
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    check(read >= 0) { "AudioRecord.read() returned error code $read" }
                    if (read > 0) {
                        onChunk(buffer.copyOf(read))
                    }
                }
            } finally {
                audioRecord.stop()
            }
        } finally {
            audioRecord.release()
        }
    }

    override fun close() = Unit
}
