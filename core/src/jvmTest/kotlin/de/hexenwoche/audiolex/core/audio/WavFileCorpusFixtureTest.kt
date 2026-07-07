package de.hexenwoche.audiolex.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Sanity check against a real Piper-generated file (checked in as a small
 * fixture, see .gitignore exception), not just hand-built WAV bytes.
 */
class WavFileCorpusFixtureTest {

    @Test
    fun decodesRealPiperGeneratedFile() {
        val bytes = requireNotNull(
            javaClass.classLoader.getResourceAsStream("sample-thorsten-ball.wav")
        ) { "test fixture missing: sample-thorsten-ball.wav" }.readBytes()

        val buffer = WavFile.decode(bytes)

        assertEquals(22050, buffer.sampleRate)
        assertEquals(1, buffer.channels)
        assertTrue(buffer.frameCount > 0)
        // "Ball" from thorsten-medium is a fraction of a second, not silence
        // and not absurdly long -- guards against gross decoding errors.
        val durationSeconds = buffer.frameCount.toDouble() / buffer.sampleRate
        assertTrue(durationSeconds in 0.1..2.0, "unexpected duration: ${durationSeconds}s")
    }
}
