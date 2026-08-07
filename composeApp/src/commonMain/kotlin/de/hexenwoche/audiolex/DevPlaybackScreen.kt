package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import de.hexenwoche.audiolex.core.audio.RECORDING_CHANNELS
import de.hexenwoche.audiolex.core.audio.RECORDING_SAMPLE_RATE
import de.hexenwoche.audiolex.core.audio.StereoGain
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.audio.createAudioSource
import de.hexenwoche.audiolex.core.corpus.AudioRecording
import de.hexenwoche.audiolex.core.corpus.LoadedCorpus
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Playback smoke test for M1 (backlog: Desktop-/Android-Sink verifizieren):
 * loads the real generated corpus and plays a chosen word end-to-end
 * through WavFile + AudioSink. Not the learning-mode UI (that's M2) --
 * kept as a standalone dev screen so it doesn't grow into App.kt's
 * eventual navigation host (Opus-Review 2026-07-07).
 *
 * Playback goes through [PlaybackQueue], same as [LernmodusScreen]/
 * [PruefmodusScreen] (Backlog M4 "Dev-Kanaltest: überlappende Wiedergaben",
 * A53-Befund 2026-08-06): this used to be the one remaining screen calling
 * `sink.play()` directly in a fire-and-forget `scope.launch`, so a fast
 * repeat tap never cancelled the previous playback -- both decode and play
 * happened outside any cancellable job, stacking overlapping `AudioTrack`s.
 * Decode now runs *inside* the queue's producer for both playback paths (the
 * three channel-test buttons and the word list below), same fix as the
 * "Kakaffee" regression in the training screens (Autor-Finding 2026-07-13):
 * decoding first and only then calling `queue.play(buffer)` would still
 * leave the decode step racy.
 *
 * [ownCorpusRepository] exists here only so [decodeRecording]/
 * [buildTwoWordsPerEar] can go through the same [readRecordingBytes] as the
 * training screens (Backlog Eigen-Korpus Batch C, AC5) -- this screen's own
 * [loadCorpus] call below stays unfiltered/mitgeliefert-only as it always
 * has (no source switch here, that's the training screens' territory), so
 * every recording this screen ever plays resolves as
 * [de.hexenwoche.audiolex.core.corpus.RecordingSource.MITGELIEFERT] and the
 * repository is never actually asked for bytes.
 */
