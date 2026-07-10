package de.hexenwoche.audiolex.core.persistence

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.hexenwoche.audiolex.core.session.Session
import de.hexenwoche.audiolex.core.srs.ReviewRating
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Real Room roundtrip (no mocking), same approach as ReviewCardRepositoryTest.
 */
class SessionRepositoryTest {

    private fun newRepository(): SessionRepository {
        val db = Room.inMemoryDatabaseBuilder<AudioLexDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        return RoomSessionRepository(db.sessionDao())
    }

    private fun session(startedAt: Long, ratingCounts: Map<ReviewRating, Int> = emptyMap()) = Session(
        startedAtEpochMillis = startedAt,
        zoneId = "Europe/Berlin",
        mode = "PRUEFMODUS",
        ratedCount = ratingCounts.values.sum(),
        ratingCounts = ratingCounts,
    )

    @Test
    fun allIsEmptyForFreshDatabase() = runTest {
        assertTrue(newRepository().all().isEmpty())
    }

    @Test
    fun savedSessionRoundtripsWithRatingDistribution() = runTest {
        val repository = newRepository()
        val ratingCounts = mapOf(
            ReviewRating.AGAIN to 1,
            ReviewRating.SOON to 0,
            ReviewRating.LATER to 2,
            ReviewRating.GOOD to 3,
            ReviewRating.PERFECT to 1,
        )

        repository.save(session(startedAt = 1_000L, ratingCounts = ratingCounts))

        val loaded = repository.all().single()
        assertEquals(1_000L, loaded.startedAtEpochMillis)
        assertEquals("Europe/Berlin", loaded.zoneId)
        assertEquals("PRUEFMODUS", loaded.mode)
        assertEquals(7, loaded.ratedCount)
        assertEquals(ratingCounts, loaded.ratingCounts)
    }

    @Test
    fun multipleSessionsComeBackNewestFirst() = runTest {
        val repository = newRepository()
        repository.save(session(startedAt = 1_000L))
        repository.save(session(startedAt = 3_000L))
        repository.save(session(startedAt = 2_000L))

        val loaded = repository.all()

        assertEquals(listOf(3_000L, 2_000L, 1_000L), loaded.map { it.startedAtEpochMillis })
    }

    @Test
    fun emptyRatingDistributionMapsToZeroForEveryRating() = runTest {
        val repository = newRepository()
        repository.save(session(startedAt = 1_000L, ratingCounts = emptyMap()))

        val loaded = repository.all().single()

        assertEquals(0, loaded.ratedCount)
        ReviewRating.entries.forEach { rating ->
            assertEquals(0, loaded.ratingCounts[rating])
        }
    }
}
