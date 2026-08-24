package de.hexenwoche.audiolex

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.i18n.UiLanguage
import de.hexenwoche.audiolex.core.i18n.stringsFor
import de.hexenwoche.audiolex.core.persistence.AudioLexDatabase
import de.hexenwoche.audiolex.core.persistence.RoomReviewCardRepository
import de.hexenwoche.audiolex.core.persistence.RoomSessionRepository
import de.hexenwoche.audiolex.core.persistence.RoomSettingsRepository
import de.hexenwoche.audiolex.core.settings.AppSettings
import de.hexenwoche.audiolex.core.settings.ThemeMode
import de.hexenwoche.audiolex.core.settings.applyProfile
import de.hexenwoche.audiolex.core.settings.derivedProfile
import de.hexenwoche.audiolex.core.time.Clock
import kotlinx.coroutines.launch

/**
 * Flat navigation (DESIGN.md "Screenstruktur"): Start is the hub, training
 * screens are dead ends that return to Start via "Beenden". No navigation
 * library -- at most two levels, a simple state switch is enough.
 */
private sealed interface Screen {
    data object Start : Screen
    data object Lernmodus : Screen
    data object Pruefmodus : Screen
    data object Einstellungen : Screen
    data object Sitzungshistorie : Screen
    data object Impressum : Screen
    data object Kurzanleitung : Screen
    data object EigeneAufnahmen : Screen
    data object EigeneStoergeraeusche : Screen
    data object DevKanaltest : Screen
}

