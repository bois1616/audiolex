package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.audio.AudioSource
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import de.hexenwoche.audiolex.core.audio.RECORDING_CHANNELS
import de.hexenwoche.audiolex.core.audio.RECORDING_SAMPLE_RATE
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.audio.createAudioSource
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.corpus.OwnEntry
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import de.hexenwoche.audiolex.core.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Own-corpus management (Backlog Eigen-Korpus Batch B, ADR-0012 point 5):
 * the "Neue Aufnahme" section is the Aufnahmemaske (AC3) -- aufnehmen,
 * anhören, Text zuordnen, speichern, order not enforced -- and "Meine
 * Einträge" below it is the management list (AC5) with the three
 * correction paths the ADR calls the fachlicher Kern of this batch: text
 * without re-recording, re-recording without losing the text, and delete.
 * One screen rather than two (a dead end reachable from the StartScreen,
 * same flat-navigation pattern as every other screen) -- the two parts
 * never need to be visited independently.
 *
 * Recording reimplements the record-and-merge-chunks flow from
 * [DevPlaybackScreen]'s Mikrofon-Rohtest rather than sharing it: that
 * screen is explicitly frozen for this batch. [RecorderController] is this
 * file's own reusable piece, used both for the "Neue Aufnahme" section and,
 * inline, for re-recording an existing entry.
 *
 * Title and "Zurück" stay pinned; only the content between them scrolls
 * (same `Modifier.weight(1f).verticalScroll(...)` pattern as
 * `EinstellungenScreen`/`SitzungshistorieScreen` -- an unreachable "Zurück"
 * has been a real bug in this project twice before).
 */
