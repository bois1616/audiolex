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
 * Kurzanleitung (Autor-Requirement 2026-08-24, ADR-0015): what the two
 * training modes are for, what the five ratings mean, and which setting does
 * what -- the things the screens themselves cannot say without cluttering
 * the training flow. Reached from a quiet TextButton on the StartScreen,
 * next to Impressum, and a dead end with "Zurück" like every other secondary
 * screen.
 *
 * It follows the chosen UI language, which is the whole reason it exists as
 * a screen rather than a link to the repository README: the README is German
 * only and is written for someone building the app, not using it.
 *
 * Title and "Zurück" stay pinned, only the prose scrolls -- the same
 * `Modifier.weight(1f).verticalScroll(...)` pattern as `ImpressumScreen`.
 * This screen is longer than that one, so getting it wrong would have been
 * the third unreachable "Zurück" in this project's history.
 *
 * The text lives in the string catalogs (`:core`, `i18n`) like every other
 * label. It is longer than a label, but a second mechanism just for prose
 * would mean a second place a translation can go missing.
 */
@Composable
fun KurzanleitungScreen(onBeenden: () -> Unit) {
    val strings = LocalStrings.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(strings.guideTitle, style = MaterialTheme.typography.headlineLarge)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.guideIntro, style = MaterialTheme.typography.bodyLarge)

            Section(strings.guideLearningHeading, strings.guideLearningBody)

            Section(strings.guideExamHeading, strings.guideExamBody)
            Text(strings.guideRatingBody, style = MaterialTheme.typography.bodyLarge)

            Section(strings.guideSettingsHeading, strings.guideSettingsLevel)
            Text(strings.guideSettingsContent, style = MaterialTheme.typography.bodyLarge)
            Text(strings.guideSettingsNoise, style = MaterialTheme.typography.bodyLarge)
            Text(strings.guideSettingsOutput, style = MaterialTheme.typography.bodyLarge)

            Section(strings.guideOwnRecordingsHeading, strings.guideOwnRecordingsBody)

            Section(strings.guideBackupHeading, strings.guideBackupBody)
        }

        Button(onClick = onBeenden) {
            Text(strings.back)
        }
    }
}

/** Heading plus its first paragraph -- the shape this screen repeats six times. */
@Composable
private fun Section(heading: String, body: String) {
    Text(heading, style = MaterialTheme.typography.titleMedium)
    Text(body, style = MaterialTheme.typography.bodyLarge)
}
