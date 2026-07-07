package de.hexenwoche.audiolex.core.srs

/**
 * Strategy interface so the scheduling algorithm stays swappable
 * (fixed table now, SM-2/FSRS later — ADR-0005).
 */
interface ReviewScheduler {
    fun review(card: ReviewCard, rating: ReviewRating, nowEpochMillis: Long): ReviewCard

    fun isDue(card: ReviewCard, nowEpochMillis: Long): Boolean =
        card.dueAtEpochMillis <= nowEpochMillis
}

/** MVP scheduler: the manual rating selects the next interval directly. */
class FixedIntervalScheduler : ReviewScheduler {
    override fun review(card: ReviewCard, rating: ReviewRating, nowEpochMillis: Long): ReviewCard =
        card.copy(
            dueAtEpochMillis = nowEpochMillis + rating.interval.inWholeMilliseconds,
            lastRating = rating,
            repetitions = card.repetitions + 1,
        )
}
