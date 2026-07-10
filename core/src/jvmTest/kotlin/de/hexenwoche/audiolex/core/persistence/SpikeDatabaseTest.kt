package de.hexenwoche.audiolex.core.persistence

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Persistence spike (Backlog M3, ADR-0004): proves Room + KSP + the bundled
 * SQLite driver actually run in this project's jvm target, not just that
 * they compile. Real Room instance, real SQLite file -- no mocking.
 */
class SpikeDatabaseTest {

    @Test
    fun writtenCardRoundtripsThroughRealSqlite() = runTest {
        val path = tempDbPath()
        try {
            val card = SpikeCardEntity(wordId = "ball", dueAtEpochMillis = 12345L)

            val writeDb = createSpikeDatabase(path)
            writeDb.spikeCardDao().upsert(card)
            writeDb.close()

            // Reopen from the same file to prove it was actually persisted
            // to disk, not just held in the first instance's memory.
            val readDb = createSpikeDatabase(path)
            val loaded = readDb.spikeCardDao().findByWordId("ball")
            readDb.close()
            assertEquals(card, loaded)
        } finally {
            kotlin.io.path.Path(path).toFile().delete()
        }
    }

    @Test
    fun missingWordIdReturnsNull() = runTest {
        val db = createInMemorySpikeDatabase()
        assertNull(db.spikeCardDao().findByWordId("does-not-exist"))
        db.close()
    }

    @Test
    fun upsertOverwritesExistingEntryForSameWordId() = runTest {
        val db = createInMemorySpikeDatabase()
        db.spikeCardDao().upsert(SpikeCardEntity(wordId = "haus", dueAtEpochMillis = 1L))
        db.spikeCardDao().upsert(SpikeCardEntity(wordId = "haus", dueAtEpochMillis = 2L))

        val loaded = db.spikeCardDao().findByWordId("haus")
        assertEquals(2L, loaded?.dueAtEpochMillis)
        db.close()
    }
}
