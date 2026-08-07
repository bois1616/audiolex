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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import de.hexenwoche.audiolex.core.audio.OutputSetup
import de.hexenwoche.audiolex.core.corpus.SpeakerContingent
import de.hexenwoche.audiolex.core.corpus.speakerContingents
import de.hexenwoche.audiolex.core.settings.ChannelMode
import de.hexenwoche.audiolex.core.settings.CorpusMode
import de.hexenwoche.audiolex.core.settings.SNR_DB_MAX
import de.hexenwoche.audiolex.core.settings.SNR_DB_MIN
import de.hexenwoche.audiolex.core.settings.SettingsProfile
import de.hexenwoche.audiolex.core.settings.ThemeMode
import kotlin.math.roundToInt

/**
 * Einstellungen (Backlog M4 "Settings-Persistenz-Fundament", erweitert in M2
 * "Satz-Bogen Batch B" um den Trainingsinhalt-Schalter, in M4
 * "Störgeräusch-Overlay" um SNR/Szenario, ADR-0010, in M4 "Szenario-Presets"
 * um die Trainingsstufe): the first section is the one-tap training level
 * (Backlog M4 "Szenario-Presets Einfach/Schwierig/Fortgeschritten", S9) --
 * tapping a level applies it over the regular settings path, writing only
 * the atomic noise pair. The level itself is never persisted: which option
 * is active is re-derived from the atomic values (`derivedProfile` in
 * `:core`), so manually adjusting the noise switch or the SNR slider simply
 * moves or drops the selection, and when no level matches, no option is
 * active and the "Individuell eingestellt" line says so. The first setting
 * historically was a manual theme override -- the app previously followed
 * `isSystemInDarkTheme()` unconditionally with no way to confirm Dark Mode
 * actually engaged (Autor-Finding 2026-07-13, A53-Gerätetest). The second
 * setting picks the corpus entries the training screens work on (Wörter /
 * Sätze, ADR-0009 point 4 -- a plain setting, not a preset). The third is the
 * noise overlay shared by both training modes: a switch, and -- only while
 * on -- an SNR slider and a scenario choice drawn from the merged catalog
 * (bundled `noise.json` plus the user's own noises, Backlog M4 "Eigene
 * Störgeräusche", AC2); under the scenario choice a TextButton opens the
 * own-noise management screen (AC3). The
 * fourth is the "Ausgabe" section (Backlog M4 "Kopfhörer-Bogen Batch A",
 * ADR-0011): which [OutputSetup] is currently detected, so a wrong detection
 * is at least visible to the user. The fifth is "Kanäle" (Backlog M4
 * "Kopfhörer-Bogen Batch B", ADR-0011 point 5), directly below it: a radio
 * group for [ChannelMode], `enabled = false` with an explanatory line
 * whenever the detected setup is [OutputSetup.HOERGERAET] -- a channel
 * choice would be wirkungslos there (ADR-0007), and the disabled state says
 * so instead of just hiding the control. The sixth, "Korpus" (Backlog
 * Eigen-Korpus Batch D, ADR-0012
 * Nachtrag "Die Quellentrennung wird durch Kontingente ersetzt"), sits
 * directly below "Trainingsinhalt" -- both settings answer "what does the
 * training screen even draw from", the checkbox list picking *which*
 * speaker contingents (the mitgelieferte Stimme `thorsten` is just one of
 * them, no special status), [corpusMode] picking Wörter/Sätze within that.
 * This replaces Batch C's `CorpusSource` radio group ("Quelle") outright,
 * not alongside it -- see the ADR Nachtrag for why a mitgeliefert/eigene/
 * beide choice was really just a two-bucket special case of this. A dead
 * end with "Zurück", same navigation pattern as the other screens.
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
    activeProfile: SettingsProfile?,
    onProfileChange: (SettingsProfile) -> Unit,
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
    channelMode: ChannelMode,
    onChannelModeChange: (ChannelMode) -> Unit,
    excludedSpeakers: Set<String>,
    onExcludedSpeakersChange: (Set<String>) -> Unit,
    ownCorpusRepository: OwnCorpusRepository,
    ownNoiseRepository: OwnNoiseRepository,
    onOpenEigeneStoergeraeusche: () -> Unit,
    onBeenden: () -> Unit,
) {
    // Loaded once when the screen is entered, just for the scenario labels --
    // the training screens separately (re-)load the actual WAV for mixing.
    // The merged catalog (bundled + own noises, Backlog M4 "Eigene
    // Störgeräusche", AC2): what can be chosen here and what the training
    // screens can play is the same list.
    var scenarios by remember { mutableStateOf<List<NoiseScenario>>(emptyList()) }
    LaunchedEffect(Unit) {
        scenarios = loadAllNoiseScenarios(ownNoiseRepository)
    }

    // The Kontingent-Liste (Backlog Eigen-Korpus Batch D, AC4): loaded fully
    // unfiltered (no `kind`, no `excludedSpeakers`) so both word/sentence
    // counts stay visible and stable no matter what's currently
    // selected/excluded -- the list itself must not wobble while the user
    // (de)selects contingents.
    var contingents by remember { mutableStateOf<List<SpeakerContingent>>(emptyList()) }
    LaunchedEffect(Unit) {
        contingents = loadCorpus(ownCorpusRepository = ownCorpusRepository).speakerContingents()
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
            // One-tap training level (Backlog M4 "Szenario-Presets", AC3),
            // the first section. Which option is active is the *derived*
            // level (`activeProfile` from the atomic noise pair) -- nothing
            // about the level itself is stored, so a manual change of the
            // noise switch or the SNR slider simply moves or drops the
            // selection. When no level matches (`null`), no option is active
            // and the "Individuell eingestellt" line says so -- deliberately
            // not a tappable option of its own (Nicht-Ziel), just the same
            // reserved styling as the "leiser/lauter" orientation on the SNR
            // slider.
            Text("Trainingsstufe", style = MaterialTheme.typography.titleMedium)

            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                RadioOption(
                    label = "Einfach",
                    selected = activeProfile == SettingsProfile.EINFACH,
                    onSelect = { onProfileChange(SettingsProfile.EINFACH) },
                )
                RadioOption(
                    label = "Schwierig",
                    selected = activeProfile == SettingsProfile.SCHWIERIG,
                    onSelect = { onProfileChange(SettingsProfile.SCHWIERIG) },
                )
                RadioOption(
                    label = "Fortgeschritten",
                    selected = activeProfile == SettingsProfile.FORTGESCHRITTEN,
                    onSelect = { onProfileChange(SettingsProfile.FORTGESCHRITTEN) },
                )
            }

            if (activeProfile == null) {
                Text(
                    "Individuell eingestellt",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

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

            Text("Korpus", style = MaterialTheme.typography.titleMedium)

            if (contingents.isEmpty()) {
                // Only reachable if the built-in corpus itself somehow
                // shipped without a single playable entry -- `thorsten`
                // always contributes at least one, so this is a defensive
                // fallback, not an expected state.
                Text(
                    "Kein Kontingent verfügbar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val allSpeakers = contingents.map { it.speaker }.toSet()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onExcludedSpeakersChange(excludedSpeakers - allSpeakers) }) {
                        Text("Alle auswählen")
                    }
                    TextButton(onClick = { onExcludedSpeakersChange(excludedSpeakers + allSpeakers) }) {
                        Text("Alle abwählen")
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    for (contingent in contingents) {
                        ContingentOption(
                            contingent = contingent,
                            included = contingent.speaker !in excludedSpeakers,
                            onIncludedChange = { included ->
                                // AC2, bindend: the *exclusion* is what's
                                // persisted, not the selection -- unchecking
                                // adds the name, checking removes it. Ghost
                                // names (excluded once, not in `contingents`
                                // anymore) are never touched here, since this
                                // callback only ever names a speaker that's
                                // currently visible (AC5).
                                onExcludedSpeakersChange(
                                    if (included) excludedSpeakers - contingent.speaker else excludedSpeakers + contingent.speaker,
                                )
                            },
                        )
                    }
                }
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
                // The slider drags on a local value and commits once on
                // release (`onValueChangeFinished`) -- persisting every tick
                // meant dozens of Room writes per drag gesture (Backlog
                // "Code-Qualität"). `remember(snrDb)` re-syncs the local
                // value whenever the persisted one changes from outside the
                // drag (initial load, next screen visit). Persistence-on-
                // release is unchanged by the axis flip below (Backlog M4
                // AC3) -- only the mapping from slider position to dB value
                // changed, not when it's saved.
                var draggedSnrDb by remember(snrDb) { mutableStateOf(snrDb) }
                val snrLabel = if (draggedSnrDb >= 0) "SNR: +$draggedSnrDb dB" else "SNR: $draggedSnrDb dB"
                Text(snrLabel, style = MaterialTheme.typography.bodyLarge)

                // Axis mirrored against the raw SNR value (A53-Befund +
                // Autor-Entscheid 2026-08-06): a higher SNR means *less*
                // noise, but the author expects "further right" to mean
                // "more noise". The slider's own position still runs
                // SNR_DB_MIN..SNR_DB_MAX left-to-right like any Slider, so
                // dragging is mirrored both ways -- `sliderPositionFor`
                // reflects a dB value onto its on-screen position, and
                // applying it again (position -> dB) undoes it, since the
                // mapping is its own inverse. AppSettings.snrDb / the mixer
                // never see the mirrored position, only the real dB value.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "leiser",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = sliderPositionFor(draggedSnrDb).toFloat(),
                        onValueChange = { draggedSnrDb = sliderPositionFor(it.roundToInt()) },
                        onValueChangeFinished = { onSnrDbChange(draggedSnrDb) },
                        valueRange = SNR_DB_MIN.toFloat()..SNR_DB_MAX.toFloat(),
                        // 26 integer values (-5..20 inclusive); `steps` excludes
                        // the two endpoints, so 26 - 2 = 24.
                        steps = SNR_DB_MAX - SNR_DB_MIN - 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "lauter",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

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

                // AC3: the way into the own-noise screen -- a quiet
                // TextButton directly under the scenario choice, the place
                // where a missing sound is noticed. Own noises show up in
                // the radio list above like bundled ones (AC2).
                TextButton(onClick = onOpenEigeneStoergeraeusche) {
                    Text("Eigene Störgeräusche")
                }
            }

            Text("Ausgabe", style = MaterialTheme.typography.titleMedium)

            // Read-only, no control -- AC4 of Batch A. Recedes in
            // onSurfaceVariant (DESIGN.md "Sekundäres tritt zurück"); its
            // sole purpose is making a wrong detection noticeable (ADR-0011),
            // not offering anything to tap. Live-updates via
            // rememberOutputSetup()'s own device-callback registration
            // (ADR-0011 point 4) -- no extra state needed here.
            val outputSetup = rememberOutputSetup()
            val outputSetupLabel = when (outputSetup) {
                OutputSetup.STEREO_KOPFHOERER -> "Stereo-Kopfhörer"
                OutputSetup.HOERGERAET -> "Hörgerät"
            }
            Text(
                "$outputSetupLabel erkannt",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Kanäle", style = MaterialTheme.typography.titleMedium)

            // Wirksam nur im Kopfhörer-Setup (Backlog M4 "Kopfhörer-Bogen
            // Batch B" AC2/AC3, ADR-0011 point 5): the radio group itself
            // stays visible either way -- disabling it, rather than hiding
            // it, is what makes the connection to the "Ausgabe" line above
            // legible instead of the option just quietly not being there.
            val channelSelectionEnabled = outputSetup == OutputSetup.STEREO_KOPFHOERER
            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                RadioOption(
                    label = "Beide",
                    selected = channelMode == ChannelMode.BEIDE,
                    enabled = channelSelectionEnabled,
                    onSelect = { onChannelModeChange(ChannelMode.BEIDE) },
                )
                RadioOption(
                    label = "Nur links",
                    selected = channelMode == ChannelMode.NUR_LINKS,
                    enabled = channelSelectionEnabled,
                    onSelect = { onChannelModeChange(ChannelMode.NUR_LINKS) },
                )
                RadioOption(
                    label = "Nur rechts",
                    selected = channelMode == ChannelMode.NUR_RECHTS,
                    enabled = channelSelectionEnabled,
                    onSelect = { onChannelModeChange(ChannelMode.NUR_RECHTS) },
                )
            }

            if (!channelSelectionEnabled) {
                // Sachliche Begründung, keine Fehlermeldung (SOUL.md
                // Tonalität) -- erklärt die Physik (das Hörgerät summiert
                // Stereo zu Mono, ADR-0007/ADR-0011), nicht ein Problem.
                Text(
                    "Ohne Wirkung am Hörgerät: Es summiert Stereo automatisch zu Mono. " +
                        "Verfügbar mit Stereo-Kopfhörern.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(onClick = onBeenden) {
            Text("Zurück")
        }
    }
}

/**
 * Mirrors a dB value onto its on-screen slider position, and back again --
 * the mapping is its own inverse, so this one function handles both
 * directions: dB value -> slider position for [Slider]'s `value`, and raw
 * slider position -> dB value inside `onValueChange` (Backlog M4
 * "Störgeräusch-Regler: Achse umdrehen", A53-Befund + Autor-Entscheid
 * 2026-08-06).
 */
