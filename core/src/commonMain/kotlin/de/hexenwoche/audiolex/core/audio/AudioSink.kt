package de.hexenwoche.audiolex.core.audio

/**
 * Thin platform boundary (ADR-0003): receives fully mixed PCM and plays it.
 * No business logic below this interface.
 */
interface AudioSink : AutoCloseable {
    /** Plays the buffer, blocking until playback finished. */
    fun play(buffer: PcmBuffer)

    override fun close()
}

expect fun createAudioSink(): AudioSink
