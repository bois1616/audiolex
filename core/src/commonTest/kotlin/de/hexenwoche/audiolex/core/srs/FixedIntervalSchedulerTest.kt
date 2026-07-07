package de.hexenwoche.audiolex.core.srs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FixedIntervalSchedulerTest {

    private val scheduler = FixedIntervalScheduler()
    private val now = 1_000_000L

    @Test
    fun everyRatingSchedulesItsFixedInterval() {
        for (rating in ReviewRating.entries) {
            val card = scheduler.review(ReviewCard(wordId = "w1"), rating, now)
            assertEquals(
                now + rating.interval.inWholeMilliseconds,
                card.dueAtEpochMillis,
                "rating $rating",
            )
            assertEquals(rating, card.lastRating)
        }
    }

    @Test
    fun repetitionsIncrementAcrossReviews() {
        var card = ReviewCard(wordId = "w1")
        card = scheduler.review(card, ReviewRating.AGAIN, now)
        card = scheduler.review(card, ReviewRating.GOOD, now + 60_000)
        assertEquals(2, card.repetitions)
    }

    @Test
    fun dueExactlyAtDeadlineCountsAsDue() {
        val card = ReviewCard(wordId = "w1", dueAtEpochMillis = now)
        assertTrue(scheduler.isDue(card, now))
        assertFalse(scheduler.isDue(card, now - 1))
    }

    @Test
    fun newCardIsImmediatelyDue() {
        assertTrue(scheduler.isDue(ReviewCard(wordId = "w1"), now))
    }
}
