package de.hexenwoche.audiolex.core.persistence

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.hexenwoche.audiolex.core.settings.AppSettings
import de.hexenwoche.audiolex.core.settings.ThemeMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Real Room roundtrip (no mocking), same approach as SessionRepositoryTest.
 */
class SettingsRepositoryTest {

    private fun newDatabase(): AudioLexDatabase =
        Room.inMemoryDatabaseBuilder<AudioLexDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()

    private fun newRepository(db: AudioLexDatabase = newDatabase()): SettingsRepository =
        RoomSettingsRepository(db.settingsDao())

    @Test
    fun loadReturnsSystemDefaultForFreshDatabase() = runTest {
        assertEquals(ThemeMode.SYSTEM, newRepository().load().themeMode)
    }

    @Test
    fun savedThemeModeRoundtrips() = runTest {
        val repository = newRepository()

        repository.save(AppSettings(ThemeMode.DARK))

        assertEquals(ThemeMode.DARK, repository.load().themeMode)
    }

    @Test
    fun secondSaveOverwritesTheSingletonRowInsteadOfAddingASecondOne() = runTest {
        // Both saves target the fixed id=0 row (see SettingsEntity docstring),
        // so a second save can only ever overwrite it, never add a row --
        // load() reflecting the second value is the observable proof.
        val repository = newRepository()

        repository.save(AppSettings(ThemeMode.DARK))
        repository.save(AppSettings(ThemeMode.LIGHT))

        assertEquals(ThemeMode.LIGHT, repository.load().themeMode)
    }

    @Test
    fun unknownStoredThemeModeFallsBackToSystem() = runTest {
        val db = newDatabase()
        db.settingsDao().upsert(SettingsEntity(themeMode = "SOME_FUTURE_MODE"))

        val loaded = newRepository(db).load()

        assertEquals(ThemeMode.SYSTEM, loaded.themeMode)
    }
}
