package de.hexenwoche.audiolex.core.persistence

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * The app's persistence root (ADR-0004, Backlog M3 "Fälligkeits-Persistenz";
 * Settings joined in Backlog M4 "Settings-Persistenz-Fundament").
 */
@Database(
    entities = [ReviewCardEntity::class, SessionEntity::class, SettingsEntity::class],
    // v4 -> v5: noiseEnabled/snrDb/noiseScenario columns on SettingsEntity
    // (Backlog M4 "Störgeräusch-Overlay", ADR-0010); the destructive fallback
    // carries the bump like v2 -> v3 -> v4 did.
    version = 5,
    exportSchema = false,
)
@ConstructedBy(AudioLexDatabaseConstructor::class)
abstract class AudioLexDatabase : RoomDatabase() {
    abstract fun reviewCardDao(): ReviewCardDao
    abstract fun sessionDao(): SessionDao
    abstract fun settingsDao(): SettingsDao
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
 *
 * Uses a destructive fallback across schema versions instead of writing a
 * real migration: there are no real user installs yet (pre-release MVP), so
 * wiping and recreating on a version bump is acceptable and far simpler than
 * migration code that would only ever run against empty/test databases.
 */
fun createAudioLexDatabase(builder: RoomDatabase.Builder<AudioLexDatabase>): AudioLexDatabase =
    builder.setDriver(BundledSQLiteDriver()).fallbackToDestructiveMigration(true).build()
