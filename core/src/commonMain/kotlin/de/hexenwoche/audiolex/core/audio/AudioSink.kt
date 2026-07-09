package de.hexenwoche.audiolex.core.audio

/**
 * Thin platform boundary (ADR-0003): receives fully mixed PCM and plays it.
 * No business logic below this interface.
 */
interface AudioSink : AutoCloseable {
    /**
     * Plays the buffer, suspending until playback finished. Cooperatively
     * cancellable: cancelling the calling coroutine must stop the actual
     * audio promptly (not just abandon the suspension point), so a fast
     * double-tap or leaving mid-session (Szenario S5) is heard to cut off
     * immediately rather than finishing in the background.
     */
    suspend fun play(buffer: PcmBuffer)

    override fun close()
}

expect fun createAudioSink(): AudioSink
