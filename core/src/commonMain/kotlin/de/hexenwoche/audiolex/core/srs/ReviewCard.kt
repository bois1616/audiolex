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

/**
 * Single source of truth for the due comparison (Backlog "Code-Qualität":
 * previously duplicated between [ReviewScheduler.isDue] and [ReviewQueue]'s
 * own inline filter). Both now delegate here instead of repeating
 * `dueAtEpochMillis <= now` independently.
 */
fun ReviewCard.isDue(nowEpochMillis: Long): Boolean = dueAtEpochMillis <= nowEpochMillis
