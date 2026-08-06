package de.hexenwoche.audiolex.core.persistence

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

/**
 * The app's persistence root (ADR-0004, Backlog M3 "Fälligkeits-Persistenz";
 * Settings joined in Backlog M4 "Settings-Persistenz-Fundament").
 */
@Database(
    entities = [ReviewCardEntity::class, SessionEntity::class, SettingsEntity::class],
    // v6 -> v7: corpusSource column on SettingsEntity (Backlog Eigen-Korpus
    // Batch C, ADR-0012). The first bump carried by a real Migration
    // (MIGRATION_6_7 below) instead of the destructive fallback that carried
    // every earlier one (v2 -> ... -> v6, see createAudioLexDatabase's doc
    // for why that stops here).
    version = 7,
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
 * v6 -> v7 (Backlog Eigen-Korpus Batch C, AC2): adds [SettingsEntity.corpusSource]
 * with the same default the Kotlin property already declares, so a row
 * written before this migration ran reads back exactly as if it had always
 * had the column. A plain `ALTER TABLE ... ADD COLUMN` is enough -- Room
 * only ever appends columns to this entity across its whole history, no
 * table has been dropped, renamed, or restructured here (that's still the
 * destructive fallback's job below, kept registered as the safety net for
 * everything this migration doesn't cover).
 */
internal val MIGRATION_6_7 = object : Migration(startVersion = 6, endVersion = 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE SettingsEntity ADD COLUMN corpusSource TEXT NOT NULL DEFAULT 'MITGELIEFERT'",
        )
    }
}

/**
 * Builds the real database from a platform-supplied [RoomDatabase.Builder]
 * (Context-based on Android, file-path-based on jvm) -- :core stays
 * Context-free, the platform-specific builder is created in :composeApp.
 *
 * Every schema bump up to and including v5 -> v6 relied on a destructive
 * fallback instead of a real migration: there were no real user installs
 * yet (pre-release MVP), so wiping and recreating on a version bump was
 * acceptable and far simpler than migration code that would only ever run
 * against empty/test databases. That stopped being true in the week of
 * v6 -> v7 (Backlog Eigen-Korpus Batch C, AC2): the author's A53 now carries
 * real SRS due-dates and a session history, so this bump ships an actual
 * [MIGRATION_6_7] via [RoomDatabase.Builder.addMigrations] instead. The
 * destructive fallback stays registered underneath as the safety net for
 * any *other* version jump (e.g. a skipped version, or a future schema
 * change this migration doesn't cover) -- it just no longer carries the
 * one jump this app is actually shipping right now.
 */
fun createAudioLexDatabase(builder: RoomDatabase.Builder<AudioLexDatabase>): AudioLexDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_6_7)
        .fallbackToDestructiveMigration(true)
        .build()
