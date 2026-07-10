package de.hexenwoche.audiolex.core.persistence

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Upsert

/**
 * Throwaway persistence spike (Backlog M3, ADR-0004): proves Room + KSP
 * builds and runs across this project's actual targets (androidTarget,
 * jvm) before any real schema is designed. Delete once the real
 * ReviewCard/Session schema replaces it.
 */
@Entity
data class SpikeCardEntity(
    @PrimaryKey val wordId: String,
    val dueAtEpochMillis: Long,
)

@Dao
interface SpikeCardDao {
    @Upsert
    suspend fun upsert(card: SpikeCardEntity)

    @Query("SELECT * FROM SpikeCardEntity WHERE wordId = :wordId")
    suspend fun findByWordId(wordId: String): SpikeCardEntity?
}

@Database(entities = [SpikeCardEntity::class], version = 1, exportSchema = false)
@ConstructedBy(SpikeDatabaseConstructor::class)
abstract class SpikeDatabase : RoomDatabase() {
    abstract fun spikeCardDao(): SpikeCardDao
}

// The Room KSP compiler generates the actual implementation per target --
// no actual declaration is written here on purpose.
@Suppress("KotlinNoActualForExpect")
expect object SpikeDatabaseConstructor : RoomDatabaseConstructor<SpikeDatabase> {
    override fun initialize(): SpikeDatabase
}
