package de.hexenwoche.audiolex.core.settings

import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.persistence.SettingsEntity
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

/** Valid SNR slider range in dB (Backlog M4 AC4); a stored value outside this falls back to [DEFAULT_SNR_DB]. */
const val SNR_DB_MIN = -5
const val SNR_DB_MAX = 20
private const val DEFAULT_SNR_DB = 5

/**
 * No scenario chosen. There is no meaningful default id any more: since the
 * three bundled loops were removed (ADR-0014, v0.32.0) the catalog consists
 * of the user's own noises, and a fresh install has none. The empty id is
 * resolved where the catalog is known -- the settings screen adopts the first
 * available scenario as soon as one exists, playback falls back to clean
 * speech while none does (ADR-0010 point 4).
 */
private const val NO_NOISE_SCENARIO = ""

/**
 * One-tap training levels (Backlog M4 "Szenario-Presets Einfach/Schwierig/
 * Fortgeschritten", S9): a preset is a single-tap *applier* of atomic
 * settings -- the atomic values ([AppSettings.noiseEnabled]/
 * [AppSettings.snrDb]) stay the source of truth, and the level itself is
 * never persisted. Which level is currently active (or none) is always
 * re-derived from the atomic values via [derivedProfile], so a stored
 * preset state that could contradict them cannot exist.
 *
 * Each level is exactly two effects on the noise pair, and both SNR values
 * are already-calibrated points of the existing slider rather than newly
 * invented ones: [EINFACH] turns the noise off (the SNR stays whatever it
 * was), [SCHWIERIG] turns it on at the hear-tested +5 dB default,
 * [FORTGESCHRITTEN] at the slider's other stop (−5 dB, noise over the
 * speech). [AppSettings.noiseScenario] deliberately stays out of this: the
 * loops are loudness-normalized since v0.13.0, so difficulty comes from the
 * SNR alone -- the scenario is a choice of surroundings, not of level. The
 * word-side of the levels from the Fachkonzept (word length, phonetic
 * pairs, playback behaviour) does not exist yet (M5) and will later extend
 * *this same* definition, not a new construct.
 *
 * The constructor parameters are the single definition of the level
 * parameters: [applyProfile] and [derivedProfile] both only read them, so
 * the two can never drift apart (a `null` [snrDb] means "leave the current
 * value untouched").
 */
enum class SettingsProfile(val noiseEnabled: Boolean, val snrDb: Int?) {
    /** Noise off; the current SNR stays untouched (`snrDb = null`). */
    EINFACH(noiseEnabled = false, snrDb = null),

    /** Noise on at +5 dB, the hear-tested default (v0.13.0 A53-Hörprobe). */
    SCHWIERIG(noiseEnabled = true, snrDb = DEFAULT_SNR_DB),

    /** Noise on at −5 dB ([SNR_DB_MIN]), the slider's other stop. */
    FORTGESCHRITTEN(noiseEnabled = true, snrDb = SNR_DB_MIN),
}

private val excludedSpeakersJson = Json { ignoreUnknownKeys = true }

/**
 * Persisted app-wide settings, Room-free like the other domain types
 * ([de.hexenwoche.audiolex.core.session.Session]) -- persistence mapping
 * happens at the boundary via [toEntity]/[toDomain]. Currently the theme,
 * the corpus mode, the noise overlay (Backlog M4 "Störgeräusch-Overlay",
 * ADR-0010, [noiseEnabled]/[snrDb]/[noiseScenario] shared by both training
 * modes), the channel selection (Backlog M4 "Kopfhörer-Bogen Batch B",
 * ADR-0011, [channelMode]), and the speaker/contingent selection (Backlog
 * Eigen-Korpus Batch D, ADR-0012 Nachtrag "Die Quellentrennung wird durch
 * Kontingente ersetzt", [excludedSpeakers]).
 */
data class AppSettings(
    val themeMode: ThemeMode,
    val corpusMode: CorpusMode = CorpusMode.WOERTER,
    val noiseEnabled: Boolean = false,
    val snrDb: Int = DEFAULT_SNR_DB,
    val noiseScenario: String = NO_NOISE_SCENARIO,
    val channelMode: ChannelMode = ChannelMode.BEIDE,
    /**
     * Which speaker contingents (Batch D replaces the Batch C `CorpusSource`
     * choice with this) are left *out* of training -- not which ones are
     * picked. Storing the exclusion, not the selection, is deliberate and
     * bindend (ADR-0012 Nachtrag): an empty set means "alle", so a
     * contingent that didn't exist yet when this was last saved is
     * automatically included instead of silently muted, and "alle abwählen"
     * has a state to reach (with a stored *selection* set, "leer" would
     * have to mean "alles" and the button would visibly do nothing).
     * `thorsten` (the built-in corpus) is just one entry in here like any
     * other -- no special-cased default.
     */
    val excludedSpeakers: Set<String> = emptySet(),
)

