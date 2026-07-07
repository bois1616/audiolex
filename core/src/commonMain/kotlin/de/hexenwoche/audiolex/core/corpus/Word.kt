package de.hexenwoche.audiolex.core.corpus

/**
 * Generic corpus entry — nothing hearing-loss specific is wired in here,
 * so the corpus can later serve plain vocabulary training (concept 3.4).
 */
data class Word(
    val id: String,
    val text: String,
    /** BCP-47 tag, e.g. "de-DE". */
    val language: String,
    val syllableCount: Int,
    val category: WordCategory,
    /** Key of a minimal-pair group (e.g. "gnu-kuh"); null if not grouped. */
    val phoneticGroup: String? = null,
)

enum class WordCategory { EVERYDAY, LOANWORD, FOREIGN }

/** One recorded rendition of a word; a word can have several speakers. */
data class AudioRecording(
    val id: String,
    val wordId: String,
    val speaker: String,
    /** Path relative to the corpus data root. */
    val fileRef: String,
)
