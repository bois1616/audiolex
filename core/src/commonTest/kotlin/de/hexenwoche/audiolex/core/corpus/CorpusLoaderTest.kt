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

class MergeCorpusTest {

    private val builtInWords = listOf(
        Word("ball", "Ball", "de-DE", 1, WordCategory.EVERYDAY),
        Word("satz-morgen", "Am Morgen...", "de-DE", 10, WordCategory.EVERYDAY, kind = EntryKind.SENTENCE),
    )
    private val builtInRecordings = listOf(
        AudioRecording("ball__thorsten", "ball", "thorsten", "de-DE", "raw/de-DE/ball__thorsten.wav"),
        AudioRecording("satz-morgen__thorsten", "satz-morgen", "thorsten", "de-DE", "raw/de-DE/satz-morgen__thorsten.wav"),
    )
    private val ownWordEntry = OwnEntry(
        id = "own-1", text = "Fenster", kind = EntryKind.WORD, speaker = "Anna", fileName = "own-1.wav",
        createdAtEpochMillis = 1L,
    )
    private val ownSentenceEntry = OwnEntry(
        id = "own-2", text = "Die Tür ist offen.", kind = EntryKind.SENTENCE, speaker = "Anna",
        fileName = "own-2.wav", createdAtEpochMillis = 2L,
    )

    @Test
    fun `only built-in entries when own list is empty`() {
        val corpus = mergeCorpus(builtInWords, builtInRecordings, emptyList())

        assertEquals(listOf("ball", "satz-morgen"), corpus.words.map { it.id })
    }

    @Test
    fun `only own entries when built-in lists are empty`() {
        val corpus = mergeCorpus(emptyList(), emptyList(), listOf(ownWordEntry, ownSentenceEntry))

        assertEquals(listOf("own-1", "own-2"), corpus.words.map { it.id })
    }

    @Test
    fun `both sources merge together`() {
        val corpus = mergeCorpus(builtInWords, builtInRecordings, listOf(ownWordEntry))

        assertEquals(listOf("ball", "satz-morgen", "own-1"), corpus.words.map { it.id })
    }

    @Test
    fun `kind filter applies across both sources`() {
        val corpus = mergeCorpus(builtInWords, builtInRecordings, listOf(ownWordEntry, ownSentenceEntry), kind = EntryKind.WORD)

        assertEquals(listOf("ball", "own-1"), corpus.words.map { it.id })
    }

    @Test
    fun `empty on both sides yields an empty corpus`() {
        val corpus = mergeCorpus(emptyList(), emptyList(), emptyList())

        assertEquals(emptyList(), corpus.words)
    }

    @Test
    fun `own entry maps voiceId from speaker and locale defaults to de-DE`() {
        val corpus = mergeCorpus(emptyList(), emptyList(), listOf(ownWordEntry))

        val recording = corpus.recordingFor("own-1")
        assertEquals("Anna", recording?.voiceId)
        assertEquals("de-DE", recording?.locale)
        assertEquals(RecordingSource.EIGEN, recording?.source)
    }

    @Test
    fun `built-in recording keeps the MITGELIEFERT default source`() {
        val corpus = mergeCorpus(builtInWords, builtInRecordings, emptyList())

        assertEquals(RecordingSource.MITGELIEFERT, corpus.recordingFor("ball")?.source)
    }
}
