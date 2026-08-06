package de.hexenwoche.audiolex.core.corpus

import kotlinx.serialization.json.Json

/**
 * Parsed corpus metadata with indexed lookups, loaded once per screen entry
 * (Backlog "Code-Qualität": the three screens used to load/parse the same
 * two JSON files and scan the recordings list linearly on every playback).
 * [words] is already filtered by the caller's requested [EntryKind].
 */
class LoadedCorpus(
    val words: List<Word>,
    val recordings: List<AudioRecording>,
) {
    private val wordsById: Map<String, Word> = words.associateBy { it.id }

    // firstOrNull semantics on the original list: groupBy keeps element
    // order, so the first recording in file order wins per word
    // (associateBy would keep the last instead).
    private val recordingByWordId: Map<String, AudioRecording> =
        recordings.groupBy { it.wordId }.mapValues { (_, recs) -> recs.first() }

    fun wordById(id: String): Word? = wordsById[id]

    fun recordingFor(wordId: String): AudioRecording? = recordingByWordId[wordId]
}

private val json = Json { ignoreUnknownKeys = true }

/**
 * Parses `words.json` + `recordings.json` content into a [LoadedCorpus],
 * keeping only entries of [kind]. [kind] = null loads everything unfiltered
 * (the dev channel test lists all entries; the training screens pass the
 * entry kind their corpus mode admits). Platform-independent on purpose --
 * the caller supplies the JSON strings, so this stays jvm-testable while
 * the Compose-resource read lives in composeApp (`loadCorpus`).
 */
fun parseCorpus(
    wordsJson: String,
    recordingsJson: String,
    kind: EntryKind? = null,
): LoadedCorpus {
    val allWords = json.decodeFromString<List<Word>>(wordsJson)
    val words = if (kind == null) allWords else allWords.filter { it.kind == kind }
    val recordings = json.decodeFromString<List<AudioRecording>>(recordingsJson)
    return LoadedCorpus(words, recordings)
}

/**
 * Maps a self-recorded entry onto the shared corpus model (Backlog
 * Eigen-Korpus Batch C, AC3) -- so [LoadedCorpus] and every training screen
 * stay unaware there even are two sources, the same way [Word.kind] already
 * keeps them unaware of Wörter vs. Sätze. [Word.category] has no own-corpus
 * equivalent to draw from; [WordCategory.EVERYDAY] here is a placeholder,
 * not a judgment -- like [Word.syllableCount]/[Word.phoneticGroup] (both
 * hard-coded to "unbekannt" per AC3), it's never read outside tests.
 */
private fun OwnEntry.toWord(): Word = Word(
    id = id,
    text = text,
    language = "de-DE",
    syllableCount = 0,
    category = WordCategory.EVERYDAY,
    phoneticGroup = null,
    kind = kind,
)

/** Counterpart to [toWord] -- [AudioRecording.id] reuses [OwnEntry.id] since nothing looks a recording up by its own id, only by [AudioRecording.wordId] ([LoadedCorpus.recordingFor]). */
private fun OwnEntry.toRecording(): AudioRecording = AudioRecording(
    id = id,
    wordId = id,
    voiceId = speaker,
    locale = "de-DE",
    fileRef = fileName,
    source = RecordingSource.EIGEN,
)

/**
 * Merges the built-in corpus with the caller's own entries into one
 * [LoadedCorpus] (Backlog Eigen-Korpus Batch C, AC3). Which source(s) end up
 * represented is entirely up to what the caller passes in here -- an empty
 * [ownEntries] list is "nur mitgeliefert", an empty [builtInWords] is "nur
 * eigene", both non-empty is "beide". That's deliberate: it lets composeApp's
 * `loadCorpus` implement the `CorpusSource` choice by deciding *what to load
 * in the first place* (skipping the Compose-resource read entirely for
 * EIGENE, skipping the own-corpus read for MITGELIEFERT) without this
 * function ever needing to know the setting exists -- `core/corpus` stays
 * one-directional with respect to `core/settings` (which already depends on
 * `core/corpus` for [de.hexenwoche.audiolex.core.settings.entryKind], so the
 * reverse dependency would have made the two packages mutually dependent).
 *
 * [kind] filters the merged word list exactly like [parseCorpus] does for
 * the built-in one alone -- applied *after* merging, so it treats both
 * sources identically (Backlog AC3: the kind filter is orthogonal to which
 * source(s) are present, not a special case of it). [builtInRecordings] is
 * never filtered by [kind], same as [parseCorpus]'s recordings -- an
 * unreachable recording for a word the [kind] filter dropped is simply never
 * looked up.
 */
fun mergeCorpus(
    builtInWords: List<Word>,
    builtInRecordings: List<AudioRecording>,
    ownEntries: List<OwnEntry>,
    kind: EntryKind? = null,
): LoadedCorpus {
    val allWords = builtInWords + ownEntries.map { it.toWord() }
    val words = if (kind == null) allWords else allWords.filter { it.kind == kind }
    val recordings = builtInRecordings + ownEntries.map { it.toRecording() }
    return LoadedCorpus(words, recordings)
}
