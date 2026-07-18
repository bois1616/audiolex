package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    data object DevKanaltest : Screen
}

// Entry point and navigation host (Start/Lernmodus/Prüfmodus, see DESIGN.md).
// [database] and [clock] are created once per process by the platform entry
// point (MainActivity/main) and handed down -- the SQLite file isn't reopened
// on every screen visit, and the time source stays injectable (ADR-0008).
@Composable
fun App(database: AudioLexDatabase, clock: Clock) {
    val scope = rememberCoroutineScope()
    val settingsRepository = remember(database) { RoomSettingsRepository(database.settingsDao()) }
    // Renders in the SYSTEM default until the LaunchedEffect below loads the
    // persisted value -- a brief, one-time re-theme on a non-default setting,
    // accepted for the MVP (Architektur-Notiz Backlog M4).
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    LaunchedEffect(Unit) {
        themeMode = settingsRepository.load().themeMode
    }

    AudioLexTheme(themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            var screen by remember { mutableStateOf<Screen>(Screen.Start) }

            when (val current = screen) {
                is Screen.Start -> StartScreen(
                    onStartLernmodus = { screen = Screen.Lernmodus },
                    onStartPruefmodus = { screen = Screen.Pruefmodus },
                    onOpenEinstellungen = { screen = Screen.Einstellungen },
                    onOpenSitzungshistorie = { screen = Screen.Sitzungshistorie },
                    onOpenDevKanaltest = { screen = Screen.DevKanaltest },
                )
                is Screen.Lernmodus -> LernmodusScreen(onBeenden = { screen = Screen.Start })
                is Screen.Pruefmodus -> PruefmodusScreen(
                    repository = remember(database) { RoomReviewCardRepository(database.reviewCardDao()) },
                    sessionRepository = remember(database) { RoomSessionRepository(database.sessionDao()) },
                    clock = clock,
                    onBeenden = { screen = Screen.Start },
                    onZumLernmodus = { screen = Screen.Lernmodus },
                )
                is Screen.Einstellungen -> EinstellungenScreen(
                    themeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        scope.launch { settingsRepository.save(AppSettings(newMode)) }
                    },
                    onBeenden = { screen = Screen.Start },
                )
                is Screen.Sitzungshistorie -> SitzungshistorieScreen(
                    repository = remember(database) { RoomSessionRepository(database.sessionDao()) },
                    onBeenden = { screen = Screen.Start },
                )
                is Screen.DevKanaltest -> DevKanaltestScreen(onBeenden = { screen = Screen.Start })
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
    onOpenDevKanaltest: () -> Unit,
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
        Button(onClick = onOpenDevKanaltest) {
            Text("Kanaltest (Dev)")
        }
        // Version pinned quietly at the bottom so a device test can always
        // tell which build is running (Autor-Wunsch 2026-07-13). Muted color,
        // never the accent -- it's reference info, not an active element.
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "v$VERSION_NAME",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DevKanaltestScreen(onBeenden: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = onBeenden) {
            Text("Beenden")
        }
        DevPlaybackScreen()
    }
}
