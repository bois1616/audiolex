package de.hexenwoche.audiolex.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [parseExcludedSpeakers]/[encodeExcludedSpeakers] (Backlog Eigen-Korpus
 * Batch D, AC2), same defensive posture as
 * [de.hexenwoche.audiolex.core.corpus.parseOwnCorpus] -- broken content
 * yields the empty set, which is also "alle", the harmless fallback.
 */
class SettingsTest {

    @Test
    fun `roundtrip preserves the set`() {
        val speakers = setOf("thorsten", "Anna", "")

        assertEquals(speakers, parseExcludedSpeakers(encodeExcludedSpeakers(speakers)))
    }

    @Test
    fun `empty set roundtrips to an empty array`() {
        assertEquals(emptySet(), parseExcludedSpeakers(encodeExcludedSpeakers(emptySet())))
        assertEquals("[]", encodeExcludedSpeakers(emptySet()))
    }

    @Test
    fun `corrupt JSON yields a quiet empty set instead of throwing`() {
        assertTrue(parseExcludedSpeakers("""["thorsten", "An""").isEmpty())
    }

    @Test
    fun `valid JSON that is not an array of strings also yields an empty set`() {
        assertTrue(parseExcludedSpeakers("""{"not": "an array"}""").isEmpty())
    }

    @Test
    fun `duplicate names collapse into the set`() {
        assertEquals(setOf("thorsten"), parseExcludedSpeakers("""["thorsten", "thorsten"]"""))
    }
}
