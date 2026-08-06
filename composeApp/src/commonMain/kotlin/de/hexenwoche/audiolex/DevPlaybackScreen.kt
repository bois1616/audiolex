package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
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
import de.hexenwoche.audiolex.core.audio.StereoGain
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.corpus.LoadedCorpus
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import de.hexenwoche.audiolex.generated.resources.Res
import kotlinx.coroutines.Dispatchers
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
 */
@Composable
fun DevPlaybackScreen() {
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    queue.play { buildTwoWordsPerEar(left.fileRef, right.fileRef, gain) }
                },
            ) {
                Text(label)
            }
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(currentCorpus.words) { word ->
            val recording = currentCorpus.recordingFor(word.id)
            Button(
                enabled = recording != null,
                onClick = {
                    val rec = recording ?: return@Button
                    status = "Spiele „${word.text}“ (${rec.voiceId})…"
                    queue.play { decodeRecording(rec.fileRef) }
                },
            ) {
                Text(word.text)
            }
        }
    }
}

private suspend fun decodeRecording(fileRef: String): PcmBuffer =
    withContext(Dispatchers.Default) {
        val bytes = Res.readBytes("files/corpus/$fileRef")
        WavFile.decode(bytes)
    }

/**
 * Builds a stereo buffer with [leftFileRef] panned to the left ear and
 * [rightFileRef] panned to the right ear at the same time, then applies
 * [gain] on top (so e.g. LEFT_ONLY silences the right word entirely,
 * proving the ear that hears something is the ear StereoGain intended).
 * Only builds the buffer -- playing it is the caller's (the queue's) job.
 */
private suspend fun buildTwoWordsPerEar(
    leftFileRef: String,
    rightFileRef: String,
    gain: StereoGain,
): PcmBuffer =
    withContext(Dispatchers.Default) {
        val left = WavFile.decode(Res.readBytes("files/corpus/$leftFileRef"))
        val right = WavFile.decode(Res.readBytes("files/corpus/$rightFileRef"))
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
