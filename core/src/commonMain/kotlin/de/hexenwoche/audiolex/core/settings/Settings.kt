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
 * Which ear(s) the trained signal reaches (Backlog M4 "Kopfhörer-Bogen Batch
 * B", ADR-0011 point 5): makes the already-existing, already-tested
 * [de.hexenwoche.audiolex.core.audio.StereoGain] productive for the first
 * time. [BEIDE] is the default so existing installs keep today's plain-mono
 * playback unchanged. Only ever *wirksam* in the detected stereo-headphone
 * setup -- in the hearing-aid setup it is stored like any other setting, but
 * the training screens leave the buffer mono regardless of its value (ADR-0007,
 * ADR-0011 point 5), and the settings UI shows the options disabled there.
 */
enum class ChannelMode { BEIDE, NUR_LINKS, NUR_RECHTS }

/**
 * Which corpus the training screens draw from (Backlog Eigen-Korpus Batch C,
 * ADR-0012): [MITGELIEFERT] is the default so an existing install behaves
 * exactly as before this setting existed -- the new source is opted into,
 * never switched on silently by an update. The merge itself (mapping
 * [de.hexenwoche.audiolex.core.corpus.OwnEntry] onto the shared
 * [de.hexenwoche.audiolex.core.corpus.Word]/[de.hexenwoche.audiolex.core.corpus.AudioRecording]
 * model, combining both sources) lives in `core/corpus/CorpusLoader.kt`, not
 * here -- this enum only ever gets compared, never interpreted, on the
 * `:core` side (see [de.hexenwoche.audiolex.core.corpus.mergeCorpus]'s doc
 * for why the merge function stays unaware of this type entirely).
 */
enum class CorpusSource { MITGELIEFERT, EIGENE, BEIDE }

/** Valid SNR slider range in dB (Backlog M4 AC4); a stored value outside this falls back to [DEFAULT_SNR_DB]. */
const val SNR_DB_MIN = -5
const val SNR_DB_MAX = 20
private const val DEFAULT_SNR_DB = 5
private const val DEFAULT_NOISE_SCENARIO = "restaurant"

/**
 * Persisted app-wide settings, Room-free like the other domain types
 * ([de.hexenwoche.audiolex.core.session.Session]) -- persistence mapping
 * happens at the boundary via [toEntity]/[toDomain]. Currently the theme,
 * the corpus mode, the noise overlay (Backlog M4 "Störgeräusch-Overlay",
 * ADR-0010, [noiseEnabled]/[snrDb]/[noiseScenario] shared by both training
 * modes), the channel selection (Backlog M4 "Kopfhörer-Bogen Batch B",
 * ADR-0011, [channelMode]), and the corpus source (Backlog Eigen-Korpus
 * Batch C, ADR-0012, [corpusSource]).
 */
data class AppSettings(
    val themeMode: ThemeMode,
    val corpusMode: CorpusMode = CorpusMode.WOERTER,
    val noiseEnabled: Boolean = false,
    val snrDb: Int = DEFAULT_SNR_DB,
    val noiseScenario: String = DEFAULT_NOISE_SCENARIO,
    val channelMode: ChannelMode = ChannelMode.BEIDE,
    val corpusSource: CorpusSource = CorpusSource.MITGELIEFERT,
)

/**
 * Maps the stored enum names back to the domain enums, not by ordinal
 * (survives reordering the enums) -- unknown/corrupted names fall back to
 * the defaults defensively instead of crashing. [snrDb] outside
 * [SNR_DB_MIN]/[SNR_DB_MAX] and a blank [noiseScenario] are the "kaputt"
 * cases this guards against; an unknown-but-well-formed [noiseScenario] id
 * (a scenario removed from `noise.json` after being saved) is resolved
 * against the loaded catalog at the composeApp boundary instead, since this
 * mapper has no access to that catalog.
 */
fun SettingsEntity.toDomain(): AppSettings =
    AppSettings(
        themeMode = ThemeMode.entries.firstOrNull { it.name == themeMode } ?: ThemeMode.SYSTEM,
        corpusMode = CorpusMode.entries.firstOrNull { it.name == corpusMode } ?: CorpusMode.WOERTER,
        noiseEnabled = noiseEnabled,
        snrDb = snrDb.takeIf { it in SNR_DB_MIN..SNR_DB_MAX } ?: DEFAULT_SNR_DB,
        noiseScenario = noiseScenario.takeIf { it.isNotBlank() } ?: DEFAULT_NOISE_SCENARIO,
        channelMode = ChannelMode.entries.firstOrNull { it.name == channelMode } ?: ChannelMode.BEIDE,
        corpusSource = CorpusSource.entries.firstOrNull { it.name == corpusSource } ?: CorpusSource.MITGELIEFERT,
    )

fun AppSettings.toEntity(): SettingsEntity =
    SettingsEntity(
        themeMode = themeMode.name,
        corpusMode = corpusMode.name,
        noiseEnabled = noiseEnabled,
        snrDb = snrDb,
        noiseScenario = noiseScenario,
        channelMode = channelMode.name,
        corpusSource = corpusSource.name,
    )

/**
 * The corpus entry kind a mode admits (Backlog M2 Satz-Bogen Batch B, AC3) --
 * the training screens filter with this, so the mapping lives in exactly one
 * place instead of being re-derived per screen.
 */
fun CorpusMode.entryKind(): EntryKind = when (this) {
    CorpusMode.WOERTER -> EntryKind.WORD
    CorpusMode.SAETZE -> EntryKind.SENTENCE
}
