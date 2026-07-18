package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.settings.ThemeMode

/**
 * Einstellungen (Backlog M4 "Settings-Persistenz-Fundament"): the first real
 * setting, a manual theme override -- the app previously followed
 * `isSystemInDarkTheme()` unconditionally with no way to confirm Dark Mode
 * actually engaged (Autor-Finding 2026-07-13, A53-Gerätetest). Everything
 * else (channel selection, presets, noise settings) stays out of scope, own
 * backlog items. A dead end with "Zurück", same navigation pattern as the
 * other screens.
 */
@Composable
fun EinstellungenScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBeenden: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineLarge)
        Text("Erscheinungsbild", style = MaterialTheme.typography.titleMedium)

        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ThemeModeOption(
                label = "System",
                selected = themeMode == ThemeMode.SYSTEM,
                onSelect = { onThemeModeChange(ThemeMode.SYSTEM) },
            )
            ThemeModeOption(
                label = "Hell",
                selected = themeMode == ThemeMode.LIGHT,
                onSelect = { onThemeModeChange(ThemeMode.LIGHT) },
            )
            ThemeModeOption(
                label = "Dunkel",
                selected = themeMode == ThemeMode.DARK,
                onSelect = { onThemeModeChange(ThemeMode.DARK) },
            )
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
private fun ThemeModeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
