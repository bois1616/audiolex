package de.hexenwoche.audiolex.core.persistence

import de.hexenwoche.audiolex.core.settings.AppSettings
import de.hexenwoche.audiolex.core.settings.CorpusMode
import de.hexenwoche.audiolex.core.settings.ThemeMode
import de.hexenwoche.audiolex.core.settings.toDomain
import de.hexenwoche.audiolex.core.settings.toEntity

/**
 * Persists app-wide settings (Backlog M4 "Settings-Persistenz-Fundament").
 * Callers work against this facade, not the DAO/Entity directly.
 */
interface SettingsRepository {
    suspend fun load(): AppSettings
    suspend fun save(settings: AppSettings)
}

/**
 * [load] returns the [AppSettings] defaults ([ThemeMode.SYSTEM],
 * [CorpusMode.WOERTER]) when the singleton row hasn't been written yet --
 * the table starts empty, there is no seeding step.
 */
class RoomSettingsRepository(private val dao: SettingsDao) : SettingsRepository {
    override suspend fun load(): AppSettings = dao.get()?.toDomain() ?: AppSettings(ThemeMode.SYSTEM)
    override suspend fun save(settings: AppSettings) = dao.upsert(settings.toEntity())
}
