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

    // Backlog Eigen-Korpus Batch D, AC7: exclusion filter, applied inside mergeCorpus.

    @Test
    fun `empty exclusion set is bit-identical to not passing the parameter at all`() {
        val withDefault = mergeCorpus(builtInWords, builtInRecordings, listOf(ownWordEntry))
        val withEmptySet = mergeCorpus(builtInWords, builtInRecordings, listOf(ownWordEntry), excludedSpeakers = emptySet())

        assertEquals(withDefault.words, withEmptySet.words)
        assertEquals(withDefault.recordings, withEmptySet.recordings)
    }

    @Test
    fun `excluding one speaker drops only that speaker's recordings and now-orphaned words`() {
        val corpus = mergeCorpus(
            builtInWords, builtInRecordings, listOf(ownWordEntry, ownSentenceEntry),
            excludedSpeakers = setOf("Anna"),
        )

        // "Anna" contributed both own entries -- both their recordings and
        // words disappear, thorsten's built-in entries are untouched.
        assertEquals(listOf("ball", "satz-morgen"), corpus.words.map { it.id })
        assertEquals(listOf("ball__thorsten", "satz-morgen__thorsten"), corpus.recordings.map { it.id })
    }

    @Test
    fun `excluding every speaker empties both lists`() {
        val corpus = mergeCorpus(
            builtInWords, builtInRecordings, listOf(ownWordEntry),
            excludedSpeakers = setOf("thorsten", "Anna"),
        )

        assertEquals(emptyList(), corpus.words)
        assertEquals(emptyList(), corpus.recordings)
    }

    @Test
    fun `unknown name in the exclusion set has no effect`() {
        val corpus = mergeCorpus(
            builtInWords, builtInRecordings, listOf(ownWordEntry),
            excludedSpeakers = setOf("jemand-den-es-nicht-gibt"),
        )

        assertEquals(listOf("ball", "satz-morgen", "own-1"), corpus.words.map { it.id })
    }

    @Test
    fun `a word that never had a recording stays even when everything else is excluded`() {
        val wordWithoutRecording = Word("kein-ton", "Kein Ton", "de-DE", 1, WordCategory.EVERYDAY)
        val corpus = mergeCorpus(
            builtInWords + wordWithoutRecording, builtInRecordings, emptyList(),
            excludedSpeakers = setOf("thorsten"),
        )

        // AC7: a word without a recording today is unrelated to the
        // exclusion feature and must not be affected by it.
        assertEquals(listOf("kein-ton"), corpus.words.map { it.id })
    }

    @Test
    fun `kind filter and exclusion combine`() {
        val corpus = mergeCorpus(
            builtInWords, builtInRecordings, listOf(ownWordEntry, ownSentenceEntry),
            kind = EntryKind.WORD,
            excludedSpeakers = setOf("thorsten"),
        )

        // thorsten excluded removes "ball"; the kind filter then also drops
        // Anna's sentence, leaving only Anna's word.
        assertEquals(listOf("own-1"), corpus.words.map { it.id })
    }
}

class SpeakerContingentsTest {

    private val words = listOf(
        Word("ball", "Ball", "de-DE", 1, WordCategory.EVERYDAY),
        Word("satz-morgen", "Am Morgen...", "de-DE", 10, WordCategory.EVERYDAY, kind = EntryKind.SENTENCE),
        Word("own-1", "Fenster", "de-DE", 2, WordCategory.EVERYDAY),
    )
    private val recordings = listOf(
        AudioRecording("ball__thorsten", "ball", "thorsten", "de-DE", "raw/de-DE/ball__thorsten.wav"),
        AudioRecording(
            "satz-morgen__thorsten", "satz-morgen", "thorsten", "de-DE",
            "raw/de-DE/satz-morgen__thorsten.wav",
        ),
        AudioRecording("own-1", "own-1", "Anna", "de-DE", "own-1.wav", source = RecordingSource.EIGEN),
    )

    @Test
    fun `counts words and sentences separately per speaker`() {
        val contingents = LoadedCorpus(words, recordings).speakerContingents()

        assertEquals(
            listOf(SpeakerContingent("Anna", wordCount = 1, sentenceCount = 0), SpeakerContingent("thorsten", wordCount = 1, sentenceCount = 1)),
            contingents,
        )
    }

    @Test
    fun `a speaker with no recordings never appears`() {
        val contingents = LoadedCorpus(words, emptyList()).speakerContingents()

        assertEquals(emptyList(), contingents)
    }

    @Test
    fun `a recording for a word outside the corpus is not counted`() {
        val orphanRecording = AudioRecording("ghost", "unbekanntes-wort", "Stephan", "de-DE", "ghost.wav")

        val contingents = LoadedCorpus(words, listOf(orphanRecording)).speakerContingents()

        assertEquals(emptyList(), contingents)
    }

    @Test
    fun `blank speaker is its own contingent, sorted before named ones`() {
        val blankSpeakerRecording = AudioRecording("own-1", "own-1", "", "de-DE", "own-1.wav", source = RecordingSource.EIGEN)

        val contingents = LoadedCorpus(words, listOf(blankSpeakerRecording) + recordings).speakerContingents()

        assertEquals(listOf("", "Anna", "thorsten"), contingents.map { it.speaker })
    }

    @Test
    fun `contingents are sorted alphabetically`() {
        val contingents = LoadedCorpus(words, recordings).speakerContingents()

        assertEquals(listOf("Anna", "thorsten"), contingents.map { it.speaker })
    }
}
