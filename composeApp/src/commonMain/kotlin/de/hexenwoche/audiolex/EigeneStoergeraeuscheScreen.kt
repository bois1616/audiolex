package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.audio.OwnNoise
import de.hexenwoche.audiolex.core.audio.OwnNoiseImportCheck
import de.hexenwoche.audiolex.core.audio.OwnNoiseSource
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.checkOwnNoiseImport
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.i18n.Strings
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import de.hexenwoche.audiolex.core.time.Clock
import kotlinx.coroutines.launch

/**
 * Own-noise management (Backlog M4 "Eigene Störgeräusche aufnehmen,
 * importieren und löschen", AC3/AC4): a dead end with "Zurück", same flat-
 * navigation pattern as [EigeneAufnahmenScreen], reachable from the noise
 * section of the settings. The "Neues Geräusch" mask reuses the own-corpus
 * recorder (extracted into `RecorderControls.kt` for exactly this reuse) and
 * runs aufnehmen → anhören → Label → speichern, order not enforced; the
 * "Import" section is the WAV picker's landing place, validated in `:core`
 * (AC4). "Meine Geräusche" below lists label, provenance and date with
 * "Anhören" and delete-with-confirmation per row.
 *
 * Deliberately missing, all Nicht-Ziele: no editing of label or sound, no
 * re-recording of an existing entry, no trimming, no duration limit. A sound
 * you want differently gets deleted and recorded again (Autor-Entscheid).
 */