@Composable
fun DevPlaybackScreen(ownCorpusRepository: OwnCorpusRepository) {
    var corpus by remember { mutableStateOf<LoadedCorpus?>(null) }
    var status by remember { mutableStateOf("Lade Korpus…") }
    val scope = rememberCoroutineScope()
    val sink = remember { createAudioSink() }
    val queue = remember {
        PlaybackQueue(sink, scope, onError = { e ->
            status = "Fehler: ${e.message}"
        })
    }

    // This screen has no dispose handling of its own further up the tree --
    // it's embedded directly by DevKanaltestScreen in App.kt, which doesn't
    // own sink/queue -- so the cleanup lives here, next to where they're
    // created, same as the training screens since v0.17.0.
    DisposableEffect(Unit) {
        onDispose {
            queue.stop()
            sink.close()
        }
    }

    LaunchedEffect(Unit) {
        // Unfiltered (kind = null): the dev list shows every entry.
        corpus = loadCorpus()
        status = "Bereit"
    }

    Text(status, style = MaterialTheme.typography.bodyMedium)

    MikrofonRohtestSection(queue)

    val currentCorpus = corpus
    if (currentCorpus == null) {
        CircularProgressIndicator()
        return
    }

    // Channel-separation smoke test (backlog M1): plays two *different*
    // words, one per ear, so the test doesn't rely on judging the same
    // word's loudness -- with a single hearing aid on one ear, hearing
    // "which word" is a much clearer signal than "louder/quieter".
    // Not the learning-mode UI (that's M2's session/settings work).
    val leftWordRecording = currentCorpus.words.getOrNull(0)?.let { currentCorpus.recordingFor(it.id) }
    val rightWordRecording = currentCorpus.words.getOrNull(1)?.let { currentCorpus.recordingFor(it.id) }
    val leftWordText = currentCorpus.words.getOrNull(0)?.text ?: "?"
    val rightWordText = currentCorpus.words.getOrNull(1)?.text ?: "?"
    // FlowRow, not Row: the labels carry the word texts ("Nur „Haus“ rechts"),
    // so the three buttons overflow the A53's width and the last one gets
    // squeezed into a one-character-wide column. Visible on the device only
    // once the corpus list below shrank to two entries and stopped drawing
    // the eye away from it. Same overflow, same remedy as the speaker chips
    // and the per-entry action row (A53-Befund 2026-08-06). Beyond this
    // batch's ACs, but shipping a mangled control on the very screen the
    // batch is about would be worse.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((label, gain) in listOf(
            "Nur „$leftWordText“ links" to StereoGain.LEFT_ONLY,
            "Beide" to StereoGain.BOTH,
            "Nur „$rightWordText“ rechts" to StereoGain.RIGHT_ONLY,
        )) {
            Button(
                enabled = leftWordRecording != null && rightWordRecording != null,
                onClick = {
                    val left = leftWordRecording ?: return@Button
                    val right = rightWordRecording ?: return@Button
                    status = "Kanaltest: $label…"
                    // Decode + mix happen inside the producer (AC1): a fast
                    // follow-up tap cancels this job, decode included,
                    // instead of two decodes racing to the sink.
                    queue.play { buildTwoWordsPerEar(left, right, gain, ownCorpusRepository) }
                },
            ) {
                Text(label)
            }
        }
    }

    // AC2: only the two words the channel test itself uses, not the whole
    // corpus. The list had grown to 58 entries including sentences, which
    // turned the channel tool into a corpus browser -- Autor 2026-08-06:
    // "für den Test brauche ich nur Ball und Haus". These are the same two
    // words the per-ear buttons above play, so hearing one on its own is the
    // natural cross-check when a channel sounds wrong.
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(currentCorpus.words.take(2)) { word ->
            val recording = currentCorpus.recordingFor(word.id)
            Button(
                enabled = recording != null,
                onClick = {
                    val rec = recording ?: return@Button
                    status = "Spiele „${word.text}“ (${rec.voiceId})…"
                    queue.play { decodeRecording(rec, ownCorpusRepository) }
                },
            ) {
                Text(word.text)
            }
        }
    }
}

/**
 * Mikrofon-Rohtest (Backlog Eigen-Korpus Batch A, AC5): record, then listen
 * back -- nothing is saved, no text, no corpus link (explicit Nicht-Ziele).
 * Purpose is narrower than it looks: this is where the recording *format*
 * gets checked on a real device before Batch B builds persistence on top of
 * it (ADR-0012 Konsequenzen).
 *
 * Playback goes through the caller's [queue] (same [PlaybackQueue] the rest
 * of this screen uses, AC5), but recording has its own
 * [de.hexenwoche.audiolex.core.audio.AudioSource]/scope -- record and
 * playback are independent capabilities here, unlike the channel-test
 * buttons above which only ever play.
 */
