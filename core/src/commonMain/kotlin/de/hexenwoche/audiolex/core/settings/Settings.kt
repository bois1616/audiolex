package de.hexenwoche.audiolex.core.settings

import de.hexenwoche.audiolex.core.persistence.SettingsEntity

/**
 * Manual theme override (Backlog M4 "Settings-Persistenz-Fundament"): the
 * app previously followed `isSystemInDarkTheme()` unconditionally, with no
 * way to tell whether Dark Mode actually engaged (Autor-Finding
 * 2026-07-13). [SYSTEM] keeps that behaviour as the default; [LIGHT]/[DARK]
 * force a mode regardless of the OS setting.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Persisted app-wide settings, Room-free like the other domain types
 * ([de.hexenwoche.audiolex.core.session.Session]) -- persistence mapping
 * happens at the boundary via [toEntity]/[toDomain]. Currently just the
 * theme; more fields are expected to land here later (see
 * [SettingsEntity]).
 */
data class AppSettings(val themeMode: ThemeMode)

/**
 * Maps the stored enum name back to [ThemeMode], not by ordinal (survives
 * reordering the enum) -- an unknown/corrupted name falls back to [ThemeMode.SYSTEM]
 * defensively instead of crashing.
 */
fun SettingsEntity.toDomain(): AppSettings =
    AppSettings(themeMode = ThemeMode.entries.firstOrNull { it.name == themeMode } ?: ThemeMode.SYSTEM)

fun AppSettings.toEntity(): SettingsEntity = SettingsEntity(themeMode = themeMode.name)
