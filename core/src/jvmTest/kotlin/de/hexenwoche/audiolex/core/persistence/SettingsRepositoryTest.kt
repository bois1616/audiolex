package de.hexenwoche.audiolex.core.persistence

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.hexenwoche.audiolex.core.settings.AppSettings
import de.hexenwoche.audiolex.core.settings.ChannelMode
import de.hexenwoche.audiolex.core.settings.CorpusMode
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
    fun loadReturnsWoerterDefaultForFreshDatabase() = runTest {
        assertEquals(CorpusMode.WOERTER, newRepository().load().corpusMode)
    }

    @Test
    fun savedCorpusModeRoundtrips() = runTest {
        val repository = newRepository()

        repository.save(AppSettings(ThemeMode.SYSTEM, CorpusMode.SAETZE))

        assertEquals(CorpusMode.SAETZE, repository.load().corpusMode)
    }

    @Test
    fun unknownStoredCorpusModeFallsBackToWoerter() = runTest {
        val db = newDatabase()
        db.settingsDao().upsert(SettingsEntity(themeMode = "DARK", corpusMode = "GEMISCHT"))

        val loaded = newRepository(db).load()

        assertEquals(CorpusMode.WOERTER, loaded.corpusMode)
        // The corpusMode fallback must not take the other field down with it.
        assertEquals(ThemeMode.DARK, loaded.themeMode)
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

    @Test
    fun loadReturnsNoiseDefaultsForFreshDatabase() = runTest {
        val loaded = newRepository().load()

        assertEquals(false, loaded.noiseEnabled)
        assertEquals(5, loaded.snrDb)
        assertEquals("restaurant", loaded.noiseScenario)
    }

    @Test
    fun savedNoiseSettingsRoundtrip() = runTest {
        val repository = newRepository()

        repository.save(AppSettings(ThemeMode.SYSTEM, noiseEnabled = true, snrDb = -5, noiseScenario = "verkehr"))
        val loaded = repository.load()

        assertEquals(true, loaded.noiseEnabled)
        assertEquals(-5, loaded.snrDb)
        assertEquals("verkehr", loaded.noiseScenario)
    }

    @Test
    fun outOfRangeStoredSnrDbFallsBackToFiveDb() = runTest {
        val db = newDatabase()
        db.settingsDao().upsert(SettingsEntity(themeMode = "SYSTEM", snrDb = 999))

        val loaded = newRepository(db).load()

        assertEquals(5, loaded.snrDb)
        // The snrDb fallback must not take the other fields down with it.
        assertEquals(ThemeMode.SYSTEM, loaded.themeMode)
    }

    @Test
    fun blankStoredNoiseScenarioFallsBackToRestaurant() = runTest {
        val db = newDatabase()
        db.settingsDao().upsert(SettingsEntity(themeMode = "SYSTEM", noiseScenario = ""))

        val loaded = newRepository(db).load()

        assertEquals("restaurant", loaded.noiseScenario)
    }

    @Test
    fun loadReturnsBeideChannelModeDefaultForFreshDatabase() = runTest {
        assertEquals(ChannelMode.BEIDE, newRepository().load().channelMode)
    }

    @Test
    fun savedChannelModeRoundtrips() = runTest {
        val repository = newRepository()

        repository.save(AppSettings(ThemeMode.SYSTEM, channelMode = ChannelMode.NUR_LINKS))

        assertEquals(ChannelMode.NUR_LINKS, repository.load().channelMode)
    }

    @Test
    fun unknownStoredChannelModeFallsBackToBeide() = runTest {
        val db = newDatabase()
        db.settingsDao().upsert(SettingsEntity(themeMode = "DARK", channelMode = "MITTE"))

        val loaded = newRepository(db).load()

        assertEquals(ChannelMode.BEIDE, loaded.channelMode)
        // The channelMode fallback must not take the other field down with it.
        assertEquals(ThemeMode.DARK, loaded.themeMode)
    }
}