// Entry point and navigation host (Start/Lernmodus/Prüfmodus, see DESIGN.md).
// [database] and [clock] are created once per process by the platform entry
// point (MainActivity/main) and handed down -- the SQLite file isn't reopened
// on every screen visit, and the time source stays injectable (ADR-0008).
// [ownCorpusDir] is the same pattern for the own-corpus WAVs + metadata JSON
// (Backlog Eigen-Korpus Batch B, ADR-0012 Nachtrag) -- a plain absolute path
// from the platform-specific `getOwnCorpusDir`, not the database itself,
// since the own corpus deliberately isn't in Room (Nachtrag "JSON statt
// Datenbank"). [ownNoiseDir] is the same pattern again for the own noise
// loops (Backlog M4 "Eigene Störgeräusche", AC1), resolved by the
// platform-specific `getOwnNoiseDir`.
@Composable
fun App(database: AudioLexDatabase, clock: Clock, onExitApp: () -> Unit, ownCorpusDir: String, ownNoiseDir: String) {
    val scope = rememberCoroutineScope()
    val settingsRepository = remember(database) { RoomSettingsRepository(database.settingsDao()) }
    // JSON-backed, not Room (ADR-0012 Nachtrag) -- created once per process
    // like the other repositories above, from the directory the platform
    // entry point resolved.
    val ownCorpusRepository = remember(ownCorpusDir) {
        OwnCorpusRepository(createOwnCorpusFiles(ownCorpusDir), clock)
    }
    val ownNoiseRepository = remember(ownNoiseDir) {
        OwnNoiseRepository(createOwnNoiseFiles(ownNoiseDir), clock)
    }
    // Hoisted out of the individual screens: three of them need it now that
    // the backup covers the session history too (ADR-0013 Nachtrag), and
    // building it per call site meant three instances over one DAO.
    val sessionRepository = remember(database) { RoomSessionRepository(database.sessionDao()) }
    // One state object for all persisted settings (Backlog "Code-Qualität":
    // five separate states + five near-identical save blocks used to mean
    // every new field had to be maintained in 6+ places). Renders on the
    // defaults until the LaunchedEffect below loads the persisted values --
    // a brief, one-time re-theme on a non-default setting, accepted for the
    // MVP (Architektur-Notiz Backlog M4). The UI language (ADR-0015) rides
    // along in the same state and inherits the same caveat, with the same
    // shape of default: UiLanguage.SYSTEM follows the device, so the only
    // install that can flicker is one whose stored language contradicts its
    // device -- an explicit English choice on a German phone, or the
    // reverse.
    var settings by remember { mutableStateOf(AppSettings(ThemeMode.SYSTEM)) }
    LaunchedEffect(Unit) {
        settings = settingsRepository.load()
    }

    // Immediate state update + a single async persist, the only save path
    // for every change handler -- the old per-field handlers each rebuilt a
    // full AppSettings(...) from the individual states, so a forgotten
    // field would have been silently reset to its default.
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(settings)
        settings = updated
        scope.launch { settingsRepository.save(updated) }
    }

    // Resolved per recomposition rather than remembered: `stringsFor` is a
    // `when` over two objects, and SYSTEM has to keep following the device
    // language even when that changes underneath a live process (ADR-0015).
    val strings = stringsFor(settings.uiLanguage)

    AudioLexTheme(settings.themeMode) {
        CompositionLocalProvider(LocalStrings provides strings) {
            Surface(modifier = Modifier.fillMaxSize()) {
                // The Surface above stays edge-to-edge -- it keeps painting its
                // background color under the status/navigation bars, so the
                // borderless look from MainActivity's enableEdgeToEdge() is
                // unchanged. Only the *content* below insets from the system
                // bars, so the bottom-pinned button on every screen
                // (Sitzungshistorie "Zurück", Einstellungen "Zurück", StartScreen
                // "App beenden"/version line) lands fully above the navigation
                // bar instead of half under it (Backlog M2 "System-
                // Navigationsleiste überlappt", A53-Befund 2026-08-06). One
                // insets consumer at the root instead of per screen. On Desktop
                // the insets are empty, so this is a no-op there.
                Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Start) }

                    when (val current = screen) {
                        is Screen.Start -> StartScreen(
                            onStartLernmodus = { screen = Screen.Lernmodus },
                            onStartPruefmodus = { screen = Screen.Pruefmodus },
                            onOpenEinstellungen = { screen = Screen.Einstellungen },
                            onOpenSitzungshistorie = { screen = Screen.Sitzungshistorie },
                            onOpenImpressum = { screen = Screen.Impressum },
                            onOpenKurzanleitung = { screen = Screen.Kurzanleitung },
                            onOpenEigeneAufnahmen = { screen = Screen.EigeneAufnahmen },
                            onOpenDevKanaltest = { screen = Screen.DevKanaltest },
                            onExitApp = onExitApp,
                            // The picker always writes a concrete language, never
                            // SYSTEM: once someone has said which language they
                            // want, following the device would be a second, silent
                            // opinion on top of theirs.
                            activeLanguage = settings.uiLanguage.resolve(),
                            onLanguageChange = { newLanguage ->
                                updateSettings { it.copy(uiLanguage = newLanguage) }
                            },
                        )
                        is Screen.Lernmodus -> LernmodusScreen(
                            corpusMode = settings.corpusMode,
                            corpusLanguage = settings.corpusLanguage,
                            noiseEnabled = settings.noiseEnabled,
                            snrDb = settings.snrDb,
                            noiseScenario = settings.noiseScenario,
                            channelMode = settings.channelMode,
                            excludedSpeakers = settings.excludedSpeakers,
                            ownCorpusRepository = ownCorpusRepository,
                            ownNoiseRepository = ownNoiseRepository,
                            onBeenden = { screen = Screen.Start },
                        )
                        is Screen.Pruefmodus -> PruefmodusScreen(
                            repository = remember(database) { RoomReviewCardRepository(database.reviewCardDao()) },
                            sessionRepository = sessionRepository,
                            clock = clock,
                            corpusMode = settings.corpusMode,
                            corpusLanguage = settings.corpusLanguage,
                            noiseEnabled = settings.noiseEnabled,
                            snrDb = settings.snrDb,
                            noiseScenario = settings.noiseScenario,
                            channelMode = settings.channelMode,
                            excludedSpeakers = settings.excludedSpeakers,
                            ownCorpusRepository = ownCorpusRepository,
                            ownNoiseRepository = ownNoiseRepository,
                            onBeenden = { screen = Screen.Start },
                            onZumLernmodus = { screen = Screen.Lernmodus },
                        )
                        is Screen.Einstellungen -> EinstellungenScreen(
                            // The level is never persisted -- the option state
                            // is re-derived from the atomic noise pair on every
                            // recomposition, and tapping one applies it over the
                            // regular settings path (Backlog M4
                            // "Szenario-Presets", AC2/AC3).
                            activeProfile = settings.derivedProfile(),
                            onProfileChange = { profile ->
                                updateSettings { it.applyProfile(profile) }
                            },
                            themeMode = settings.themeMode,
                            onThemeModeChange = { newMode ->
                                updateSettings { it.copy(themeMode = newMode) }
                            },
                            corpusMode = settings.corpusMode,
                            onCorpusModeChange = { newMode ->
                                updateSettings { it.copy(corpusMode = newMode) }
                            },
                            corpusLanguage = settings.corpusLanguage,
                            onCorpusLanguageChange = { newLanguage ->
                                updateSettings { it.copy(corpusLanguage = newLanguage) }
                            },
                            noiseEnabled = settings.noiseEnabled,
                            onNoiseEnabledChange = { newEnabled ->
                                updateSettings { it.copy(noiseEnabled = newEnabled) }
                            },
                            snrDb = settings.snrDb,
                            onSnrDbChange = { newSnrDb ->
                                updateSettings { it.copy(snrDb = newSnrDb) }
                            },
                            noiseScenario = settings.noiseScenario,
                            onNoiseScenarioChange = { newScenario ->
                                updateSettings { it.copy(noiseScenario = newScenario) }
                            },
                            channelMode = settings.channelMode,
                            onChannelModeChange = { newMode ->
                                updateSettings { it.copy(channelMode = newMode) }
                            },
                            excludedSpeakers = settings.excludedSpeakers,
                            onExcludedSpeakersChange = { newExcludedSpeakers ->
                                updateSettings { it.copy(excludedSpeakers = newExcludedSpeakers) }
                            },
                            ownCorpusRepository = ownCorpusRepository,
                            ownNoiseRepository = ownNoiseRepository,
                            onOpenEigeneStoergeraeusche = { screen = Screen.EigeneStoergeraeusche },
                            onBeenden = { screen = Screen.Start },
                        )
                        is Screen.Sitzungshistorie -> SitzungshistorieScreen(
                            repository = sessionRepository,
                            onBeenden = { screen = Screen.Start },
                        )
                        is Screen.Impressum -> ImpressumScreen(onBeenden = { screen = Screen.Start })
                        is Screen.Kurzanleitung -> KurzanleitungScreen(onBeenden = { screen = Screen.Start })
                        is Screen.EigeneAufnahmen -> EigeneAufnahmenScreen(
                            repository = ownCorpusRepository,
                            // The backup covers the session history too (ADR-0013
                            // Nachtrag), which is why this screen needs a second
                            // repository it otherwise has no use for.
                            sessionRepository = sessionRepository,
                            // And, since Backlog M4 "Eigene Störgeräusche" (AC5),
                            // the own noises as well -- the backup section lives
                            // here, so this screen hands all three to it.
                            ownNoiseRepository = ownNoiseRepository,
                            clock = clock,
                            corpusLanguage = settings.corpusLanguage,
                            onBeenden = { screen = Screen.Start },
                        )
                        is Screen.EigeneStoergeraeusche -> EigeneStoergeraeuscheScreen(
                            repository = ownNoiseRepository,
                            clock = clock,
                            // Reached from the Einstellungen noise section; the
                            // way back is where it came from, not the Start hub.
                            onBeenden = { screen = Screen.Einstellungen },
                        )
                        is Screen.DevKanaltest -> DevKanaltestScreen(
                            ownCorpusRepository = ownCorpusRepository,
                            onBeenden = { screen = Screen.Start },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StartScreen(
    onStartLernmodus: () -> Unit,
    onStartPruefmodus: () -> Unit,
    onOpenEinstellungen: () -> Unit,
    onOpenSitzungshistorie: () -> Unit,
    onOpenImpressum: () -> Unit,
    onOpenKurzanleitung: () -> Unit,
    onOpenEigeneAufnahmen: () -> Unit,
    onOpenDevKanaltest: () -> Unit,
    onExitApp: () -> Unit,
    activeLanguage: UiLanguage,
    onLanguageChange: (UiLanguage) -> Unit,
) {
    val strings = LocalStrings.current

    // The hub has to survive a viewport shorter than its content -- a desktop
    // window sized down, a phone held sideways, a large system font scale.
    // It used to pin the quiet zone to the bottom with a weighted Spacer,
    // which has no fallback: whatever doesn't fit is simply clipped, and an
    // unreachable button has been a real bug in this project three times
    // (Sitzungshistorie, Einstellungen, and the entry actions on the A53).
    // Adding the language row and the Kurzanleitung pushed the default
    // 800x600 desktop window past that edge, which is how this surfaced.
    //
    // `heightIn(min = viewport)` plus `Arrangement.SpaceBetween` does both
    // jobs at once: with room to spare the column is exactly one viewport
    // tall and the two groups are pushed apart, which is the look the
    // weighted Spacer gave; once the content is taller the column grows past
    // the viewport and the scroll around it takes over. A weighted Spacer
    // cannot do this -- inside a scrollable parent the available height is
    // unbounded, so its weight resolves to zero.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight)
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("AudioLex", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        strings.appSubtitle,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = onStartLernmodus) {
                        Text(strings.startLearningMode)
                    }
                    Button(onClick = onStartPruefmodus) {
                        Text(strings.startExamMode)
                    }
                    Button(onClick = onOpenSitzungshistorie) {
                        Text(strings.sessionHistory)
                    }
                    Button(onClick = onOpenEinstellungen) {
                        Text(strings.settings)
                    }
                    // Eigen-Korpus Batch B (ADR-0012): entry point for the
                    // own-recordings maske + management list, alongside the
                    // other hub navigation.
                    Button(onClick = onOpenEigeneAufnahmen) {
                        Text(strings.ownRecordings)
                    }
                }

                // The quiet zone: set once, read once, or reference. Tighter
                // spacing than the training actions above so it reads as one
                // group rather than five more choices -- Material already
                // gives every TextButton a 48dp touch target, so the smaller
                // gap costs no tap accuracy.
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Language picker (ADR-0015), down here with the other
                    // set-once entries rather than above the training
                    // buttons: it is touched once per install, and the start
                    // screen's job is to reach the first word in two taps
                    // (DESIGN.md Leitprinzip 6). It does have to live on
                    // *this* screen and not in the settings -- someone who
                    // cannot read the current language must not have to guess
                    // which of five German buttons opens them
                    // (Autor-Requirement).
                    LanguageRow(active = activeLanguage, onChange = onLanguageChange)
                    TextButton(onClick = onOpenKurzanleitung) {
                        Text(
                            strings.quickGuide,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // "App beenden" is deliberately a TextButton (not the
                    // filled Button used for the training actions above) with
                    // a muted color -- it must recede, not compete with the
                    // primary navigation (DESIGN.md "Sekundäres tritt
                    // zurück"). No confirmation dialog: a restart makes this
                    // trivially reversible (Autor-Requirement 2026-07-19).
                    TextButton(onClick = onExitApp) {
                        Text(
                            strings.exitApp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onOpenImpressum) {
                        Text(
                            strings.imprintAndPrivacy,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // The version line doubles as the way into the
                    // Dev-Kanaltest (Backlog M4 "Kanaltest aus dem
                    // Regelbetrieb ausblenden, aber erreichbar halten", AC1):
                    // a long press opens it, a normal tap does nothing. The
                    // tool stays available in every build -- it is the
                    // instrument for the channel work and was already that
                    // during the Re-Test-Protokoll of 2026-07-08 -- but it no
                    // longer sits between the training actions as if it were
                    // one of them.
                    //
                    // Deliberately without ripple or any visual hint: a
                    // discoverable affordance here would put the developer
                    // tool back into the user's field of view, which is
                    // exactly what this item removes. Whoever needs it knows
                    // about it.
                    Text(
                        "v$VERSION_NAME",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp).combinedClickable(
                            interactionSource = null,
                            indication = null,
                            onLongClick = onOpenDevKanaltest,
                            onClick = {},
                        ),
                    )
                }
            }
        }
    }
}

/**
 * The two selectable languages side by side (ADR-0015).
 *
 * [UiLanguage.SYSTEM] is deliberately not an option. It is the *initial*
 * state, not a choice worth offering: a third button would ask the user to
 * think about locale inheritance on a screen whose job is to start a
 * training session. Which language SYSTEM currently resolves to is what the
 * accent color already shows.
 */
@Composable
private fun LanguageRow(active: UiLanguage, onChange: (UiLanguage) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (language in UiLanguage.entries) {
            if (language == UiLanguage.SYSTEM) continue
            val isActive = language == active
            TextButton(onClick = { onChange(language) }) {
                Text(
                    language.nativeName,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun DevKanaltestScreen(ownCorpusRepository: OwnCorpusRepository, onBeenden: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // The dev channel test itself stays German (ADR-0015 Nicht-Ziele) --
        // it is an instrument, not a product screen, and it is reachable only
        // by a long press nobody finds by accident. Its way out is shared UI,
        // so that one does follow the language.
        Button(onClick = onBeenden) {
            Text(LocalStrings.current.back)
        }
        DevPlaybackScreen(ownCorpusRepository)
    }
}
