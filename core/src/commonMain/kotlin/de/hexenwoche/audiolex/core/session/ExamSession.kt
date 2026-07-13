package de.hexenwoche.audiolex.core.session

import de.hexenwoche.audiolex.core.srs.ReviewCard
import de.hexenwoche.audiolex.core.srs.ReviewRating
import de.hexenwoche.audiolex.core.srs.ReviewScheduler

/**
 * Pure progress tracking for Prüfmodus (Szenario S3): a linear walk through
 * due [cards] where each card is heard-only until [reveal]ed, then rated.
 * No audio, no persistence -- [rate] hands back the newly scheduled card so
 * the caller can persist it (M3's SRS persistence), it isn't stored here.
 *
 * Leaving mid-session ends it; there is no pause/resume state (Szenario
 * S5, decided 2026-07-08), same as [LearningSession].
 */
data class ExamSession(
    val cards: List<ReviewCard>,
    val currentIndex: Int = 0,
    val revealed: Boolean = false,
) {
    init {
        require(cards.isNotEmpty()) { "an exam session needs at least one due card" }
        require(currentIndex in cards.indices) { "currentIndex out of bounds" }
    }

    val currentCard: ReviewCard get() = cards[currentIndex]
    val progress: Int get() = currentIndex + 1
    val total: Int get() = cards.size
    val isLastCard: Boolean get() = currentIndex == cards.lastIndex

    /** Reveals the current card's word (Szenario S3): showing it is the caller's job. */
    fun reveal(): ExamSession = copy(revealed = true)

    /**
     * Moves from the current card to the next (unrevealed) one, or `null` on
     * the last card (Szenario S3 end-of-session). Deliberately separate from
     * [rate]: rating and advancing are distinct user actions in Prüfmodus --
     * the caller stays on the rated, revealed card until this is invoked
     * (Autor-Finding 2026-07-10, "Wiederholen"/"Nächstes"-Entzerrung).
     */
    fun advance(): ExamSession? =
        if (isLastCard) null else copy(currentIndex = currentIndex + 1, revealed = false)

    /**
     * Rates the current card via [scheduler], producing the newly scheduled
     * [ReviewCard] the caller should persist. Advancing to the next card is
     * a separate, explicit step ([advance]), not part of rating.
     */
    fun rate(rating: ReviewRating, nowEpochMillis: Long, scheduler: ReviewScheduler): RateResult {
        val ratedCard = scheduler.review(currentCard, rating, nowEpochMillis)
        return RateResult(ratedCard)
    }
}

/** Result of [ExamSession.rate]: the newly scheduled card to persist. */
data class RateResult(val ratedCard: ReviewCard)
