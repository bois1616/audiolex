package de.hexenwoche.audiolex.core.session

import de.hexenwoche.audiolex.core.audio.AudioSink
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Serializes playback through an [AudioSink]: a new [play] request cancels
 * whatever is currently playing and starts immediately, instead of queuing
 * up behind it -- without this a fast double-tap starts overlapping
 * playbacks on separate coroutines (Opus-Review 2026-07-07, kleine Hinweise).
 * Relies on [AudioSink.play] being cooperatively cancellable (it suspends
 * rather than blocks), so cancelling the previous job actually stops that
 * playback's sound instead of merely abandoning the caller's wait for it.
 *
 * Not thread-safe across scopes; intended for one UI-bound [CoroutineScope].
 */
class PlaybackQueue(private val sink: AudioSink, private val scope: CoroutineScope) {
    private var currentJob: Job? = null

    /** Cancels any in-flight playback and starts [buffer] on [scope]. */
    fun play(buffer: PcmBuffer) {
        currentJob?.cancel()
        currentJob = scope.launch {
            sink.play(buffer)
        }
    }

    /** Stops playback without starting a new one (Szenario S5: leaving mid-session). */
    fun stop() {
        currentJob?.cancel()
        currentJob = null
    }
}
