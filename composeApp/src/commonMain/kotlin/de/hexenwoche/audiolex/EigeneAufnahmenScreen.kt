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
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.corpus.CorpusLanguage
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.corpus.OwnEntry
import de.hexenwoche.audiolex.core.i18n.Strings
import de.hexenwoche.audiolex.core.persistence.SessionRepository
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import de.hexenwoche.audiolex.core.time.Clock
import kotlinx.coroutines.launch

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
 * Recording uses the shared [RecorderController] (extracted into
 * `RecorderControls.kt` when the own-noise screen, Backlog M4 "Eigene
 * Störgeräusche", AC3, needed the same recorder): one controller drives the
 * "Neue Aufnahme" section and, inline, a row's re-record panel. The flow
 * itself reimplements [DevPlaybackScreen]'s Mikrofon-Rohtest rather than
 * sharing it -- that screen stays frozen.
 *
 * Title and "Zurück" stay pinned; only the content between them scrolls
 * (same `Modifier.weight(1f).verticalScroll(...)` pattern as
 * `EinstellungenScreen`/`SitzungshistorieScreen` -- an unreachable "Zurück"
 * has been a real bug in this project twice before).
 */
@Composable
fun EigeneAufnahmenScreen(
    repository: OwnCorpusRepository,
    sessionRepository: SessionRepository,
    ownNoiseRepository: OwnNoiseRepository,
    clock: Clock,
    corpusLanguage: CorpusLanguage,
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

    var entries by remember { mutableStateOf<List<OwnEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // Read alongside the entries for the export button's enabled state (AC5):
    // a backup is worth taking once any content kind exists, not only
    // entries. Kept fresh in [reload] so an import that adds noises flips
    // the button without a screen re-entry.
    var hasNoises by remember { mutableStateOf(false) }

    suspend fun reload() {
        entries = repository.all()
        hasNoises = ownNoiseRepository.all().isNotEmpty()
    }

    var newText by remember { mutableStateOf("") }
    var newSpeaker by remember { mutableStateOf("") }
    var kindOverride by remember { mutableStateOf<EntryKind?>(null) }
    // Prefilled with the language currently being trained (ADR-0016) --
    // recording for the drawer you are standing in is the overwhelmingly
    // common case, and it stays a free choice.
    var newLanguage by remember(corpusLanguage) { mutableStateOf(corpusLanguage) }

    LaunchedEffect(Unit) {
        reload()
        isLoading = false
        // AC4b: prefilled with the most recently used speaker, so recording
        // ten words for the same person doesn't mean typing it ten times.
        newSpeaker = entries.maxByOrNull { it.createdAtEpochMillis }?.speaker ?: ""
    }

    // AC8 (Backlog Eigen-Korpus Batch D): every speaker name already used at
    // least once, offered as a pick instead of only ever prefilling the most
    // recent one -- this is the field's Batch D promotion from Beschriftung
    // to Struktur (ADR-0012 Nachtrag), so a typo here would silently split
    // one contingent into two. Blank stays out of the suggestion list: it's
    // already the default for "kein Sprecher", nothing to suggest there.
    val knownSpeakers = entries.map { it.speaker }.filter { it.isNotBlank() }.distinct().sorted()

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

    // Guards both backup buttons while one is running: an export of a large
    // collection takes a moment, and a second tap mid-write would be a
    // second archive of the same data at best.
    var isBackupBusy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(strings.ownRecordings, style = MaterialTheme.typography.headlineLarge)

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

            Text(strings.sectionNewRecording, style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                label = { Text(strings.fieldText) },
                modifier = Modifier.fillMaxWidth(),
            )

            Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                for (kind in EntryKind.entries) {
                    KindOption(
                        label = strings.kindLabel(kind),
                        selected = newKind == kind,
                        onSelect = { kindOverride = kind },
                    )
                }
            }

            Text(strings.fieldEntryLanguage, style = MaterialTheme.typography.bodyLarge)

            Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                for (language in CorpusLanguage.entries) {
                    KindOption(
                        label = strings.corpusLanguageLabel(language),
                        selected = newLanguage == language,
                        onSelect = { newLanguage = language },
                    )
                }
            }

            // The author's own framing (Autor-Entscheid 2026-08-24): this is
            // a filing decision, not a claim about the audio. Saying so here
            // is what keeps someone from expecting the app to notice when
            // they slip an English sentence into the German drawer.
            Text(
                strings.entryLanguageHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = newSpeaker,
                onValueChange = { newSpeaker = it },
                label = { Text(strings.fieldSpeakerOptional) },
                modifier = Modifier.fillMaxWidth(),
            )

            // AC8: a tap fills the field with the exact existing spelling --
            // free typing above stays possible for a genuinely new speaker.
            // FlowRow so the list wraps instead of clipping once there are
            // more than a couple of speakers (same overflow fix as the
            // per-entry action row below, A53-Befund 2026-08-06).
            if (knownSpeakers.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (speaker in knownSpeakers) {
                        FilledTonalButton(onClick = { newSpeaker = speaker }) {
                            Text(speaker)
                        }
                    }
                }
            }

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
                    strings.untranscribedHint,
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
                        repository.add(newText.trim(), newKind, newSpeaker.trim(), newLanguage, audio)
                        newText = ""
                        kindOverride = null
                        recorder.reset()
                        reload()
                    }
                },
            ) {
                Text(strings.save)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(strings.sectionMyEntries, style = MaterialTheme.typography.titleMedium)

            if (isLoading) {
                CircularProgressIndicator()
            } else if (entries.isEmpty()) {
                Text(strings.noOwnRecordingsYet, style = MaterialTheme.typography.bodyLarge)
            } else {
                val sorted = entries.sortedByDescending { it.createdAtEpochMillis }
                sorted.forEachIndexed { index, entry ->
                    EntryRow(
                        entry = entry,
                        strings = strings,
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
                        onPlaybackMissing = { status = strings.noRecordingAvailable(entry.text) },
                        onDeleteRequested = { pendingDeleteId = entry.id },
                    )
                    if (index < sorted.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(strings.sectionBackup, style = MaterialTheme.typography.titleMedium)

            // AC6: after this version the system backup is off (AC4), so a
            // user who never exports has *less* protection than before. That
            // has to be visible -- but as one quiet line, not a dialog and
            // not a recurring nag: ADR-0013 hands the decision to the user
            // on purpose.
            Text(
                strings.backupExplainer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    // A backup is worth taking once any of the three content
                    // kinds exists (AC5) -- not only when entries do.
                    enabled = !isBackupBusy && (entries.isNotEmpty() || hasNoises),
                    onClick = {
                        scope.launch {
                            isBackupBusy = true
                            status = runExport(repository, ownNoiseRepository, sessionRepository, backup, clock, strings)
                            isBackupBusy = false
                        }
                    },
                ) {
                    Text(strings.export)
                }
                Button(
                    enabled = !isBackupBusy,
                    onClick = {
                        isBackupBusy = true
                        backup.pickArchive { bytes ->
                            if (bytes == null) {
                                // Cancelling the picker is not a failure and
                                // gets no message -- the user knows what they did.
                                isBackupBusy = false
                            } else {
                                scope.launch {
                                    status = runImport(repository, ownNoiseRepository, sessionRepository, bytes, strings)
                                    reload()
                                    isBackupBusy = false
                                }
                            }
                        }
                    },
                ) {
                    Text(strings.importAction)
                }
            }
        }

        Button(onClick = onBeenden) {
            Text(strings.back)
        }
    }

    val deleteTarget = entries.firstOrNull { it.id == pendingDeleteId }
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(strings.deleteEntryTitle) },
            text = { Text(strings.deleteConfirmBody(deleteTarget.text)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteTarget.id
                    scope.launch {
                        repository.delete(id)
                        pendingDeleteId = null
                        reload()
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
private fun EntryRow(
    entry: OwnEntry,
    strings: Strings,
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
                label = { Text(strings.fieldText) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = editedText.isNotBlank(), onClick = onSaveEdit) {
                    Text(strings.save)
                }
                TextButton(onClick = onCancelEdit) {
                    Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Text(entry.text, style = MaterialTheme.typography.bodyLarge)
            val speakerPart = if (entry.speaker.isNotBlank()) " · ${entry.speaker}" else ""
            // Which drawer this entry sits in (ADR-0016) -- worth showing,
            // because an entry filed under the other language is invisible in
            // training and its absence would otherwise be a puzzle. An
            // unknown tag falls back to the raw tag rather than a guess.
            val languagePart = CorpusLanguage.entries.firstOrNull { it.matches(entry.language) }
                ?.let { " · ${strings.corpusLanguageLabel(it)}" }
                ?: " · ${entry.language}"
            Text(
                "${strings.kindLabel(entry.kind)}$languagePart$speakerPart · " +
                    formatTimestamp(entry.createdAtEpochMillis, zoneId),
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
                    Text(strings.listen)
                }
                FilledTonalButton(onClick = onStartEdit) {
                    Text(strings.editText)
                }
                FilledTonalButton(onClick = onToggleReRecord) {
                    Text(if (isReRecording) strings.closeRecording else strings.reRecord)
                }
                TextButton(onClick = onDeleteRequested) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (isReRecording) {
            // A fresh recorder (and its own AudioSource) per expansion --
            // created when this row opens its re-record panel, released
            // when it closes (DisposableEffect inside rememberRecorderController).
            val recorder = rememberRecorderController()
            RecorderControls(recorder = recorder, permission = permission, queue = queue, recordLabel = strings.reRecord)
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
                    Text(strings.applyRecording)
                }
                TextButton(onClick = onToggleReRecord) {
                    Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
 * Exports and reports the outcome as the screen's status line (AC1). Every
 * path ends in a sentence the user can act on -- including the one where the
 * file couldn't be written, which is otherwise indistinguishable from a
 * successful backup that simply isn't there when it's needed.
 *
 * The sentence is assembled from catalog pieces rather than formatted from a
 * template (ADR-0015): the count phrases and the joiner are per-language, so
 * German gets "zwei Aufnahme(n) und eine Sitzung(en)" in its own shorthand
 * and English gets proper plurals, without either having to fit the other's
 * grammar.
 */
private suspend fun runExport(
    repository: OwnCorpusRepository,
    ownNoises: OwnNoiseRepository,
    sessions: SessionRepository,
    backup: BackupActions,
    clock: Clock,
    strings: Strings,
): String {
    val now = clock.nowEpochMillis()
    val export = exportBackup(repository, ownNoises, sessions, now)
    if (export.exported == 0 && export.sessions == 0 && export.noises == 0) {
        return strings.nothingToBackUp
    }
    val location = backup.saveToDocuments(backupFileName(now), export.bytes)
        ?: return strings.backupWriteFailed

    // AC5: all three kinds named separately. A backup that holds recordings
    // but no noises/sessions yet says so by staying silent about them, not
    // by claiming "0".
    val what = strings.joinLast(
        buildList {
            if (export.exported > 0) add(strings.recordingCount(export.exported))
            if (export.noises > 0) add(strings.noiseCount(export.noises))
            if (export.sessions > 0) add(strings.sessionCount(export.sessions))
        },
    )
    val skipped = if (export.skippedWithoutRecording > 0) {
        strings.backupSkippedWithoutRecording(export.skippedWithoutRecording)
    } else {
        ""
    }
    return strings.backupSaved(what, location) + skipped
}

/**
 * Imports and reports the outcome (AC2/AC3). The counts are spelled out
 * rather than reduced to "fertig": on a restore the difference between
 * "12 hinzugefügt" and "12 waren schon da" is the whole information.
 */
private suspend fun runImport(
    repository: OwnCorpusRepository,
    ownNoises: OwnNoiseRepository,
    sessions: SessionRepository,
    bytes: ByteArray,
    strings: Strings,
): String =
    when (val result = importBackup(bytes, repository, ownNoises, sessions)) {
        ArchiveImport.Unreadable -> strings.archiveUnreadable

        is ArchiveImport.Merged -> buildString {
            // AC5: the three kinds are reported separately, and each stays
            // silent when the archive carried none of it -- an old backup
            // without a sitzungen/ or stoergeraeusche/ folder must not read
            // as "0 Sitzungen"/"0 Geräusche".
            val added = buildList {
                if (result.added > 0) add(strings.entryCount(result.added))
                if (result.noisesAdded > 0) add(strings.noiseCount(result.noisesAdded))
                if (result.sessionsAdded > 0) add(strings.sessionCount(result.sessionsAdded))
            }
            val known = buildList {
                if (result.alreadyPresent > 0) add(strings.entryCount(result.alreadyPresent))
                if (result.noisesAlreadyPresent > 0) add(strings.noiseCount(result.noisesAlreadyPresent))
                if (result.sessionsAlreadyPresent > 0) add(strings.sessionCount(result.sessionsAlreadyPresent))
            }
            when {
                added.isNotEmpty() -> {
                    append(strings.importAdded(strings.joinLast(added)))
                    if (known.isNotEmpty()) append(strings.importAlsoAlreadyPresent(strings.joinLast(known)))
                    append(".")
                }

                known.isNotEmpty() -> append(strings.importOnlyAlreadyPresent(strings.joinLast(known)))

                else -> append(strings.importNothingAdded)
            }
            if (result.unusable > 0) {
                append(strings.importSkippedEntries(result.unusable))
            }
            if (result.noisesUnusable > 0) {
                append(strings.importSkippedNoises(result.noisesUnusable))
            }
        }
    }
