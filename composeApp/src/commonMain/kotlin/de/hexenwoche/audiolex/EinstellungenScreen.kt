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
import de.hexenwoche.audiolex.core.i18n.Strings
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
 * on -- an SNR slider and a scenario choice drawn from the catalog of the
 * user's own noises (Backlog M4 "Eigene Störgeräusche", AC2; nothing is
 * bundled any more since ADR-0014, so an empty catalog gets a line saying
 * so instead of an empty radio group); under the scenario choice a
 * TextButton opens the own-noise management screen (AC3). The
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
    val strings = LocalStrings.current

    // Loaded once when the screen is entered, just for the scenario labels --
    // the training screens separately (re-)load the actual WAV for mixing.
    // The merged catalog (bundled + own noises, Backlog M4 "Eigene
    // Störgeräusche", AC2): what can be chosen here and what the training
    // screens can play is the same list.
    var scenarios by remember { mutableStateOf<List<NoiseScenario>>(emptyList()) }
    LaunchedEffect(Unit) {
        val loaded = loadAllNoiseScenarios(ownNoiseRepository)
        scenarios = loaded
        // A saved scenario that no longer exists (an own noise deleted since)
        // left the radio group without any selected option while playback
        // silently resolved to the first catalog entry -- display and
        // behaviour diverging, and no toggle healed it (A53-Abnahme v0.31.0,
        // Autor-Entscheid 2026-08-07: beim Öffnen heilen). Adopt the first
        // available scenario and persist that choice; an empty catalog has
        // nothing to heal to -- the ADR-0010 clean-speech fallback covers
        // playback there.
        if (loaded.isNotEmpty() && loaded.none { it.id == noiseScenario }) {
            onNoiseScenarioChange(loaded.first().id)
        }
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
        Text(strings.settings, style = MaterialTheme.typography.headlineLarge)

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
            Text(strings.sectionTrainingLevel, style = MaterialTheme.typography.titleMedium)

            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (profile in SettingsProfile.entries) {
                    RadioOption(
                        label = strings.profileLabel(profile),
                        selected = activeProfile == profile,
                        onSelect = { onProfileChange(profile) },
                    )
                }
            }

            if (activeProfile == null) {
                Text(
                    strings.customLevel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(strings.sectionAppearance, style = MaterialTheme.typography.titleMedium)

            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (mode in ThemeMode.entries) {
                    RadioOption(
                        label = strings.themeModeLabel(mode),
                        selected = themeMode == mode,
                        onSelect = { onThemeModeChange(mode) },
                    )
                }
            }

            Text(strings.sectionTrainingContent, style = MaterialTheme.typography.titleMedium)

            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (mode in CorpusMode.entries) {
                    RadioOption(
                        label = strings.corpusModeLabel(mode),
                        selected = corpusMode == mode,
                        onSelect = { onCorpusModeChange(mode) },
                    )
                }
            }

            Text(strings.sectionCorpus, style = MaterialTheme.typography.titleMedium)

            if (contingents.isEmpty()) {
                // Only reachable if the built-in corpus itself somehow
                // shipped without a single playable entry -- `thorsten`
                // always contributes at least one, so this is a defensive
                // fallback, not an expected state.
                Text(
                    strings.noContingentAvailable,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val allSpeakers = contingents.map { it.speaker }.toSet()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onExcludedSpeakersChange(excludedSpeakers - allSpeakers) }) {
                        Text(strings.selectAll)
                    }
                    TextButton(onClick = { onExcludedSpeakersChange(excludedSpeakers + allSpeakers) }) {
                        Text(strings.deselectAll)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    for (contingent in contingents) {
                        ContingentOption(
                            contingent = contingent,
                            strings = strings,
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

            Text(strings.sectionNoise, style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Switch(checked = noiseEnabled, onCheckedChange = onNoiseEnabledChange)
                Text(strings.onOff, style = MaterialTheme.typography.bodyLarge)
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
                Text(strings.snrLabel(draggedSnrDb), style = MaterialTheme.typography.bodyLarge)

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
                        strings.quieter,
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
                        strings.louder,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(strings.sectionScenario, style = MaterialTheme.typography.titleMedium)

                if (scenarios.isEmpty()) {
                    // The published normal case, not an edge case (ADR-0014):
                    // nothing is bundled any more, so until the user records
                    // or imports a sound there is nothing to choose. An empty
                    // radio group under a "Szenario" heading would look
                    // broken; this says what is missing and the TextButton
                    // right below is the way there.
                    Text(
                        strings.noNoiseYet,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
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

                // AC3: the way into the own-noise screen -- a quiet
                // TextButton directly under the scenario choice, the place
                // where a missing sound is noticed. Own noises show up in
                // the radio list above like bundled ones (AC2).
                TextButton(onClick = onOpenEigeneStoergeraeusche) {
                    Text(strings.ownNoises)
                }
            }

            Text(strings.sectionOutput, style = MaterialTheme.typography.titleMedium)

            // Read-only, no control -- AC4 of Batch A. Recedes in
            // onSurfaceVariant (DESIGN.md "Sekundäres tritt zurück"); its
            // sole purpose is making a wrong detection noticeable (ADR-0011),
            // not offering anything to tap. Live-updates via
            // rememberOutputSetup()'s own device-callback registration
            // (ADR-0011 point 4) -- no extra state needed here.
            val outputSetup = rememberOutputSetup()
            Text(
                strings.outputSetupDetected(outputSetup),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(strings.sectionChannels, style = MaterialTheme.typography.titleMedium)

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
                for (mode in ChannelMode.entries) {
                    RadioOption(
                        label = strings.channelModeLabel(mode),
                        selected = channelMode == mode,
                        enabled = channelSelectionEnabled,
                        onSelect = { onChannelModeChange(mode) },
                    )
                }
            }

            if (!channelSelectionEnabled) {
                // Sachliche Begründung, keine Fehlermeldung (SOUL.md
                // Tonalität) -- erklärt die Physik (das Hörgerät summiert
                // Stereo zu Mono, ADR-0007/ADR-0011), nicht ein Problem.
                Text(
                    strings.channelsIneffectiveHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(onClick = onBeenden) {
            Text(strings.back)
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
private fun ContingentOption(
    contingent: SpeakerContingent,
    strings: Strings,
    included: Boolean,
    onIncludedChange: (Boolean) -> Unit,
) {
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
            Text(speakerLabel(contingent.speaker, strings), style = MaterialTheme.typography.bodyLarge)
            Text(
                "${strings.wordCount(contingent.wordCount)}, ${strings.sentenceCount(contingent.sentenceCount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * An unset speaker field (Batch B allows this) is its own visible contingent
 * (AC4), not a blank label. A real speaker name is never translated -- it is
 * data the user typed.
 */
private fun speakerLabel(speaker: String, strings: Strings): String = speaker.ifBlank { strings.noSpeaker }