private fun sliderPositionFor(snrDb: Int): Int = SNR_DB_MIN + SNR_DB_MAX - snrDb

// The row itself carries the selectable semantics/click target (Material
// guidance), not just the RadioButton -- tapping the label also switches the
// mode. RadioButton's own onClick stays null so there's exactly one click
// handler, not two racing ones. [enabled] (Backlog M4 "Kopfhörer-Bogen Batch
// B" AC2): sichtbar-inaktiv, not hidden -- Material's own disabled styling on
// RadioButton/selectable already dims and blocks taps, no extra state needed.
@Composable
private fun RadioOption(label: String, selected: Boolean, enabled: Boolean = true, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, enabled = enabled, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

// Same selectable-row/single-click-handler pattern as RadioOption above,
// Checkbox instead of RadioButton since any number of contingents can be
// included at once (Backlog Eigen-Korpus Batch D, AC5). The index line
// (word/sentence counts) is secondary text under the label, same visual
// weight as the "Ohne Wirkung am Hörgerät" explanations elsewhere on this
// screen (DESIGN.md "Sekundäres tritt zurück").
@Composable
private fun ContingentOption(contingent: SpeakerContingent, included: Boolean, onIncludedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = included, onClick = { onIncludedChange(!included) }, role = Role.Checkbox)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = included, onCheckedChange = null)
        Column {
            Text(speakerLabel(contingent.speaker), style = MaterialTheme.typography.bodyLarge)
            Text(
                "${wordCountLabel(contingent.wordCount)}, ${sentenceCountLabel(contingent.sentenceCount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** An unset speaker field (Batch B allows this) is its own visible contingent (AC4), not a blank label. */
private fun speakerLabel(speaker: String): String = speaker.ifBlank { "Ohne Sprecher" }

private fun wordCountLabel(count: Int): String = if (count == 1) "1 Wort" else "$count Wörter"

private fun sentenceCountLabel(count: Int): String = if (count == 1) "1 Satz" else "$count Sätze"
