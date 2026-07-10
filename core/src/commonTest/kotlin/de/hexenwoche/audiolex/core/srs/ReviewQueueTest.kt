package de.hexenwoche.audiolex.core.srs

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
}
