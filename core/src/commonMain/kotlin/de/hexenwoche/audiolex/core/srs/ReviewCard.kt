package de.hexenwoche.audiolex.core.srs

/**
 * SRS state of a single word, deliberately separate from the corpus entry
 * so the corpus stays generic (concept 3.4). Times are epoch milliseconds
 * to keep the core free of date-library dependencies.
 */
data class ReviewCard(
    val wordId: String,
    val dueAtEpochMillis: Long = 0L,
    val lastRating: ReviewRating? = null,
    val repetitions: Int = 0,
)
