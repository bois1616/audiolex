package de.hexenwoche.audiolex.core.persistence

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.hexenwoche.audiolex.core.srs.ReviewCard
import de.hexenwoche.audiolex.core.srs.ReviewRating
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Real Room roundtrip (no mocking), same approach as the earlier persistence
 * spike (ADR-0004) but against the actual ReviewCardEntity/Dao schema.
 */
class ReviewCardRepositoryTest {

    private fun newRepository(): ReviewCardRepository {
        val db = Room.inMemoryDatabaseBuilder<AudioLexDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        return RoomReviewCardRepository(db.reviewCardDao())
    }

    @Test
    fun allIsEmptyForFreshDatabase() = runTest {
        assertTrue(newRepository().all().isEmpty())
    }

    @Test
    fun savedCardRoundtripsWithAllFieldsIncludingRating() = runTest {
        val repository = newRepository()
        val card = ReviewCard(
            wordId = "ball",
            dueAtEpochMillis = 12345L,
            lastRating = ReviewRating.GOOD,
            repetitions = 3,
        )

        repository.save(card)

        assertEquals(listOf(card), repository.all())
    }

    @Test
    fun savedCardWithNullRatingRoundtrips() = runTest {
        val repository = newRepository()
        val card = ReviewCard(wordId = "haus", dueAtEpochMillis = 0L)

        repository.save(card)

        val loaded = repository.all().single()
        assertNull(loaded.lastRating)
        assertEquals(card, loaded)
    }

    @Test
    fun saveOverwritesExistingCardForSameWordId() = runTest {
        val repository = newRepository()
        repository.save(ReviewCard(wordId = "ball", dueAtEpochMillis = 1L))
        repository.save(ReviewCard(wordId = "ball", dueAtEpochMillis = 2L, lastRating = ReviewRating.PERFECT))

        val loaded = repository.all().single()
        assertEquals(2L, loaded.dueAtEpochMillis)
        assertEquals(ReviewRating.PERFECT, loaded.lastRating)
    }

    @Test
    fun allOrSeedCreatesImmediatelyDueCardsForUnknownWords() = runTest {
        val repository = newRepository()

        val seeded = repository.allOrSeed(listOf("a", "b"))

        assertEquals(listOf("a", "b"), seeded.map { it.wordId })
        assertTrue(seeded.all { it.dueAtEpochMillis == 0L })
        // Seeding also persists, so a second load doesn't create new cards.
        assertEquals(seeded, repository.allOrSeed(listOf("a", "b")))
    }

    @Test
    fun allOrSeedPreservesExistingCardsAndOnlySeedsMissingOnes() = runTest {
        val repository = newRepository()
        repository.save(ReviewCard(wordId = "a", dueAtEpochMillis = 999L, lastRating = ReviewRating.GOOD))

        val result = repository.allOrSeed(listOf("a", "b"))

        val cardA = result.single { it.wordId == "a" }
        val cardB = result.single { it.wordId == "b" }
        assertEquals(999L, cardA.dueAtEpochMillis)
        assertEquals(ReviewRating.GOOD, cardA.lastRating)
        assertEquals(0L, cardB.dueAtEpochMillis)
    }

    /**
     * Many-seeds case (AC3): exercises the [ReviewCardDao.upsertAll] batch
     * path with more than a handful of missing ids in one go -- every id
     * gets an immediately-due card, the batch persists them all, and a
     * second call sees them as already-existing (no duplicate/second seed).
     */
    @Test
    fun allOrSeedBatchPersistsManyMissingCardsAtOnce() = runTest {
        val repository = newRepository()
        val wordIds = (1..50).map { "word-$it" }

        val seeded = repository.allOrSeed(wordIds)

        assertEquals(wordIds, seeded.map { it.wordId })
        assertTrue(seeded.all { it.dueAtEpochMillis == 0L })
        assertEquals(wordIds.size, repository.all().size)
        // Second call sees them all as pre-existing -- no reseed, no drift.
        assertEquals(seeded, repository.allOrSeed(wordIds))
    }
}
