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
    // v7 -> v8: corpusSource is replaced by excludedSpeakers (Backlog
    // Eigen-Korpus Batch D, ADR-0012 Nachtrag "Die Quellentrennung wird
    // durch Kontingente ersetzt") -- MIGRATION_7_8 below, the first jump
    // that actually has to *remove* a column, not just append one.
    // v8 -> v9: uiLanguage column on SettingsEntity (ADR-0015, UI-Sprache) --
    // MIGRATION_8_9 below, back to a plain append like MIGRATION_6_7.
    version = 9,
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
 * v7 -> v8 (Backlog Eigen-Korpus Batch D, AC3): drops [SettingsEntity.corpusSource]
 * and adds [SettingsEntity.excludedSpeakers] -- unlike [MIGRATION_6_7], this
 * jump *removes* a column, and plain SQLite has no `ALTER TABLE ... DROP
 * COLUMN` in the general case (only added in SQLite 3.35). Rather than lean
 * on an assumed driver version for something this central to the schema --
 * this project has had a build broken once already by an API level that
 * turned out not to hold, see the Umsetzungslog -- this migration takes the
 * classic, version-independent path instead. The portable fix is the table
 * rebuild: build the new shape under a temporary name, copy the one settings row across
 * (`excludedSpeakers` seeded to `'[]'`, the same "nothing excluded" default
 * Kotlin already declares), drop the old table, rename the new one into its
 * place. Single-row table, so the copy is cheap and there's no index/FK to
 * carry over. The destructive fallback stays registered underneath for any
 * *other* jump, same as it did for [MIGRATION_6_7] -- it must not be the one
 * to carry 7 -> 8, or the settings row (and, more importantly on a real
 * device, the SRS due-dates and session history in the other two tables)
 * would be wiped instead of preserved.
 */
internal val MIGRATION_7_8 = object : Migration(startVersion = 7, endVersion = 8) {
    override fun migrate(connection: SQLiteConnection) {
        // Deliberately shaped exactly like Room's own generated DDL for this
        // entity -- no DEFAULT clauses, because the entity declares no
        // @ColumnInfo(defaultValue = ...) and Kotlin default parameter values
        // produce none. Verified against the real v7 database pulled off the
        // A53, whose Room-created table reads `id` INTEGER NOT NULL,
        // `themeMode` TEXT NOT NULL, ... with defaults nowhere except on the
        // column MIGRATION_6_7 appended. Room would tolerate extra DB-side
        // defaults (its column comparison only checks the default when the
        // *entity* declares one), so this isn't about passing validation --
        // it's that a fresh install and an upgraded install must not end up
        // with two different table shapes for the same schema version. That
        // divergence goes unnoticed until some later migration behaves
        // differently on the two. Every value is supplied explicitly by the
        // INSERT below, so no DEFAULT is needed for correctness either.
        connection.execSQL(
            "CREATE TABLE `SettingsEntity_new` (" +
                "`id` INTEGER NOT NULL, " +
                "`themeMode` TEXT NOT NULL, " +
                "`corpusMode` TEXT NOT NULL, " +
                "`noiseEnabled` INTEGER NOT NULL, " +
                "`snrDb` INTEGER NOT NULL, " +
                "`noiseScenario` TEXT NOT NULL, " +
                "`channelMode` TEXT NOT NULL, " +
                "`excludedSpeakers` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "INSERT INTO `SettingsEntity_new` " +
                "(`id`, `themeMode`, `corpusMode`, `noiseEnabled`, `snrDb`, `noiseScenario`, `channelMode`, `excludedSpeakers`) " +
                "SELECT `id`, `themeMode`, `corpusMode`, `noiseEnabled`, `snrDb`, `noiseScenario`, `channelMode`, '[]' " +
                "FROM `SettingsEntity`",
        )
        connection.execSQL("DROP TABLE `SettingsEntity`")
        connection.execSQL("ALTER TABLE `SettingsEntity_new` RENAME TO `SettingsEntity`")
    }
}

/**
 * v8 -> v9 (ADR-0015): adds [SettingsEntity.uiLanguage] with the same
 * default the Kotlin property declares, so a settings row written before
 * this migration ran reads back as [de.hexenwoche.audiolex.core.i18n.UiLanguage.SYSTEM]
 * -- follow the device language -- exactly as a fresh install would. That
 * default is what makes the upgrade invisible on the author's own device:
 * German phone, German app, before and after.
 *
 * A plain `ALTER TABLE ... ADD COLUMN` is enough here, the same shape as
 * [MIGRATION_6_7]; only [MIGRATION_7_8] needed the table rebuild, because
 * only it had to *drop* a column.
 */
internal val MIGRATION_8_9 = object : Migration(startVersion = 8, endVersion = 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE SettingsEntity ADD COLUMN uiLanguage TEXT NOT NULL DEFAULT 'SYSTEM'",
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
 * [MIGRATION_6_7] via [RoomDatabase.Builder.addMigrations] instead. v7 -> v8
 * (Backlog Eigen-Korpus Batch D, AC3) adds [MIGRATION_7_8] alongside it, the
 * first migration that has to rebuild the table rather than just append a
 * column, and v8 -> v9 (ADR-0015, UI-Sprache) [MIGRATION_8_9] after that.
 * The destructive fallback stays registered underneath as the
 * safety net for any *other* version jump (e.g. a skipped version, or a
 * future schema change neither migration covers) -- it just no longer
 * carries either of the two jumps this app has actually shipped.
 */
fun createAudioLexDatabase(builder: RoomDatabase.Builder<AudioLexDatabase>): AudioLexDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
        .fallbackToDestructiveMigration(true)
        .build()