@Composable
fun EigeneAufnahmenScreen(repository: OwnCorpusRepository, clock: Clock, onBeenden: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sink = remember { createAudioSink() }
    var status by remember { mutableStateOf<String?>(null) }
    val queue = remember {
        PlaybackQueue(sink, scope, onError = { e -> status = "Wiedergabe fehlgeschlagen: ${e.message}" })
    }
    val permission = rememberRecordingPermissionState()

    DisposableEffect(Unit) {
        onDispose {
            queue.stop()
            sink.close()
        }
    }

    var entries by remember { mutableStateOf<List<OwnEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    suspend fun reload() {
        entries = repository.all()
    }

    var newText by remember { mutableStateOf("") }
    var newSpeaker by remember { mutableStateOf("") }
    var kindOverride by remember { mutableStateOf<EntryKind?>(null) }

    LaunchedEffect(Unit) {
        reload()
        isLoading = false
        // AC4b: prefilled with the most recently used speaker, so recording
        // ten words for the same person doesn't mean typing it ten times.
        newSpeaker = entries.maxByOrNull { it.createdAtEpochMillis }?.speaker ?: ""
    }

    val recorder = rememberRecorderController()

    // AC4: derived from the text (a space means a sentence), but only until
    // the user explicitly picks one -- from then on the explicit choice
    // wins even if the text changes underneath it.
    val derivedKind = if (newText.contains(' ')) EntryKind.SENTENCE else EntryKind.WORD
    val newKind = kindOverride ?: derivedKind

    var editingId by remember { mutableStateOf<String?>(null) }
    var editedText by remember { mutableStateOf("") }
    var reRecordingId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Eigene Aufnahmen", style = MaterialTheme.typography.headlineLarge)

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

            Text("Neue Aufnahme", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                label = { Text("Text") },
                modifier = Modifier.fillMaxWidth(),
            )

            Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                KindOption(label = "Wort", selected = newKind == EntryKind.WORD, onSelect = { kindOverride = EntryKind.WORD })
                KindOption(
                    label = "Satz",
                    selected = newKind == EntryKind.SENTENCE,
                    onSelect = { kindOverride = EntryKind.SENTENCE },
                )
            }

            OutlinedTextField(
                value = newSpeaker,
                onValueChange = { newSpeaker = it },
                label = { Text("Sprecher (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            // AC3: "Anhören" is available before "Speichern" -- the point
            // where a mistyped or mis-spoken entry is most likely to be
            // caught (ADR-0012 point 5), not a convenience.
            RecorderControls(recorder = recorder, permission = permission, queue = queue)

            // Saving audio without a text stays *allowed* (the text can be
            // added later), but it's worth a word: an untranscribed entry
            // is unusable for training until the text exists, and the
            // author asked for the warning after hitting exactly that case
            // (A53-Befund 2026-08-06). A quiet hint, not a dialog and not a
            // block -- the decision stays the user's.
            if (recorder.buffer != null && newText.isBlank()) {
                Text(
                    "Ohne Text lässt sich der Eintrag nicht trainieren. " +
                        "Du kannst ihn später über „Text ändern“ nachtragen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // AC3: disabled only while *both* a recording and a non-blank
            // text are missing -- either one alone is enough to save.
            Button(
                enabled = !recorder.isRecording && !recorder.isBusy && (recorder.buffer != null || newText.isNotBlank()),
                onClick = {
                    val audio = recorder.buffer
                    scope.launch {
                        repository.add(newText.trim(), newKind, newSpeaker.trim(), audio)
                        newText = ""
                        kindOverride = null
                        recorder.reset()
                        reload()
                    }
                },
            ) {
                Text("Speichern")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text("Meine Einträge", style = MaterialTheme.typography.titleMedium)

            if (isLoading) {
                CircularProgressIndicator()
            } else if (entries.isEmpty()) {
                Text("Noch keine eigenen Aufnahmen.", style = MaterialTheme.typography.bodyLarge)
            } else {
                val sorted = entries.sortedByDescending { it.createdAtEpochMillis }
                sorted.forEachIndexed { index, entry ->
                    EntryRow(
                        entry = entry,
                        queue = queue,
                        repository = repository,
                        permission = permission,
                        zoneId = clock.zoneId(),
                        isEditing = editingId == entry.id,
                        editedText = editedText,
                        onEditedTextChange = { editedText = it },
                        onStartEdit = {
                            editingId = entry.id
                            editedText = entry.text
                        },
                        onCancelEdit = { editingId = null },
                        onSaveEdit = {
                            val trimmed = editedText.trim()
                            scope.launch {
                                repository.updateText(entry.id, trimmed)
                                editingId = null
                                reload()
                            }
                        },
                        isReRecording = reRecordingId == entry.id,
                        onToggleReRecord = {
                            reRecordingId = if (reRecordingId == entry.id) null else entry.id
                        },
                        onPlaybackMissing = { status = "Keine Aufnahme für „${entry.text}“ vorhanden." },
                        onDeleteRequested = { pendingDeleteId = entry.id },
                    )
                    if (index < sorted.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Button(onClick = onBeenden) {
            Text("Zurück")
        }
    }

    val deleteTarget = entries.firstOrNull { it.id == pendingDeleteId }
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Eintrag löschen?") },
            text = {
                Text(
                    "„${deleteTarget.text}“ wird endgültig entfernt, inklusive der Aufnahme. " +
                        "Das lässt sich nicht rückgängig machen.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteTarget.id
                    scope.launch {
                        repository.delete(id)
                        pendingDeleteId = null
                        reload()
                    }
                }) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun EntryRow(
    entry: OwnEntry,
    queue: PlaybackQueue,
    repository: OwnCorpusRepository,
    permission: RecordingPermissionState,
    zoneId: String,
    isEditing: Boolean,
    editedText: String,
    onEditedTextChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    isReRecording: Boolean,
    onToggleReRecord: () -> Unit,
    onPlaybackMissing: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isEditing) {
            OutlinedTextField(
                value = editedText,
                onValueChange = onEditedTextChange,
                label = { Text("Text") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = editedText.isNotBlank(), onClick = onSaveEdit) {
                    Text("Speichern")
                }
                TextButton(onClick = onCancelEdit) {
                    Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Text(entry.text, style = MaterialTheme.typography.bodyLarge)
            val speakerPart = if (entry.speaker.isNotBlank()) " · ${entry.speaker}" else ""
            Text(
                "${germanKindLabel(entry.kind)}$speakerPart · ${formatTimestamp(entry.createdAtEpochMillis, zoneId)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // FlowRow, not a fixed Row (A53-Befund 2026-08-06): the four
            // actions overflow the phone's width, which didn't just look
            // bad -- "Neu aufnehmen" wrapped mid-label and "Löschen" was
            // pushed off-screen entirely, so the author reported delete as
            // missing. A plain Row clips its overflow instead of wrapping.
            // Same fix and same precedent as the RatingBar overflow
            // (Autor-Befund 2026-07-12).
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledTonalButton(onClick = {
                    scope.launch {
                        val audio = repository.audioFor(entry)
                        if (audio == null) onPlaybackMissing() else queue.play(audio)
                    }
                }) {
                    Text("Anhören")
                }
                FilledTonalButton(onClick = onStartEdit) {
                    Text("Text ändern")
                }
                FilledTonalButton(onClick = onToggleReRecord) {
                    Text(if (isReRecording) "Aufnahme schließen" else "Neu aufnehmen")
                }
                TextButton(onClick = onDeleteRequested) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (isReRecording) {
            // A fresh recorder (and its own AudioSource) per expansion --
            // created when this row opens its re-record panel, released
            // when it closes (DisposableEffect inside rememberRecorderController).
            val recorder = rememberRecorderController()
            RecorderControls(recorder = recorder, permission = permission, queue = queue, recordLabel = "Neu aufnehmen")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = recorder.buffer != null && !recorder.isRecording && !recorder.isBusy,
                    onClick = {
                        val buffer = recorder.buffer ?: return@Button
                        scope.launch {
                            repository.reRecord(entry.id, buffer)
                            onToggleReRecord()
                        }
                    },
                ) {
                    Text("Übernehmen")
                }
                TextButton(onClick = onToggleReRecord) {
                    Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// Same selectable-row pattern as EinstellungenScreen's RadioOption (private
// there too) -- the row carries the click target, RadioButton's own
// onClick stays null so there's exactly one handler.
@Composable
private fun KindOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * "Aufnehmen"/"Stopp" + "Anhören", permission-gated -- the same three
 * [RecordingPermissionStatus] branches as [DevPlaybackScreen]'s
 * Mikrofon-Rohtest, reused by both the "Neue Aufnahme" section and a row's
 * inline re-record panel.
 */
@Composable
private fun RecorderControls(
    recorder: RecorderController,
    permission: RecordingPermissionState,
    queue: PlaybackQueue,
    recordLabel: String = "Aufnehmen",
) {
    Text(recorder.status, style = MaterialTheme.typography.bodyMedium)

    when (permission.status) {
        RecordingPermissionStatus.GRANTED -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    enabled = !recorder.isBusy,
                    onClick = { if (recorder.isRecording) recorder.stop() else recorder.start() },
                ) {
                    Text(if (recorder.isRecording) "Stopp" else recordLabel)
                }
                FilledTonalButton(
                    enabled = !recorder.isRecording && !recorder.isBusy && recorder.buffer != null,
                    onClick = { recorder.buffer?.let { queue.play(it) } },
                ) {
                    Text("Anhören")
                }
            }
        }

        RecordingPermissionStatus.PERMANENTLY_DENIED -> {
            Text(
                "Mikrofonzugriff wurde dauerhaft abgelehnt. Zum Aufnehmen bitte in den " +
                    "System-Einstellungen freigeben.",
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = permission::openSystemSettings) {
                Text("Einstellungen öffnen")
            }
        }

        RecordingPermissionStatus.NOT_REQUESTED, RecordingPermissionStatus.DENIED -> {
            Text(
                "Zum Aufnehmen braucht AudioLex kurz Zugriff auf das Mikrofon.",
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = permission::request) {
                Text("Mikrofon-Berechtigung anfragen")
            }
        }
    }
}

private fun germanKindLabel(kind: EntryKind): String = when (kind) {
    EntryKind.WORD -> "Wort"
    EntryKind.SENTENCE -> "Satz"
}

/**
 * Records via [AudioSource] into a single merged [PcmBuffer], reimplementing
 * (not sharing) the record-and-merge flow from [DevPlaybackScreen]'s
 * Mikrofon-Rohtest -- that screen is explicitly frozen for this batch. Holds
 * its own Compose [androidx.compose.runtime.State] so both call sites
 * ("Neue Aufnahme" and a row's re-record panel) can drive their own
 * record/stop/listen UI off the same small piece of logic.
 */
private class RecorderController(
    private val source: AudioSource,
    private val scope: CoroutineScope,
) {
    private val chunks = mutableListOf<ShortArray>()
    private var job: Job? = null

    var isRecording by mutableStateOf(false)
        private set
    var isBusy by mutableStateOf(false)
        private set
    var buffer by mutableStateOf<PcmBuffer?>(null)
        private set
    var status by mutableStateOf("Noch keine Aufnahme.")
        private set

    fun start() {
        chunks.clear()
        buffer = null
        isRecording = true
        status = "Nimmt auf…"
        job = scope.launch {
            try {
                // record() blocks its calling thread between chunks (see the
                // android/jvm actuals) rather than truly suspending, so this
                // must run off the composition's own dispatcher -- otherwise
                // it freezes the UI thread for as long as the recording runs.
                withContext(Dispatchers.IO) {
                    source.record { chunk -> chunks.add(chunk) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isRecording = false
                // Surfaced, not swallowed (same posture as the Batch A dev
                // screen): a format mismatch or missing mic is a fact to
                // report, not a detail to route around.
                status = "Fehler bei der Aufnahme: ${e.message}"
            }
        }
    }

    fun stop() {
        val currentJob = job ?: return
        isRecording = false
        isBusy = true
        status = "Verarbeite Aufnahme…"
        scope.launch {
            try {
                currentJob.cancelAndJoin()
                job = null
                val totalSamples = chunks.sumOf { it.size }
                if (totalSamples == 0) {
                    status = "Keine Aufnahme (kein Signal empfangen)."
                    return@launch
                }
                val merged = ShortArray(totalSamples)
                var offset = 0
                for (chunk in chunks) {
                    chunk.copyInto(merged, offset)
                    offset += chunk.size
                }
                chunks.clear()
                buffer = PcmBuffer(merged, RECORDING_SAMPLE_RATE, RECORDING_CHANNELS)
                val seconds = totalSamples / RECORDING_SAMPLE_RATE
                status = "Aufnahme fertig (~$seconds s)."
            } finally {
                isBusy = false
            }
        }
    }

    /** Clears the current take (e.g. right after it was saved), back to the initial "nothing recorded" state. */
    fun reset() {
        buffer = null
        status = "Noch keine Aufnahme."
    }

    fun dispose() {
        job?.cancel()
        source.close()
    }
}

@Composable
private fun rememberRecorderController(): RecorderController {
    val scope = rememberCoroutineScope()
    val controller = remember { RecorderController(createAudioSource(), scope) }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    return controller
}
