package de.hexenwoche.audiolex.core.settings

import de.hexenwoche.audiolex.core.corpus.EntryKind
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
 * Which corpus entries the training screens work on (Backlog M2 "Satz-Bogen
 * Batch B", ADR-0009 point 4): a plain persisted setting, deliberately *not*
 * a SettingsProfile/preset construct. [WOERTER] is the default so existing
 * installs behave exactly as before the switch existed.
 */
enum class CorpusMode { WOERTER, SAETZE }

/**
 * Persisted app-wide settings, Room-free like the other domain types
 * ([de.hexenwoche.audiolex.core.session.Session]) -- persistence mapping
 * happens at the boundary via [toEntity]/[toDomain]. Currently the theme and
 * the corpus mode; more fields are expected to land here later (see
 * [SettingsEntity]).
 */
data class AppSettings(
    val themeMode: ThemeMode,
    val corpusMode: CorpusMode = CorpusMode.WOERTER,
)

/**
 * Maps the stored enum names back to the domain enums, not by ordinal
 * (survives reordering the enums) -- unknown/corrupted names fall back to
 * the defaults defensively instead of crashing.
 */
fun SettingsEntity.toDomain(): AppSettings =
    AppSettings(
        themeMode = ThemeMode.entries.firstOrNull { it.name == themeMode } ?: ThemeMode.SYSTEM,
        corpusMode = CorpusMode.entries.firstOrNull { it.name == corpusMode } ?: CorpusMode.WOERTER,
    )

fun AppSettings.toEntity(): SettingsEntity =
    SettingsEntity(themeMode = themeMode.name, corpusMode = corpusMode.name)

/**
 * The corpus entry kind a mode admits (Backlog M2 Satz-Bogen Batch B, AC3) --
 * the training screens filter with this, so the mapping lives in exactly one
 * place instead of being re-derived per screen.
 */
fun CorpusMode.entryKind(): EntryKind = when (this) {
    CorpusMode.WOERTER -> EntryKind.WORD
    CorpusMode.SAETZE -> EntryKind.SENTENCE
}
