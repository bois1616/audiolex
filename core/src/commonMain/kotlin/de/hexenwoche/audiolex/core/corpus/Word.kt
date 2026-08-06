package de.hexenwoche.audiolex.core.corpus

import kotlinx.serialization.Serializable

/**
 * Generic corpus entry — nothing hearing-loss specific is wired in here,
 * so the corpus can later serve plain vocabulary training (concept 3.4).
 *
 * An entry is a word or a whole sentence (see [kind]; ADR-0009). Sentences
 * are trained exactly like words: same learning/exam flow, same SRS. The
 * name `Word` is kept for both; for sentences `syllableCount` is the
 * sentence's total syllables and `phoneticGroup` stays null.
 */
@Serializable
data class Word(
    val id: String,
    val text: String,
    /** BCP-47 tag, e.g. "de-DE". */
    val language: String,
    val syllableCount: Int,
    val category: WordCategory,
    /** Key of a minimal-pair group (e.g. "gnu-kuh"); null if not grouped. */
    val phoneticGroup: String? = null,
    /**
     * Entry kind (ADR-0009). kotlinx default WORD keeps words.json files
     * without this field valid — no migration needed.
     */
    val kind: EntryKind = EntryKind.WORD,
)

/** Distinguishes single-word entries from sentence entries (ADR-0009). */
@Serializable
enum class EntryKind { WORD, SENTENCE }

@Serializable
enum class WordCategory { EVERYDAY, LOANWORD, FOREIGN }

/**
 * Where a recording's audio bytes live (Backlog Eigen-Korpus Batch C, AC5):
 * [MITGELIEFERT] means [AudioRecording.fileRef] is a path under the packed
 * Compose resources, [EIGEN] means it's a file name under the own-corpus
 * directory ([de.hexenwoche.audiolex.core.corpus.OwnEntry.fileName]).
 * Replaces three near-identical `Res.readBytes` call sites in composeApp
 * with one lookup this flag drives. Defaults to [MITGELIEFERT] via the
 * kotlinx default (same pattern as [Word.kind]) so `recordings.json` --
 * which predates this field -- stays valid without a migration; the field
 * only ever exists in Room-free JSON/in-memory data, not in a database
 * column, so there's no schema to migrate either way.
 */
@Serializable
enum class RecordingSource { MITGELIEFERT, EIGEN }

/** One recorded rendition of a word; a word can have several speakers. */
@Serializable
data class AudioRecording(
    val id: String,
    val wordId: String,
    val voiceId: String,
    /**
     * BCP-47 tag of the recording's regional variant, e.g. "de-DE" (standard),
     * "de-AT", "de-CH", or a dialect subtag once available (e.g. "de-DE-bar").
     * Separate from [Word.language] so a word's dialect pool can be filtered
     * independently of its base language (ADR-0006).
     */
    val locale: String,
    /** Path relative to the corpus data root ([RecordingSource.MITGELIEFERT]) or a file name under the own-corpus directory ([RecordingSource.EIGEN]) -- see [source]. */
    val fileRef: String,
    val source: RecordingSource = RecordingSource.MITGELIEFERT,
)
