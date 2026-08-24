package de.hexenwoche.audiolex.core.persistence

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import de.hexenwoche.audiolex.core.corpus.CorpusLanguage
import de.hexenwoche.audiolex.core.settings.toDomain
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exercises [MIGRATION_9_10] (ADR-0016) against a table in the real v9 shape --
 * the shape MIGRATION_8_9 leaves behind.
 *
 * The migrations before this one were only ever checked on the device, by
 * pulling the database before and after (AGENTS.md §7). That works, but it
 * checks late and it needs the phone. This one runs the actual `migrate`
 * against actual SQLite, and it asserts the part that would hurt: the
 * settings row still holds the values it held, and the new column arrives
 * with the default the Kotlin side declares.
 *
 * Deliberately *not* a full Room open: reproducing all three tables by hand
 * only to satisfy Room's schema validation would test the hand-written DDL
 * more than the migration. The v8 `SettingsEntity` DDL below is copied from
 * [MIGRATION_7_8], which produced it and was itself verified against a real
 * v7 database off the A53.
 */
class Migration9To10Test {

    private val databaseFile: File = File.createTempFile("audiolex-migration-9-10-", ".db").apply { delete() }
    private var connection: SQLiteConnection? = null

    @AfterTest
    fun tearDown() {
        connection?.close()
        databaseFile.delete()
    }

    private fun openV9Database(): SQLiteConnection {
        val db = BundledSQLiteDriver().open(databaseFile.absolutePath)
        connection = db
        db.execSQL(
            "CREATE TABLE `SettingsEntity` (" +
                "`id` INTEGER NOT NULL, " +
                "`themeMode` TEXT NOT NULL, " +
                "`corpusMode` TEXT NOT NULL, " +
                "`noiseEnabled` INTEGER NOT NULL, " +
                "`snrDb` INTEGER NOT NULL, " +
                "`noiseScenario` TEXT NOT NULL, " +
                "`channelMode` TEXT NOT NULL, " +
                "`excludedSpeakers` TEXT NOT NULL, " +
                "`uiLanguage` TEXT NOT NULL DEFAULT 'SYSTEM', " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `SettingsEntity` VALUES " +
                "(0, 'DARK', 'SAETZE', 1, -5, 'eigen-42', 'NUR_LINKS', '[\"thorsten\"]', 'ENGLISCH')",
        )
        return db
    }

    @Test
    fun `adds the corpusLanguage column defaulting to DEUTSCH`() {
        val db = openV9Database()

        MIGRATION_9_10.migrate(db)

        db.prepare("SELECT corpusLanguage FROM SettingsEntity WHERE id = 0").use { statement ->
            check(statement.step()) { "settings row disappeared" }
            assertEquals("DEUTSCH", statement.getText(0))
        }
    }

    @Test
    fun `leaves every existing setting untouched`() {
        val db = openV9Database()

        MIGRATION_9_10.migrate(db)

        db.prepare(
            "SELECT themeMode, corpusMode, noiseEnabled, snrDb, noiseScenario, channelMode, excludedSpeakers, " +
                "uiLanguage FROM SettingsEntity WHERE id = 0",
        ).use { statement ->
            check(statement.step()) { "settings row disappeared" }
            assertEquals("DARK", statement.getText(0))
            assertEquals("SAETZE", statement.getText(1))
            assertEquals(1, statement.getInt(2))
            assertEquals(-5, statement.getInt(3))
            assertEquals("eigen-42", statement.getText(4))
            assertEquals("NUR_LINKS", statement.getText(5))
            assertEquals("[\"thorsten\"]", statement.getText(6))
            // The language chosen in v9 must survive into v10 -- this is the
            // one column the previous migration added, so it is the natural
            // place for an off-by-one in the INSERT to show up.
            assertEquals("ENGLISCH", statement.getText(7))
        }
    }

    @Test
    fun `the migrated row maps to DEUTSCH rather than a corrupted value`() {
        val db = openV9Database()

        MIGRATION_9_10.migrate(db)

        // The column's raw content is only half the answer -- what matters is
        // what `toDomain` makes of it once the app reads it back.
        val entity = db.prepare("SELECT corpusLanguage FROM SettingsEntity WHERE id = 0").use { statement ->
            check(statement.step())
            SettingsEntity(themeMode = "DARK", corpusLanguage = statement.getText(0))
        }

        assertEquals(CorpusLanguage.DEUTSCH, entity.toDomain().corpusLanguage)
    }
}
