package de.hexenwoche.audiolex.core.corpus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CorpusLoaderTest {

    private val wordsJson = """
        [
          { "id": "ball", "text": "Ball", "language": "de-DE",
            "syllableCount": 1, "category": "EVERYDAY" },
          { "id": "satz-morgen", "text": "Am Morgen steht ein Bagger vor dem Haus.",
            "language": "de-DE", "syllableCount": 10, "category": "EVERYDAY",
            "kind": "SENTENCE" }
        ]
    """.trimIndent()

    private val recordingsJson = """
        [
          { "id": "ball__thorsten", "wordId": "ball", "voiceId": "thorsten",
            "locale": "de-DE", "fileRef": "raw/de-DE/ball__thorsten.wav" },
          { "id": "ball__kerstin", "wordId": "ball", "voiceId": "kerstin",
            "locale": "de-DE", "fileRef": "raw/de-DE/ball__kerstin.wav" },
          { "id": "satz-morgen__thorsten", "wordId": "satz-morgen", "voiceId": "thorsten",
            "locale": "de-DE", "fileRef": "raw/de-DE/satz-morgen__thorsten.wav" }
        ]
    """.trimIndent()

    @Test
    fun `no kind loads all entries unfiltered`() {
        val corpus = parseCorpus(wordsJson, recordingsJson, kind = null)

        assertEquals(listOf("ball", "satz-morgen"), corpus.words.map { it.id })
    }

    @Test
    fun `kind WORD keeps only words`() {
        val corpus = parseCorpus(wordsJson, recordingsJson, kind = EntryKind.WORD)

        assertEquals(listOf("ball"), corpus.words.map { it.id })
    }

    @Test
    fun `kind SENTENCE keeps only sentences`() {
        val corpus = parseCorpus(wordsJson, recordingsJson, kind = EntryKind.SENTENCE)

        assertEquals(listOf("satz-morgen"), corpus.words.map { it.id })
    }

    @Test
    fun `wordById resolves from the filtered list`() {
        val corpus = parseCorpus(wordsJson, recordingsJson, kind = EntryKind.WORD)

        assertEquals("Ball", corpus.wordById("ball")?.text)
        // filtered out, not just missing from the recordings
        assertNull(corpus.wordById("satz-morgen"))
    }

    @Test
    fun `recordingFor returns the first recording in file order`() {
        val corpus = parseCorpus(wordsJson, recordingsJson)

        // exactly the old `recordings.firstOrNull { it.wordId == ... }`
        // semantics -- with two recordings for "ball" the file's first one
        // wins, not the last (associateBy would flip this).
        assertEquals("ball__thorsten", corpus.recordingFor("ball")?.id)
        assertEquals("satz-morgen__thorsten", corpus.recordingFor("satz-morgen")?.id)
        assertNull(corpus.recordingFor("unbekannt"))
    }

    @Test
    fun `unknown JSON fields are ignored`() {
        val withExtraField = """
            [ { "id": "ball", "text": "Ball", "language": "de-DE",
                "syllableCount": 1, "category": "EVERYDAY",
                "futureField": 42 } ]
        """.trimIndent()

        val corpus = parseCorpus(withExtraField, "[]")

        assertEquals(listOf("ball"), corpus.words.map { it.id })
    }
}
