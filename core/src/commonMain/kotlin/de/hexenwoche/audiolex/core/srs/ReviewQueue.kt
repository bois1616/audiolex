package de.hexenwoche.audiolex.core.srs

import kotlin.random.Random

/**
 * Selects and orders due cards for the exam mode (Szenario S3/S4).
 * Deterministic ordering (most overdue first, [ReviewCard.wordId] as
 * tiebreaker) keeps the review order reproducible instead of relying on
 * list/collection iteration order or randomness.
 */
object ReviewQueue {
    /** Default number of cards per exam round (Autor-Entscheid 2026-07-13). */
    const val DEFAULT_ROUND_SIZE = 15

    fun due(cards: List<ReviewCard>, nowEpochMillis: Long): List<ReviewCard> =
        cards
            .filter { it.dueAtEpochMillis <= nowEpochMillis }
            .sortedWith(compareBy(ReviewCard::dueAtEpochMillis, ReviewCard::wordId))

    /**
     * Builds one exam round of at most [size] cards (Autor-Entscheid
     * 2026-07-13): all due cards first, in [due] order; if that leaves room,
     * top up with randomly chosen not-yet-due cards so a round can still be
     * practised when the SRS says nothing is due. Fewer than [size] cards in
     * total simply yields a smaller round (no repetition). An empty [cards]
     * list yields an empty round.
     *
     * Ratings still update the SRS as usual -- this only controls *which*
     * cards a round shows, not how they're scheduled.
     */
    fun roundOf(
        cards: List<ReviewCard>,
        nowEpochMillis: Long,
        size: Int = DEFAULT_ROUND_SIZE,
        random: Random = Random.Default,
    ): List<ReviewCard> {
        if (size <= 0) return emptyList()
        val due = due(cards, nowEpochMillis)
        if (due.size >= size) return due.take(size)

        val remaining = size - due.size
        val fillers = cards
            .filter { it.dueAtEpochMillis > nowEpochMillis }
            .shuffled(random)
            .take(remaining)
        return due + fillers
    }
}
