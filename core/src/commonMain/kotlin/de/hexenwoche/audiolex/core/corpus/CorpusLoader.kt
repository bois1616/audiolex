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
