package de.hexenwoche.audiolex.core.srs

/**
 * Selects and orders due cards for the exam mode (Szenario S3/S4).
 * Deterministic ordering (most overdue first, [ReviewCard.wordId] as
 * tiebreaker) keeps the review order reproducible instead of relying on
 * list/collection iteration order or randomness.
 */
object ReviewQueue {
    fun due(cards: List<ReviewCard>, nowEpochMillis: Long): List<ReviewCard> =
        cards
            .filter { it.dueAtEpochMillis <= nowEpochMillis }
            .sortedWith(compareBy(ReviewCard::dueAtEpochMillis, ReviewCard::wordId))
}
