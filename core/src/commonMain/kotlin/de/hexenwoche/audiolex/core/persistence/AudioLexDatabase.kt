package de.hexenwoche.audiolex.core.persistence

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * The app's persistence root (ADR-0004, Backlog M3 "Fälligkeits-Persistenz").
 * Scoped to ReviewCard fälligkeiten for now; session history and settings
 * are separate schemas/items, not entities here.
 */
@Database(entities = [ReviewCardEntity::class], version = 1, exportSchema = false)
@ConstructedBy(AudioLexDatabaseConstructor::class)
abstract class AudioLexDatabase : RoomDatabase() {
    abstract fun reviewCardDao(): ReviewCardDao
}

// The Room KSP compiler generates the actual implementation per target --
// no actual declaration is written here on purpose (see ADR-0004).
@Suppress("KotlinNoActualForExpect")
expect object AudioLexDatabaseConstructor : RoomDatabaseConstructor<AudioLexDatabase> {
    override fun initialize(): AudioLexDatabase
}

/**
 * Builds the real database from a platform-supplied [RoomDatabase.Builder]
 * (Context-based on Android, file-path-based on jvm) -- :core stays
 * Context-free, the platform-specific builder is created in :composeApp.
 */
fun createAudioLexDatabase(builder: RoomDatabase.Builder<AudioLexDatabase>): AudioLexDatabase =
    builder.setDriver(BundledSQLiteDriver()).build()
