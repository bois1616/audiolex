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
    language = language,
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
    locale = language,
    fileRef = fileName,
    source = RecordingSource.EIGEN,
)

/**
 * Merges the built-in corpus with the caller's own entries into one
 * [LoadedCorpus] (Backlog Eigen-Korpus Batch C, AC3). Both sides are always
 * present here -- since Batch D (ADR-0012 Nachtrag "Die Quellentrennung wird
 * durch Kontingente ersetzt") there is no `CorpusSource` switch left that
 * would skip reading a side in the first place; composeApp's `loadCorpus`
 * always reads both. An empty [ownEntries] list still yields "nur
 * mitgeliefert" in practice (nothing recorded yet), but that's a fact about
 * the caller's data, not a mode this function branches on.
 *
 * [language] is the drawer filter (ADR-0016): entries whose
 * [Word.language] doesn't match are dropped along with their recordings,
 * before [kind] and [excludedSpeakers] ever run. Null keeps every drawer --
 * that is what the Kontingent-Liste needs, which has to show what exists,
 * not what is currently selected.
 *
 * [kind] filters the merged word list exactly like [parseCorpus] does for
 * the built-in one alone -- applied *after* merging, so it treats both
 * sources identically (Backlog AC3: the kind filter is orthogonal to which
 * source(s) are present, not a special case of it).
 *
 * [excludedSpeakers] is the Batch D contingent filter (AC7): recordings
 * whose [AudioRecording.voiceId] is in the set drop out, and so do words
 * that are left with *no* recording at all as a result -- an unplayable
 * card would otherwise sit in the round, exactly what Batch C AC4 already
 * avoids for missing recordings in general. Words that never had a
 * recording to begin with, unrelated to any exclusion, are untouched: with
 * an empty [excludedSpeakers] (the default) this function takes an early
 * return and the result is bit-identical to calling it without the
 * parameter at all (AC7) -- a stored exclusion set only ever removes
 * entries, it can't be the reason one was already missing.
 */
fun mergeCorpus(
    builtInWords: List<Word>,
    builtInRecordings: List<AudioRecording>,
    ownEntries: List<OwnEntry>,
    kind: EntryKind? = null,
    excludedSpeakers: Set<String> = emptySet(),
    language: CorpusLanguage? = null,
): LoadedCorpus {
    val allWords = builtInWords + ownEntries.map { it.toWord() }
    // Language filters first and hardest (ADR-0016): an entry filed under
    // another drawer is not merely hidden, it must not reach the SRS seeding
    // either -- otherwise switching to English once would leave English cards
    // sitting in a German round forever. Null means "every drawer", which is
    // what the Kontingent-Liste and the dev screen ask for.
    val languageWords = if (language == null) allWords else allWords.filter { language.matches(it.language) }
    val languageWordIds = languageWords.map { it.id }.toSet()
    val kindFilteredWords = if (kind == null) languageWords else languageWords.filter { it.kind == kind }
    val allRecordings = (builtInRecordings + ownEntries.map { it.toRecording() })
        .let { if (language == null) it else it.filter { recording -> recording.wordId in languageWordIds } }

    if (excludedSpeakers.isEmpty()) return LoadedCorpus(kindFilteredWords, allRecordings)

    val recordings = allRecordings.filter { it.voiceId !in excludedSpeakers }
    // A word only drops out if excluding a speaker actually orphaned it --
    // one that already had zero recordings before this filter ran stays,
    // so the empty-set early return above and this branch agree on every
    // word that isn't touched by the exclusion.
    val wordIdsWithRecordingBefore = allRecordings.map { it.wordId }.toSet()
    val wordIdsWithRecordingAfter = recordings.map { it.wordId }.toSet()
    val words = kindFilteredWords.filter { it.id !in wordIdsWithRecordingBefore || it.id in wordIdsWithRecordingAfter }
    return LoadedCorpus(words, recordings)
}

/**
 * One row of the Kontingent-Auswahl (Backlog Eigen-Korpus Batch D, AC4): a
 * speaker/[AudioRecording.voiceId] together with how many trainable words
 * and sentences it currently contributes. Purely derived, never persisted or
 * hand-maintained -- a speaker with nothing playable never becomes a key in
 * [LoadedCorpus.speakerContingents]'s grouping, so it can't show up here
 * (AC4: "kann gar nicht erst auftauchen"). [speaker] can be the empty string
 * (Batch B allows an unset speaker field) -- callers label that case
 * themselves ("Ohne Sprecher", AC4), this type stays UI-text-free like the
 * rest of `:core`.
 */
data class SpeakerContingent(val speaker: String, val wordCount: Int, val sentenceCount: Int)

/**
 * Derives the Kontingent-Liste (Backlog Eigen-Korpus Batch D, AC4) from a
 * [LoadedCorpus] that's unfiltered by [EntryKind]/exclusion -- callers pass
 * the result of [mergeCorpus] called *without* `kind`/`excludedSpeakers`, so
 * both counts stay visible regardless of the current Wörter/Sätze setting or
 * the persisted exclusion ("Beide Zahlen immer anzeigen", AC4: the list
 * shouldn't wobble as the user (de)selects contingents). Counted is what's
 * actually playable: every [AudioRecording] is joined against the [Word] it
 * belongs to, so a recording pointing at a word this corpus doesn't carry
 * (dropped upstream, e.g. by
 * [de.hexenwoche.audiolex.OwnCorpusRepository.trainable]'s "hat noch keinen
 * Text" filter) is silently not counted, same bar as Batch C AC4.
 */
fun LoadedCorpus.speakerContingents(): List<SpeakerContingent> {
    val wordsById = words.associateBy { it.id }
    val counts = mutableMapOf<String, Pair<Int, Int>>()
    for (recording in recordings) {
        val kind = wordsById[recording.wordId]?.kind ?: continue
        val (wordCount, sentenceCount) = counts.getOrDefault(recording.voiceId, 0 to 0)
        counts[recording.voiceId] = when (kind) {
            EntryKind.WORD -> (wordCount + 1) to sentenceCount
            EntryKind.SENTENCE -> wordCount to (sentenceCount + 1)
        }
    }
    return counts
        .map { (speaker, wordAndSentenceCount) -> SpeakerContingent(speaker, wordAndSentenceCount.first, wordAndSentenceCount.second) }
        .sortedBy { it.speaker }
}
