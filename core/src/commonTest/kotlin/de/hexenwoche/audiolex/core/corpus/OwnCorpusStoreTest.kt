package de.hexenwoche.audiolex.core.corpus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OwnCorpusStoreTest {

    @Test
    fun `roundtrip preserves all fields`() {
        val entries = listOf(
            OwnEntry(
                id = "own-1000-1",
                text = "Kaffeetasse",
                kind = EntryKind.WORD,
                speaker = "männlich",
                fileName = "own-1000-1.wav",
                createdAtEpochMillis = 1000L,
            ),
            OwnEntry(
                id = "own-2000-2",
                text = "Der Zug fährt heute später ab.",
                kind = EntryKind.SENTENCE,
                speaker = "",
                fileName = "own-2000-2.wav",
                createdAtEpochMillis = 2000L,
            ),
        )

        val decoded = parseOwnCorpus(encodeOwnCorpus(entries))

        assertEquals(entries, decoded)
    }

    @Test
    fun `missing file yields an empty collection`() {
        assertEquals(emptyList(), parseOwnCorpus(null))
    }

    @Test
    fun `unknown JSON field is ignored`() {
        val withExtraField = """
            [ { "id": "own-1", "text": "Ball", "kind": "WORD", "speaker": "",
                "fileName": "own-1.wav", "createdAtEpochMillis": 42,
                "futureField": "irrelevant" } ]
        """.trimIndent()

        val entries = parseOwnCorpus(withExtraField)

        assertEquals(listOf("own-1"), entries.map { it.id })
    }

    @Test
    fun `corrupt JSON yields a quiet empty collection instead of throwing`() {
        val truncated = """[ { "id": "own-1", "text": "Ball", "fileNa"""

        val entries = parseOwnCorpus(truncated)

        assertTrue(entries.isEmpty())
    }

    @Test
    fun `valid JSON that is not an array of entries also yields an empty collection`() {
        val wrongShape = """{ "not": "a list" }"""

        val entries = parseOwnCorpus(wrongShape)

        assertTrue(entries.isEmpty())
    }

    @Test
    fun `missing required field yields an empty collection, not a crash`() {
        // fileName is required and absent here -- decoding the whole array fails.
        val missingFileName = """[ { "id": "own-1", "text": "Ball", "createdAtEpochMillis": 1 } ]"""

        val entries = parseOwnCorpus(missingFileName)

        assertTrue(entries.isEmpty())
    }

    @Test
    fun `kind and speaker default when omitted`() {
        val minimal = """[ { "id": "own-1", "text": "Ball", "fileName": "own-1.wav", "createdAtEpochMillis": 1 } ]"""

        val entries = parseOwnCorpus(minimal)

        assertEquals(EntryKind.WORD, entries.single().kind)
        assertEquals("", entries.single().speaker)
    }
}
