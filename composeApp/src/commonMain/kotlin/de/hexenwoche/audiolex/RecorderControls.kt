package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.audio.AudioSource
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import de.hexenwoche.audiolex.core.audio.RECORDING_CHANNELS
import de.hexenwoche.audiolex.core.audio.RECORDING_SAMPLE_RATE
import de.hexenwoche.audiolex.core.audio.createAudioSource
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Records via [AudioSource] into a single merged [PcmBuffer], implementing
 * the record-and-merge flow first written for [DevPlaybackScreen]'s
 * Mikrofon-Rohtest. Holds its own Compose
 * [androidx.compose.runtime.State] so every call site (the own-corpus
 * "Neue Aufnahme" section, a row's inline re-record panel, and the own-noise
 * recording mask -- Backlog M4 "Eigene Störgeräusche", AC3) can drive its
 * own record/stop/listen UI off the same small piece of logic.
 *
 * Lives in its own file since the own-noise screen reuses it (it was part of
 * `EigeneAufnahmenScreen.kt` before; the AC3 "dafür aus der Datei herauslösen,
 * falls nötig"). No recording-duration limit anywhere: noise loops are
 * minutes long, words are seconds -- the same controller serves both.
 */
internal class RecorderController(
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
internal fun rememberRecorderController(): RecorderController {
    val scope = rememberCoroutineScope()
    val controller = remember { RecorderController(createAudioSource(), scope) }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    return controller
}

/**
 * "Aufnehmen"/"Stopp" + "Anhören", permission-gated -- the same three
 * [RecordingPermissionStatus] branches as [DevPlaybackScreen]'s
 * Mikrofon-Rohtest, shared by every recording mask in the app.
 */
@Composable
internal fun RecorderControls(
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
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = permission::openSystemSettings) {
                Text("Einstellungen öffnen")
            }
        }

        RecordingPermissionStatus.NOT_REQUESTED, RecordingPermissionStatus.DENIED -> {
            Text(
                "Zum Aufnehmen braucht AudioLex kurz Zugriff auf das Mikrofon.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = permission::request) {
                Text("Mikrofon-Berechtigung anfragen")
            }
        }
    }
}
