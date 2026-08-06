package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Impressum + Datenschutz (Backlog M2 "Impressum-/Datenschutz-Seite",
 * Autor-Requirement 2026-07-29). A dead end with "Zurück", entered from a
 * quiet TextButton next to the version line on the StartScreen -- same
 * navigation pattern as the other screens.
 *
 * Title and "Zurück" stay pinned; only the prose between them scrolls (same
 * `Modifier.weight(1f).verticalScroll(rememberScrollState())` pattern as
 * `SitzungshistorieScreen`/`EinstellungenScreen`) -- both of those screens
 * hit an unreachable "Zurück" before this pattern was in place, so this one
 * starts with it instead of risking the same bug a third time.
 *
 * The Datenschutz claim (no network access) is enforceable, not just
 * declared: `AndroidManifest.xml` has no `uses-permission` entry at all, not
 * even INTERNET, so the app cannot open a network connection -- verified
 * against the manifest before writing this text.
 */
@Composable
fun ImpressumScreen(onBeenden: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Impressum & Datenschutz", style = MaterialTheme.typography.headlineLarge)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Impressum", style = MaterialTheme.typography.titleMedium)
            Text(
                "Verantwortlich für diese App:\n\nStephan Reindl\nTelefon: [Nummer entfernt]\nE-Mail: audiolex26@proton.me",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Privates, nicht-kommerzielles Projekt ohne öffentlichen Vertrieb.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "AudioLex ist ein privates Übungswerkzeug, kein professionelles oder " +
                    "medizinisches Produkt. Es ersetzt keine Beratung beim " +
                    "Hörgeräteakustiker und keine Abklärung in der HNO-Heilkunde.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Text("Datenschutz", style = MaterialTheme.typography.titleMedium)
            Text(
                "AudioLex hat keine Tracker, keine Analyse-Dienste und keine Werbung.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Wortschatz, Bewertungen und Sitzungsverlauf liegen ausschließlich lokal " +
                    "auf diesem Gerät. Es gibt keine Cloud, kein Konto, keine Übertragung ins " +
                    "Internet.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Die App fordert keine einzige Android-Berechtigung an — auch nicht die " +
                    "Internet-Berechtigung. Ohne sie kann sie technisch nicht mit dem Netz " +
                    "kommunizieren.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Button(onClick = onBeenden) {
            Text("Zurück")
        }
    }
}