@Composable
private fun MikrofonRohtestSection(queue: PlaybackQueue) {
    val scope = rememberCoroutineScope()
    val source = remember { createAudioSource() }
    val permission = rememberRecordingPermissionState()

    val chunks = remember { mutableListOf<ShortArray>() }
    var recordingJob by remember { mutableStateOf<Job?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    // True from "Stopp" tapped until the merge below has actually finished
    // reading `chunks` -- without this, a fast "Stopp" then "Aufnehmen"
    // could start clearing/refilling `chunks` while the previous stop's
    // merge loop is still iterating it (both run on `scope`, but the merge
    // is itself inside its own suspending `launch`, not synchronous with
    // the button tap).
    var isProcessingStop by remember { mutableStateOf(false) }
    var recordedBuffer by remember { mutableStateOf<PcmBuffer?>(null) }
    var recordStatus by remember { mutableStateOf("Noch keine Aufnahme.") }

    DisposableEffect(Unit) {
        onDispose {
            recordingJob?.cancel()
            source.close()
        }
    }

    fun startRecording() {
        chunks.clear()
        recordedBuffer = null
        isRecording = true
        recordStatus = "Nimmt auf…"
        recordingJob = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    source.record { chunk -> chunks.add(chunk) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isRecording = false
                // Surfaced here, not swallowed or silently downgraded to
                // another format (the whole point of this batch, ADR-0012
                // Konsequenzen: a format mismatch or missing mic is a fact
                // to report, not a detail to route around).
                recordStatus = "Fehler bei der Aufnahme: ${e.message}"
            }
        }
    }

    fun stopRecording() {
        val job = recordingJob ?: return
        isRecording = false
        isProcessingStop = true
        recordStatus = "Verarbeite Aufnahme…"
        scope.launch {
            try {
                job.cancelAndJoin()
                recordingJob = null
                val totalSamples = chunks.sumOf { it.size }
                if (totalSamples == 0) {
                    recordStatus = "Keine Aufnahme (kein Signal empfangen)."
                    return@launch
                }
                val merged = ShortArray(totalSamples)
                var offset = 0
                for (chunk in chunks) {
                    chunk.copyInto(merged, offset)
                    offset += chunk.size
                }
                chunks.clear()
                recordedBuffer = PcmBuffer(merged, RECORDING_SAMPLE_RATE, RECORDING_CHANNELS)
                val seconds = totalSamples / RECORDING_SAMPLE_RATE
                recordStatus = "Aufnahme fertig (~$seconds s, $totalSamples Samples)."
            } finally {
                isProcessingStop = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mikrofon-Rohtest", style = MaterialTheme.typography.titleMedium)

        when (permission.status) {
            RecordingPermissionStatus.GRANTED -> {
                Text(recordStatus, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !isProcessingStop,
                        onClick = { if (isRecording) stopRecording() else startRecording() },
                    ) {
                        Text(if (isRecording) "Stopp" else "Aufnehmen")
                    }
                    Button(
                        enabled = !isRecording && !isProcessingStop && recordedBuffer != null,
                        onClick = { recordedBuffer?.let { queue.play(it) } },
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
                Button(onClick = permission::openSystemSettings) {
                    Text("Einstellungen öffnen")
                }
            }

            RecordingPermissionStatus.NOT_REQUESTED, RecordingPermissionStatus.DENIED -> {
                Text(
                    "Zum Aufnehmen braucht AudioLex kurz Zugriff auf das Mikrofon -- nur für " +
                        "selbst ausgelöste Aufnahmen, nichts läuft im Hintergrund mit.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = permission::request) {
                    Text("Mikrofon-Berechtigung anfragen")
                }
            }
        }
    }
}

private suspend fun decodeRecording(recording: AudioRecording, ownCorpusRepository: OwnCorpusRepository): PcmBuffer =
    withContext(Dispatchers.Default) {
        val bytes = readRecordingBytes(recording, ownCorpusRepository)
        WavFile.decode(bytes)
    }

/**
 * Builds a stereo buffer with [leftRecording] panned to the left ear and
 * [rightRecording] panned to the right ear at the same time, then applies
 * [gain] on top (so e.g. LEFT_ONLY silences the right word entirely,
 * proving the ear that hears something is the ear StereoGain intended).
 * Only builds the buffer -- playing it is the caller's (the queue's) job.
 */
private suspend fun buildTwoWordsPerEar(
    leftRecording: AudioRecording,
    rightRecording: AudioRecording,
    gain: StereoGain,
    ownCorpusRepository: OwnCorpusRepository,
): PcmBuffer =
    withContext(Dispatchers.Default) {
        val left = WavFile.decode(readRecordingBytes(leftRecording, ownCorpusRepository))
        val right = WavFile.decode(readRecordingBytes(rightRecording, ownCorpusRepository))
        require(left.sampleRate == right.sampleRate) { "sample rates differ" }
        require(left.channels == 1 && right.channels == 1) { "expected mono corpus recordings" }

        val frameCount = maxOf(left.frameCount, right.frameCount)
        val stereo = ShortArray(frameCount * 2)
        for (frame in 0 until frameCount) {
            val leftSample = left.samples.getOrElse(frame) { 0 }
            val rightSample = right.samples.getOrElse(frame) { 0 }
            stereo[frame * 2] = (leftSample * gain.left).toInt().toShort()
            stereo[frame * 2 + 1] = (rightSample * gain.right).toInt().toShort()
        }
        PcmBuffer(stereo, left.sampleRate, channels = 2)
    }
