package de.hexenwoche.audiolex.core.srs

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewQueueTest {

    private fun card(wordId: String, dueAtEpochMillis: Long) = ReviewCard(wordId, dueAtEpochMillis)

    @Test
    fun returnsEmptyListWhenNothingIsDue() {
        val cards = listOf(card("a", dueAtEpochMillis = 1000L))
        assertTrue(ReviewQueue.due(cards, nowEpochMillis = 0L).isEmpty())
    }

    @Test
    fun includesCardsDueExactlyNow() {
        val cards = listOf(card("a", dueAtEpochMillis = 500L))
        assertEquals(listOf("a"), ReviewQueue.due(cards, nowEpochMillis = 500L).map { it.wordId })
    }

    @Test
    fun ordersMostOverdueFirst() {
        val cards = listOf(
            card("recent", dueAtEpochMillis = 400L),
            card("oldest", dueAtEpochMillis = 100L),
            card("middle", dueAtEpochMillis = 200L),
        )
        val due = ReviewQueue.due(cards, nowEpochMillis = 1000L)
        assertEquals(listOf("oldest", "middle", "recent"), due.map { it.wordId })
    }

    @Test
    fun breaksTiesByWordIdForDeterministicOrder() {
        val cards = listOf(
            card("zebra", dueAtEpochMillis = 100L),
            card("apple", dueAtEpochMillis = 100L),
            card("mango", dueAtEpochMillis = 100L),
        )
        val due = ReviewQueue.due(cards, nowEpochMillis = 1000L)
        assertEquals(listOf("apple", "mango", "zebra"), due.map { it.wordId })
    }

    @Test
    fun excludesNotYetDueCardsMixedWithDueOnes() {
        val cards = listOf(
            card("due", dueAtEpochMillis = 0L),
            card("notDue", dueAtEpochMillis = 999L),
        )
        assertEquals(listOf("due"), ReviewQueue.due(cards, nowEpochMillis = 500L).map { it.wordId })
    }

    @Test
    fun roundOfCapsAtSizeUsingMostOverdueDueCards() {
        val cards = (1..20).map { card("w${it.toString().padStart(2, '0')}", dueAtEpochMillis = it.toLong()) }
        val round = ReviewQueue.roundOf(cards, nowEpochMillis = 1000L, size = 15)
        assertEquals(15, round.size)
        // Most overdue (smallest due time) first, so w01..w15.
        assertEquals((1..15).map { "w${it.toString().padStart(2, '0')}" }, round.map { it.wordId })
    }

    @Test
    fun roundOfTopsUpWithNotDueCardsWhenTooFewDue() {
        val cards = listOf(
            card("due1", dueAtEpochMillis = 100L),
            card("due2", dueAtEpochMillis = 200L),
            card("future1", dueAtEpochMillis = 10_000L),
            card("future2", dueAtEpochMillis = 20_000L),
            card("future3", dueAtEpochMillis = 30_000L),
        )
        val round = ReviewQueue.roundOf(cards, nowEpochMillis = 1000L, size = 4, random = Random(42))
        assertEquals(4, round.size)
        // Due cards come first, in due order.
        assertEquals(listOf("due1", "due2"), round.take(2).map { it.wordId })
        // The remaining two are drawn from the not-due pool (no due card repeated).
        val fillers = round.drop(2).map { it.wordId }
        assertTrue(fillers.all { it.startsWith("future") }, "fillers must be not-due cards, got $fillers")
        assertEquals(fillers.toSet().size, fillers.size, "no card repeated within a round")
    }

    @Test
    fun roundOfTakesAllWhenFewerCardsThanSize() {
        val cards = listOf(
            card("a", dueAtEpochMillis = 100L),
            card("b", dueAtEpochMillis = 50_000L),
        )
        val round = ReviewQueue.roundOf(cards, nowEpochMillis = 1000L, size = 15, random = Random(1))
        assertEquals(2, round.size)
        assertEquals(setOf("a", "b"), round.map { it.wordId }.toSet())
    }

    @Test
    fun roundOfWithNoCardsIsEmpty() {
        assertTrue(ReviewQueue.roundOf(emptyList(), nowEpochMillis = 1000L, size = 15).isEmpty())
    }

    @Test
    fun roundOfNeverRepeatsACardEvenWhenAllAreNotDue() {
        val cards = (1..5).map { card("w$it", dueAtEpochMillis = 100_000L) }
        val round = ReviewQueue.roundOf(cards, nowEpochMillis = 0L, size = 15, random = Random(7))
        assertEquals(5, round.size)
        assertEquals(5, round.map { it.wordId }.toSet().size)
    }
}