@Composable
fun EigeneStoergeraeuscheScreen(
    repository: OwnNoiseRepository,
    clock: Clock,
    onBeenden: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val sink = remember { createAudioSink() }
    var status by remember { mutableStateOf<String?>(null) }
    val queue = remember(strings) {
        PlaybackQueue(sink, scope, onError = { e -> status = strings.playbackFailed(e.message) })
    }
    val permission = rememberRecordingPermissionState()
    val backup = rememberBackupActions()

    DisposableEffect(Unit) {
        onDispose {
            queue.stop()
            sink.close()
        }
    }

    var noises by remember { mutableStateOf<List<OwnNoise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    suspend fun reload() {
        noises = repository.all()
    }

    LaunchedEffect(Unit) {
        reload()
        isLoading = false
    }

    val recorder = rememberRecorderController()
    var newLabel by remember { mutableStateOf("") }

    // The label suggestion (AC3): filled the moment a take exists and the
    // field is still empty -- whatever the user typed first is kept. Date
    // and time make takes apart when several get recorded in one sitting.
    LaunchedEffect(recorder.buffer) {
        if (recorder.buffer != null && newLabel.isBlank()) {
            newLabel = strings.recordingLabelSuggestion(formatTimestamp(clock.nowEpochMillis(), clock.zoneId()))
        }
    }

    // The picked file waiting to be saved (AC4), bytes already validated in
    // `:core` -- anything that reaches this state is importable.
    var pendingImport by remember { mutableStateOf<PickedFile?>(null) }
    var importLabel by remember { mutableStateOf("") }

    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(strings.ownNoises, style = MaterialTheme.typography.headlineLarge)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            status?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(strings.sectionNewNoise, style = MaterialTheme.typography.titleMedium)

            // AC3's loop hint, decided against the mixer's behaviour:
            // mixWithNoise restarts the loop from sample 0 for every word,
            // so the *start* of the recording is what lands before each word
            // -- leading silence would be heard every time. Said up front,
            // before the first take, not after.
            Text(
                strings.noiseRecordingHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RecorderControls(recorder = recorder, permission = permission, queue = queue)

            OutlinedTextField(
                value = newLabel,
                onValueChange = { newLabel = it },
                label = { Text(strings.fieldLabel) },
                modifier = Modifier.fillMaxWidth(),
            )

            // AC3: a blank label is not allowed -- the list lives off its
            // labels, and an unnamed loop is unfindable in the settings'
            // scenario choice.
            Button(
                enabled = !recorder.isRecording && !recorder.isBusy && recorder.buffer != null && newLabel.isNotBlank(),
                onClick = {
                    val audio = recorder.buffer ?: return@Button
                    scope.launch {
                        repository.add(newLabel.trim(), WavFile.encode(audio), OwnNoiseSource.AUFNAHME)
                        newLabel = ""
                        recorder.reset()
                        status = null
                        reload()
                    }
                },
            ) {
                Text(strings.save)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(strings.sectionImport, style = MaterialTheme.typography.titleMedium)

            if (pendingImport == null) {
                Text(
                    strings.wavImportHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        backup.pickWav { picked ->
                            if (picked != null) {
                                status = importVerdictMessage(picked, strings, onValid = {
                                    pendingImport = it
                                    importLabel = labelSuggestionFor(it.displayName)
                                })
                            }
                        }
                    },
                ) {
                    Text(strings.importWavFile)
                }
            } else {
                // Validated import waiting for a label (AC4): anhören →
                // Label → speichern, same order-freedom as the mask above.
                pendingImport?.let { import ->
                    import.displayName?.let {
                        Text(
                            strings.fileChosen(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { queue.play { WavFile.decode(import.bytes) } }) {
                            Text(strings.listen)
                        }
                    }
                    OutlinedTextField(
                        value = importLabel,
                        onValueChange = { importLabel = it },
                        label = { Text(strings.fieldLabel) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = importLabel.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    repository.add(importLabel.trim(), import.bytes, OwnNoiseSource.IMPORT)
                                    pendingImport = null
                                    importLabel = ""
                                    status = null
                                    reload()
                                }
                            },
                        ) {
                            Text(strings.save)
                        }
                        TextButton(onClick = {
                            pendingImport = null
                            importLabel = ""
                        }) {
                            Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(strings.sectionMyNoises, style = MaterialTheme.typography.titleMedium)

            if (isLoading) {
                CircularProgressIndicator()
            } else if (noises.isEmpty()) {
                Text(strings.noOwnNoisesYet, style = MaterialTheme.typography.bodyLarge)
            } else {
                val sorted = noises.sortedByDescending { it.createdAtEpochMillis }
                sorted.forEachIndexed { index, noise ->
                    NoiseRow(
                        noise = noise,
                        strings = strings,
                        queue = queue,
                        repository = repository,
                        zoneId = clock.zoneId(),
                        onPlaybackMissing = { status = strings.noRecordingAvailable(noise.label) },
                        onDeleteRequested = { pendingDeleteId = noise.id },
                    )
                    if (index < sorted.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Button(onClick = onBeenden) {
            Text(strings.back)
        }
    }

    val deleteTarget = noises.firstOrNull { it.id == pendingDeleteId }
    if (deleteTarget != null) {
        // Same confirmation as the own corpus's single-entry delete: an
        // irreplaceable recording gets one explicit moment of "are you
        // sure", in the same words.
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(strings.deleteNoiseTitle) },
            text = { Text(strings.deleteConfirmBody(deleteTarget.label)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteTarget.id
                    pendingDeleteId = null
                    scope.launch {
                        repository.delete(id)
                        reload()
                        // The setting's scenario choice may now point at a
                        // deleted noise -- loadNoiseBuffer's existing
                        // fallback resolves it to the first remaining
                        // scenario (AC2, ADR-0010 point 4), nothing to do here.
                    }
                }) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun NoiseRow(
    noise: OwnNoise,
    strings: Strings,
    queue: PlaybackQueue,
    repository: OwnNoiseRepository,
    zoneId: String,
    onPlaybackMissing: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(noise.label, style = MaterialTheme.typography.bodyLarge)
        Text(
            "${strings.noiseSourceLabel(noise.source)} · ${formatTimestamp(noise.createdAtEpochMillis, zoneId)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // FlowRow, not a fixed Row -- the same overflow reasoning as the
        // own corpus's entry actions (A53-Befund 2026-08-06).
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilledTonalButton(onClick = {
                scope.launch {
                    val audio = repository.audioFor(noise)
                    if (audio == null) onPlaybackMissing() else queue.play(audio)
                }
            }) {
                Text(strings.listen)
            }
            TextButton(onClick = onDeleteRequested) {
                Text(strings.delete, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * Runs the `:core` validation over a picked file (AC4) and turns the verdict
 * into what the screen shows: a valid file hands over to [onValid] and
 * reports nothing; a rejected one gets a message that says what is wrong and
 * what to do (SOUL-Tonalität), never a bare error code and never a silent
 * conversion.
 */
private fun importVerdictMessage(picked: PickedFile, strings: Strings, onValid: (PickedFile) -> Unit): String? =
    when (val check = checkOwnNoiseImport(picked.bytes)) {
        OwnNoiseImportCheck.Ok -> {
            onValid(picked)
            null
        }

        OwnNoiseImportCheck.NotAWav -> strings.wavNotReadable

        is OwnNoiseImportCheck.WrongFormat -> strings.wavWrongFormat(check.sampleRate, check.channels)
    }

/** The file name without its `.wav` extension; a timestamp when no name came through. */
private fun labelSuggestionFor(displayName: String?): String {
    val name = displayName?.trim().orEmpty()
    if (name.isEmpty()) return ""
    return if (name.endsWith(".wav", ignoreCase = true)) name.dropLast(4).trim() else name
}