/**
 * Applies a training level (Backlog M4 "Szenario-Presets", AC1): writes
 * only the atomic noise pair, everything else (theme, corpus, scenario,
 * channels, contingents) stays preset-neutral. The parameters come from
 * [SettingsProfile]'s constructor -- the single definition of the level
 * parameters -- so this mapper can neither invent nor forget one. A `null`
 * level SNR ([SettingsProfile.EINFACH]) leaves the current [snrDb]
 * untouched. Persistence runs over the regular settings path
 * (composeApp state -> `SettingsRepository`), identical to a manual change.
 */
fun AppSettings.applyProfile(profile: SettingsProfile): AppSettings =
    copy(noiseEnabled = profile.noiseEnabled, snrDb = profile.snrDb ?: snrDb)

/**
 * Derives which training level the current atomic values correspond to
 * (`null` = none, "Individuell eingestellt"): noise off is always
 * [SettingsProfile.EINFACH] whatever the SNR, noise on matches the level
 * whose SNR stop it sits on (+5 dB [SettingsProfile.SCHWIERIG], −5 dB
 * [SettingsProfile.FORTGESCHRITTEN]), any other SNR matches nothing. The
 * match reads the parameter table off [SettingsProfile] itself instead of
 * duplicating its literals, so a level added or re-tuned there changes the
 * derivation with it.
 */
fun AppSettings.derivedProfile(): SettingsProfile? =
    SettingsProfile.entries.firstOrNull { profile ->
        profile.noiseEnabled == noiseEnabled && (profile.snrDb == null || profile.snrDb == snrDb)
    }

/**
 * Maps the stored enum names back to the domain enums, not by ordinal
 * (survives reordering the enums) -- unknown/corrupted names fall back to
 * the defaults defensively instead of crashing. [snrDb] outside
 * [SNR_DB_MIN]/[SNR_DB_MAX] is the "kaputt" case this guards against.
 *
 * [noiseScenario] passes through untouched, blank included: blank *is* the
 * "keins gewählt" value since the bundled loops were removed (v0.32.0), no
 * longer a corrupted one. Any id the catalog doesn't hold -- a deleted own
 * noise, or one of the removed bundled ids stored by an older version -- is
 * resolved against the loaded catalog at the composeApp boundary, since this
 * mapper has no access to that catalog.
 */
fun SettingsEntity.toDomain(): AppSettings =
    AppSettings(
        themeMode = ThemeMode.entries.firstOrNull { it.name == themeMode } ?: ThemeMode.SYSTEM,
        corpusMode = CorpusMode.entries.firstOrNull { it.name == corpusMode } ?: CorpusMode.WOERTER,
        noiseEnabled = noiseEnabled,
        snrDb = snrDb.takeIf { it in SNR_DB_MIN..SNR_DB_MAX } ?: DEFAULT_SNR_DB,
        noiseScenario = noiseScenario,
        channelMode = ChannelMode.entries.firstOrNull { it.name == channelMode } ?: ChannelMode.BEIDE,
        excludedSpeakers = parseExcludedSpeakers(excludedSpeakers),
    )

fun AppSettings.toEntity(): SettingsEntity =
    SettingsEntity(
        themeMode = themeMode.name,
        corpusMode = corpusMode.name,
        noiseEnabled = noiseEnabled,
        snrDb = snrDb,
        noiseScenario = noiseScenario,
        channelMode = channelMode.name,
        excludedSpeakers = encodeExcludedSpeakers(excludedSpeakers),
    )

/**
 * Parses the [SettingsEntity.excludedSpeakers] column (Backlog Eigen-Korpus
 * Batch D, AC2): a JSON array of speaker names, same "datengetriebene Werte
 * als String, nicht als Enum" precedent as [SettingsEntity.noiseScenario].
 * Same defensive posture as [de.hexenwoche.audiolex.core.corpus.parseOwnCorpus]:
 * anything that isn't a valid JSON array of strings -- truncated, hand-
 * edited, a stray `null` -- yields the empty set rather than throwing. That's
 * deliberately also the harmless outcome (an empty exclusion set means
 * "alle", the exact fallback [AppSettings.excludedSpeakers] already defaults
 * to), not a special "corrupted" state that needs its own handling.
 */
fun parseExcludedSpeakers(json: String): Set<String> =
    try {
        excludedSpeakersJson.decodeFromString<Set<String>>(json)
    } catch (e: SerializationException) {
        emptySet()
    } catch (e: IllegalArgumentException) {
        emptySet()
    }

/** Counterpart to [parseExcludedSpeakers] -- serializes the set back to the column's JSON-array text. */
fun encodeExcludedSpeakers(speakers: Set<String>): String = excludedSpeakersJson.encodeToString(speakers)

/**
 * The corpus entry kind a mode admits (Backlog M2 Satz-Bogen Batch B, AC3) --
 * the training screens filter with this, so the mapping lives in exactly one
 * place instead of being re-derived per screen.
 */
fun CorpusMode.entryKind(): EntryKind = when (this) {
    CorpusMode.WOERTER -> EntryKind.WORD
    CorpusMode.SAETZE -> EntryKind.SENTENCE
}
