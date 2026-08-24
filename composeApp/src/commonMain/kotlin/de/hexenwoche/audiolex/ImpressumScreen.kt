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
 * The Datenschutz claim "no network access" is enforceable, not just
 * declared: `AndroidManifest.xml` has no `INTERNET` `uses-permission` entry,
 * so the app cannot open a network connection -- verified against the
 * manifest before writing this text. That stays true as of Backlog
 * Eigen-Korpus Batch A (ADR-0012), which adds the app's first-ever
 * permission (`RECORD_AUDIO`, also in the manifest) -- the "no permissions
 * at all" claim below is retired accordingly, the "no network" one isn't.
 *
 * Since ADR-0015 this text exists in two languages and follows the picker on
 * the StartScreen. Both versions say the same thing -- the privacy claims in
 * particular are statements about the manifest, so a translation that softened
 * one of them would make it false rather than merely different.
 *
 * ADR-0013 forced a correction here rather than an addition. The earlier
 * wording -- "Aufnahmen können das Gerät technisch nicht verlassen" -- was
 * false while `allowBackup="true"` stood in the manifest: Android's
 * Auto-Backup was free to upload app-private files to a Google account, and
 * it needs no INTERNET permission of ours to do it. The claim is now scoped
 * to what it can actually cover ("die App kann von sich aus nichts
 * übertragen") and the manifest was changed in the same version to make even
 * that true. The two backup paragraphs below are the honest remainder: an
 * export exists, the user triggers it, and whoever never uses it has no copy
 * at all now that the system backup is gone (Backlog AC5/AC6).
 */
@Composable
fun ImpressumScreen(onBeenden: () -> Unit) {
    val strings = LocalStrings.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(strings.imprintAndPrivacy, style = MaterialTheme.typography.headlineLarge)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.imprintHeading, style = MaterialTheme.typography.titleMedium)
            Paragraph(strings.imprintResponsible)
            Paragraph(strings.imprintNonCommercial)
            Paragraph(strings.imprintNotMedical)
            Paragraph(strings.imprintVibeCoded)
            Paragraph(strings.imprintAsIs)

            Text(strings.privacyHeading, style = MaterialTheme.typography.titleMedium)
            Paragraph(strings.privacyNoTrackers)
            Paragraph(strings.privacyLocalOnly)
            Paragraph(strings.privacyOnePermission)
            Paragraph(strings.privacyBackupIsYours)
            Paragraph(strings.privacyNoSystemBackup)
        }

        Button(onClick = onBeenden) {
            Text(strings.back)
        }
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}
