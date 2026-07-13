package de.hexenwoche.audiolex.core.session

import de.hexenwoche.audiolex.core.audio.AudioSink
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Keeps at most one active playback on an [AudioSink]: a new [play] request
 * cancels whatever is currently playing and starts immediately, instead of
 * queuing up behind it -- without this a fast double-tap starts overlapping
 * playbacks on separate coroutines (Opus-Review 2026-07-07, kleine Hinweise).
 * Relies on [AudioSink.play] being cooperatively cancellable (it suspends
 * rather than blocks), so cancelling the previous job actually stops that
 * playback's sound instead of merely abandoning the caller's wait for it.
 *
 * [play] launches on [scope] and returns immediately, so a failing [sink]
 * (device disconnected, decode error) can't throw back to the caller --
 * [onError] is the only way callers learn about it (Szenario S7: a message
 * instead of a silent failure or crash).
 *
 * The [produce]-taking overload runs buffer *production* (WAV decode) inside
 * the same cancellable job as playback, so a new request cancels the previous
 * one atomically -- decode included. This closes a double-tap race the
 * plain [play] overload leaves open: when callers decode in their own
 * `scope.launch` *before* handing a finished buffer here, two taps can each
 * finish decoding and reach the sink before either cancels the other,
 * producing two overlapping AudioTracks (Autor-Finding 2026-07-13:
 * "Kaffee" -> "Kakaffee"). Preferred entry point for tap-driven replay.
 *
 * Not thread-safe across scopes; intended for one UI-bound [CoroutineScope].
 */
class PlaybackQueue(
    private val sink: AudioSink,
    private val scope: CoroutineScope,
    private val onError: (Throwable) -> Unit = {},
) {
    private var currentJob: Job? = null

    /** Cancels any in-flight playback and starts [buffer] on [scope]. */
    fun play(buffer: PcmBuffer) = play { buffer }

    /**
     * Cancels any in-flight playback, then produces a buffer via [produce]
     * and plays it -- both inside one cancellable job, so a rapid follow-up
     * call cancels this one before its buffer can reach the sink. [produce]
     * may suspend (e.g. reading and decoding a WAV file); an exception from
     * it is reported via [onError] just like a sink failure.
     */
    fun play(produce: suspend () -> PcmBuffer) {
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                val buffer = produce()
                sink.play(buffer)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /** Stops playback without starting a new one (Szenario S5: leaving mid-session). */
    fun stop() {
        currentJob?.cancel()
        currentJob = null
    }
}
