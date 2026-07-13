package de.hexenwoche.audiolex.core.session

import de.hexenwoche.audiolex.core.srs.FixedIntervalScheduler
import de.hexenwoche.audiolex.core.srs.ReviewCard
import de.hexenwoche.audiolex.core.srs.ReviewRating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExamSessionTest {

    private val scheduler = FixedIntervalScheduler()
    private val now = 1_000_000L

    private fun card(wordId: String) = ReviewCard(wordId)

    private val threeCards = listOf(card("a"), card("b"), card("c"))

    @Test
    fun startsAtFirstCardUnrevealed() {
        val session = ExamSession(threeCards)
        assertEquals("a", session.currentCard.wordId)
        assertEquals(1, session.progress)
        assertEquals(3, session.total)
        assertFalse(session.revealed)
    }

    @Test
    fun revealSetsRevealedTrueWithoutChangingCard() {
        val session = ExamSession(threeCards).reveal()
        assertTrue(session.revealed)
        assertEquals("a", session.currentCard.wordId)
    }

    @Test
    fun rateSchedulesCurrentCardWithoutAdvancing() {
        val session = ExamSession(threeCards).reveal()
        val result = session.rate(ReviewRating.GOOD, now, scheduler)

        assertEquals("a", result.ratedCard.wordId)
        assertEquals(now + ReviewRating.GOOD.interval.inWholeMilliseconds, result.ratedCard.dueAtEpochMillis)

        val next = session.advance()
        assertEquals("b", next?.currentCard?.wordId)
        assertEquals(false, next?.revealed)
        assertEquals(2, next?.progress)
    }

    @Test
    fun rateOnLastCardStillAllowsAdvanceToReturnNull() {
        var session: ExamSession? = ExamSession(threeCards)
        repeat(2) {
            session!!.rate(ReviewRating.GOOD, now, scheduler)
            session = session?.advance()
        }
        assertTrue(session?.isLastCard == true)

        val result = session!!.rate(ReviewRating.GOOD, now, scheduler)
        assertEquals("c", result.ratedCard.wordId)
        assertNull(session!!.advance())
    }

    @Test
    fun rejectsEmptyCardList() {
        assertFailsWith<IllegalArgumentException> { ExamSession(emptyList()) }
    }

    @Test
    fun advanceMovesToNextCardUnrevealed() {
        val session = ExamSession(threeCards).reveal()
        val next = session.advance()

        assertEquals("b", next?.currentCard?.wordId)
        assertEquals(false, next?.revealed)
        assertEquals(2, next?.progress)
    }

    @Test
    fun advanceOnLastCardReturnsNull() {
        val session = ExamSession(threeCards, currentIndex = 2, revealed = true)
        assertNull(session.advance())
    }

    @Test
    fun advanceDoesNotRateTheCurrentCard() {
        val session = ExamSession(threeCards).reveal()
        val next = session.advance()
        assertEquals(threeCards, next?.cards)
    }

    @Test
    fun delegatesSchedulingEntirelyToScheduler() {
        val session = ExamSession(listOf(card("a")))
        val result = session.rate(ReviewRating.AGAIN, now, scheduler)
        val expected = scheduler.review(card("a"), ReviewRating.AGAIN, now)
        assertEquals(expected, result.ratedCard)
    }
}
