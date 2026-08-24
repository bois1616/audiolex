package de.hexenwoche.audiolex.core.persistence

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import de.hexenwoche.audiolex.core.i18n.UiLanguage
import de.hexenwoche.audiolex.core.settings.toDomain
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exercises [MIGRATION_8_9] (ADR-0015) against a table in the real v8 shape.
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
class Migration8To9Test {

    private val databaseFile: File = File.createTempFile("audiolex-migration-", ".db").apply { delete() }
    private var connection: SQLiteConnection? = null

    @AfterTest
    fun tearDown() {
        connection?.close()
        databaseFile.delete()
    }

    private fun openV8Database(): SQLiteConnection {
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
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `SettingsEntity` VALUES " +
                "(0, 'DARK', 'SAETZE', 1, -5, 'eigen-42', 'NUR_LINKS', '[\"thorsten\"]')",
        )
        return db
    }

    @Test
    fun `adds the uiLanguage column defaulting to SYSTEM`() {
        val db = openV8Database()

        MIGRATION_8_9.migrate(db)

        db.prepare("SELECT uiLanguage FROM SettingsEntity WHERE id = 0").use { statement ->
            check(statement.step()) { "settings row disappeared" }
            assertEquals("SYSTEM", statement.getText(0))
        }
    }

    @Test
    fun `leaves every existing setting untouched`() {
        val db = openV8Database()

        MIGRATION_8_9.migrate(db)

        db.prepare(
            "SELECT themeMode, corpusMode, noiseEnabled, snrDb, noiseScenario, channelMode, excludedSpeakers " +
                "FROM SettingsEntity WHERE id = 0",
        ).use { statement ->
            check(statement.step()) { "settings row disappeared" }
            assertEquals("DARK", statement.getText(0))
            assertEquals("SAETZE", statement.getText(1))
            assertEquals(1, statement.getInt(2))
            assertEquals(-5, statement.getInt(3))
            assertEquals("eigen-42", statement.getText(4))
            assertEquals("NUR_LINKS", statement.getText(5))
            assertEquals("[\"thorsten\"]", statement.getText(6))
        }
    }

    @Test
    fun `the migrated row maps to SYSTEM rather than a corrupted value`() {
        val db = openV8Database()

        MIGRATION_8_9.migrate(db)

        // The column's raw content is only half the answer -- what matters is
        // what `toDomain` makes of it once the app reads it back.
        val entity = db.prepare("SELECT uiLanguage FROM SettingsEntity WHERE id = 0").use { statement ->
            check(statement.step())
            SettingsEntity(themeMode = "DARK", uiLanguage = statement.getText(0))
        }

        assertEquals(UiLanguage.SYSTEM, entity.toDomain().uiLanguage)
    }
}
