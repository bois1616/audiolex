package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.persistence.AudioLexDatabase
import de.hexenwoche.audiolex.core.persistence.RoomReviewCardRepository
import de.hexenwoche.audiolex.core.persistence.RoomSessionRepository
import de.hexenwoche.audiolex.core.persistence.RoomSettingsRepository
import de.hexenwoche.audiolex.core.settings.AppSettings
import de.hexenwoche.audiolex.core.settings.ThemeMode
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
    data object EigeneAufnahmen : Screen
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
// Datenbank").
@Composable
fun App(database: AudioLexDatabase, clock: Clock, onExitApp: () -> Unit, ownCorpusDir: String) {
    val scope = rememberCoroutineScope()
    val settingsRepository = remember(database) { RoomSettingsRepository(database.settingsDao()) }
    // JSON-backed, not Room (ADR-0012 Nachtrag) -- created once per process
    // like the other repositories above, from the directory the platform
    // entry point resolved.
    val ownCorpusRepository = remember(ownCorpusDir) {
        OwnCorpusRepository(createOwnCorpusFiles(ownCorpusDir), clock)
    }
    // One state object for all persisted settings (Backlog "Code-Qualität":
    // five separate states + five near-identical save blocks used to mean
    // every new field had to be maintained in 6+ places). Renders on the
    // defaults until the LaunchedEffect below loads the persisted values --
    // a brief, one-time re-theme on a non-default setting, accepted for the
    // MVP (Architektur-Notiz Backlog M4).
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

    AudioLexTheme(settings.themeMode) {
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
                        onOpenEigeneAufnahmen = { screen = Screen.EigeneAufnahmen },
                        onOpenDevKanaltest = { screen = Screen.DevKanaltest },
                        onExitApp = onExitApp,
                    )
                    is Screen.Lernmodus -> LernmodusScreen(
                        corpusMode = settings.corpusMode,
                        noiseEnabled = settings.noiseEnabled,
                        snrDb = settings.snrDb,
                        noiseScenario = settings.noiseScenario,
                        channelMode = settings.channelMode,
                        corpusSource = settings.corpusSource,
                        ownCorpusRepository = ownCorpusRepository,
                        onBeenden = { screen = Screen.Start },
                    )
                    is Screen.Pruefmodus -> PruefmodusScreen(
                        repository = remember(database) { RoomReviewCardRepository(database.reviewCardDao()) },
                        sessionRepository = remember(database) { RoomSessionRepository(database.sessionDao()) },
                        clock = clock,
                        corpusMode = settings.corpusMode,
                        noiseEnabled = settings.noiseEnabled,
                        snrDb = settings.snrDb,
                        noiseScenario = settings.noiseScenario,
                        channelMode = settings.channelMode,
                        corpusSource = settings.corpusSource,
                        ownCorpusRepository = ownCorpusRepository,
                        onBeenden = { screen = Screen.Start },
                        onZumLernmodus = { screen = Screen.Lernmodus },
                    )
                    is Screen.Einstellungen -> EinstellungenScreen(
                        themeMode = settings.themeMode,
                        onThemeModeChange = { newMode ->
                            updateSettings { it.copy(themeMode = newMode) }
                        },
                        corpusMode = settings.corpusMode,
                        onCorpusModeChange = { newMode ->
                            updateSettings { it.copy(corpusMode = newMode) }
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
                        corpusSource = settings.corpusSource,
                        onCorpusSourceChange = { newSource ->
                            updateSettings { it.copy(corpusSource = newSource) }
                        },
                        onBeenden = { screen = Screen.Start },
                    )
                    is Screen.Sitzungshistorie -> SitzungshistorieScreen(
                        repository = remember(database) { RoomSessionRepository(database.sessionDao()) },
                        onBeenden = { screen = Screen.Start },
                    )
                    is Screen.Impressum -> ImpressumScreen(onBeenden = { screen = Screen.Start })
                    is Screen.EigeneAufnahmen -> EigeneAufnahmenScreen(
                        repository = ownCorpusRepository,
                        clock = clock,
                        onBeenden = { screen = Screen.Start },
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

@Composable
private fun StartScreen(
    onStartLernmodus: () -> Unit,
    onStartPruefmodus: () -> Unit,
    onOpenEinstellungen: () -> Unit,
    onOpenSitzungshistorie: () -> Unit,
    onOpenImpressum: () -> Unit,
    onOpenEigeneAufnahmen: () -> Unit,
    onOpenDevKanaltest: () -> Unit,
    onExitApp: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("AudioLex", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Hörtraining: Klang → Wort → Bedeutung",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onStartLernmodus) {
            Text("Lernmodus starten")
        }
        Button(onClick = onStartPruefmodus) {
            Text("Prüfmodus starten")
        }
        Button(onClick = onOpenSitzungshistorie) {
            Text("Sitzungshistorie")
        }
        Button(onClick = onOpenEinstellungen) {
            Text("Einstellungen")
        }
        // Eigen-Korpus Batch B (ADR-0012): entry point for the own-recordings
        // maske + management list, alongside the other hub navigation.
        Button(onClick = onOpenEigeneAufnahmen) {
            Text("Eigene Aufnahmen")
        }
        Button(onClick = onOpenDevKanaltest) {
            Text("Kanaltest (Dev)")
        }
        // Version pinned quietly at the bottom so a device test can always
        // tell which build is running (Autor-Wunsch 2026-07-13). Muted color,
        // never the accent -- it's reference info, not an active element.
        Spacer(modifier = Modifier.weight(1f))
        // "App beenden" is deliberately a TextButton (not the filled Button
        // used for the training actions above) with a muted color -- it must
        // recede, not compete with the primary navigation (DESIGN.md
        // "Sekundäres tritt zurück"). No confirmation dialog: a restart makes
        // this trivially reversible (Autor-Requirement 2026-07-19).
        TextButton(onClick = onExitApp) {
            Text(
                "App beenden",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Same quiet TextButton pattern as "App beenden" directly above --
        // the entry point into Impressum/Datenschutz needs to exist, but
        // must not compete with the training actions (Backlog M2
        // "Impressum-/Datenschutz-Seite").
        TextButton(onClick = onOpenImpressum) {
            Text(
                "Impressum & Datenschutz",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "v$VERSION_NAME",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DevKanaltestScreen(ownCorpusRepository: OwnCorpusRepository, onBeenden: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = onBeenden) {
            Text("Beenden")
        }
        DevPlaybackScreen(ownCorpusRepository)
    }
}
