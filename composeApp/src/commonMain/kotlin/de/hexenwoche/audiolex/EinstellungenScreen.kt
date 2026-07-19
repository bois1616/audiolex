package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.audio.NoiseScenario
import de.hexenwoche.audiolex.core.settings.CorpusMode
import de.hexenwoche.audiolex.core.settings.SNR_DB_MAX
import de.hexenwoche.audiolex.core.settings.SNR_DB_MIN
import de.hexenwoche.audiolex.core.settings.ThemeMode
import kotlin.math.roundToInt

/**
 * Einstellungen (Backlog M4 "Settings-Persistenz-Fundament", erweitert in M2
 * "Satz-Bogen Batch B" um den Trainingsinhalt-Schalter, in M4
 * "Störgeräusch-Overlay" um SNR/Szenario, ADR-0010): the first setting was a
 * manual theme override -- the app previously followed
 * `isSystemInDarkTheme()` unconditionally with no way to confirm Dark Mode
 * actually engaged (Autor-Finding 2026-07-13, A53-Gerätetest). The second
 * setting picks the corpus entries the training screens work on (Wörter /
 * Sätze, ADR-0009 point 4 -- a plain setting, not a preset). The third is the
 * noise overlay shared by both training modes: a switch, and -- only while
 * on -- an SNR slider and a scenario choice loaded from `noise.json`.
 * Everything else (channel selection, presets) stays out of scope, own
 * backlog items. A dead end with "Zurück", same navigation pattern as the
 * other screens.
 *
 * Title and "Zurück" stay pinned; only the settings content between them
 * scrolls (same `Modifier.weight(1f).verticalScroll(rememberScrollState())`
 * pattern as `SitzungshistorieScreen`). With the noise section visible
 * (switch + slider + scenario radio group) the non-scrollable column used to
 * overflow the A53 screen height and push "Zurück" out of view -- a dead end
 * (A53-Befund 2026-07-19).
 */
@Composable
fun EinstellungenScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    corpusMode: CorpusMode,
    onCorpusModeChange: (CorpusMode) -> Unit,
    noiseEnabled: Boolean,
    onNoiseEnabledChange: (Boolean) -> Unit,
    snrDb: Int,
    onSnrDbChange: (Int) -> Unit,
    noiseScenario: String,
    onNoiseScenarioChange: (String) -> Unit,
    onBeenden: () -> Unit,
) {
    // Loaded once when the screen is entered, just for the scenario labels --
    // the training screens separately (re-)load the actual WAV for mixing.
    var scenarios by remember { mutableStateOf<List<NoiseScenario>>(emptyList()) }
    LaunchedEffect(Unit) {
        scenarios = loadNoiseScenarios()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineLarge)

        // Only this middle section scrolls -- title above and "Zurück" below
        // stay pinned, so the noise section (switch + slider + scenario
        // radios) can never push "Zurück" out of view again.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Erscheinungsbild", style = MaterialTheme.typography.titleMedium)

            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                RadioOption(
                    label = "System",
                    selected = themeMode == ThemeMode.SYSTEM,
                    onSelect = { onThemeModeChange(ThemeMode.SYSTEM) },
                )
                RadioOption(
                    label = "Hell",
                    selected = themeMode == ThemeMode.LIGHT,
                    onSelect = { onThemeModeChange(ThemeMode.LIGHT) },
                )
                RadioOption(
                    label = "Dunkel",
                    selected = themeMode == ThemeMode.DARK,
                    onSelect = { onThemeModeChange(ThemeMode.DARK) },
                )
            }

            Text("Trainingsinhalt", style = MaterialTheme.typography.titleMedium)

            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                RadioOption(
                    label = "Wörter",
                    selected = corpusMode == CorpusMode.WOERTER,
                    onSelect = { onCorpusModeChange(CorpusMode.WOERTER) },
                )
                RadioOption(
                    label = "Sätze",
                    selected = corpusMode == CorpusMode.SAETZE,
                    onSelect = { onCorpusModeChange(CorpusMode.SAETZE) },
                )
            }

            Text("Störgeräusch", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Switch(checked = noiseEnabled, onCheckedChange = onNoiseEnabledChange)
                Text("Ein/Aus", style = MaterialTheme.typography.bodyLarge)
            }

            if (noiseEnabled) {
                val snrLabel = if (snrDb >= 0) "SNR: +$snrDb dB" else "SNR: $snrDb dB"
                Text(snrLabel, style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = snrDb.toFloat(),
                    onValueChange = { onSnrDbChange(it.roundToInt()) },
                    valueRange = SNR_DB_MIN.toFloat()..SNR_DB_MAX.toFloat(),
                    // 26 integer values (-5..20 inclusive); `steps` excludes the
                    // two endpoints, so 26 - 2 = 24.
                    steps = SNR_DB_MAX - SNR_DB_MIN - 1,
                )

                Text("Szenario", style = MaterialTheme.typography.titleMedium)

                Column(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    for (scenario in scenarios) {
                        RadioOption(
                            label = scenario.label,
                            selected = noiseScenario == scenario.id,
                            onSelect = { onNoiseScenarioChange(scenario.id) },
                        )
                    }
                }
            }
        }

        Button(onClick = onBeenden) {
            Text("Zurück")
        }
    }
}

// The row itself carries the selectable semantics/click target (Material
// guidance), not just the RadioButton -- tapping the label also switches the
// mode. RadioButton's own onClick stays null so there's exactly one click
// handler, not two racing ones.
@Composable
private fun RadioOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
