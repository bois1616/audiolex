package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.persistence.AudioLexDatabase
import de.hexenwoche.audiolex.core.persistence.RoomReviewCardRepository

/**
 * Flat navigation (DESIGN.md "Screenstruktur"): Start is the hub, training
 * screens are dead ends that return to Start via "Beenden". No navigation
 * library -- at most two levels, a simple state switch is enough.
 */
private sealed interface Screen {
    data object Start : Screen
    data object Lernmodus : Screen
    data object Pruefmodus : Screen
    data object DevKanaltest : Screen
}

// Entry point and navigation host (Start/Lernmodus/Prüfmodus, see DESIGN.md).
// [database] is created once per process by the platform entry point
// (MainActivity/main) and handed down, so the SQLite file isn't reopened
// on every screen visit.
@Composable
fun App(database: AudioLexDatabase) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var screen by remember { mutableStateOf<Screen>(Screen.Start) }

            when (val current = screen) {
                is Screen.Start -> StartScreen(
                    onStartLernmodus = { screen = Screen.Lernmodus },
                    onStartPruefmodus = { screen = Screen.Pruefmodus },
                    onOpenDevKanaltest = { screen = Screen.DevKanaltest },
                )
                is Screen.Lernmodus -> LernmodusScreen(onBeenden = { screen = Screen.Start })
                is Screen.Pruefmodus -> PruefmodusScreen(
                    repository = remember(database) { RoomReviewCardRepository(database.reviewCardDao()) },
                    onBeenden = { screen = Screen.Start },
                    onZumLernmodus = { screen = Screen.Lernmodus },
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
        Button(onClick = onOpenDevKanaltest) {
            Text("Kanaltest (Dev)")
        }
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
