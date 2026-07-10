package de.hexenwoche.audiolex.core.srs

import de.hexenwoche.audiolex.core.time.FakeClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression test for the ADR-0008 bug: a hardcoded `nowEpochMillis = 0L` at
 * the UI integration point made every rated card due again immediately
 * (dueAtEpochMillis was always in 1970), so ReviewQueue.due() kept returning
 * it forever. This drives ReviewQueue/FixedIntervalScheduler through the same
 * FakeClock abstraction the screen now uses, without needing a UI test.
 */
class ClockRegressionTest {

    private val scheduler = FixedIntervalScheduler()

    @Test
    fun ratedCardIsNotDueUntilItsIntervalElapses() {
        val clock = FakeClock(now = 1_000_000L)
        val card = ReviewCard(wordId = "ball")

        val rated = scheduler.review(card, ReviewRating.SOON, clock.nowEpochMillis())

        assertTrue(ReviewQueue.due(listOf(rated), clock.nowEpochMillis()).isEmpty())

        clock.now += ReviewRating.SOON.interval.inWholeMilliseconds

        assertEquals(listOf(rated), ReviewQueue.due(listOf(rated), clock.nowEpochMillis()))
    }
}
