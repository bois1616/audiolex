package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.audio.AudioSink
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import de.hexenwoche.audiolex.core.audio.StereoGain
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.corpus.AudioRecording
import de.hexenwoche.audiolex.core.corpus.Word
import de.hexenwoche.audiolex.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Playback smoke test for M1 (backlog: Desktop-/Android-Sink verifizieren):
 * loads the real generated corpus and plays a chosen word end-to-end
 * through WavFile + AudioSink. Not the learning-mode UI (that's M2).
 */
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("AudioLex", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Hörtraining: Klang → Wort → Bedeutung",
                    style = MaterialTheme.typography.bodyLarge,
                )
                WordPlaybackList()
            }
        }
    }
}

private val json = Json { ignoreUnknownKeys = true }

@Composable
private fun WordPlaybackList() {
    var words by remember { mutableStateOf<List<Word>?>(null) }
    var recordings by remember { mutableStateOf<List<AudioRecording>>(emptyList()) }
    var status by remember { mutableStateOf("Lade Korpus…") }
    val scope = rememberCoroutineScope()
    val sink = remember { createAudioSink() }

    LaunchedEffect(Unit) {
        val wordsJson = Res.readBytes("files/corpus/words.json").decodeToString()
        val recordingsJson = Res.readBytes("files/corpus/recordings.json").decodeToString()
        words = json.decodeFromString<List<Word>>(wordsJson)
        recordings = json.decodeFromString<List<AudioRecording>>(recordingsJson)
        status = "Bereit"
    }

    Text(status, style = MaterialTheme.typography.bodyMedium)

    val currentWords = words
    if (currentWords == null) {
        CircularProgressIndicator()
        return
    }

    // Channel-separation smoke test (backlog M1): plays two *different*
    // words, one per ear, so the test doesn't rely on judging the same
    // word's loudness -- with a single hearing aid on one ear, hearing
    // "which word" is a much clearer signal than "louder/quieter".
    // Not the learning-mode UI (that's M2's session/settings work).
    val leftWordRecording = recordings.firstOrNull { it.wordId == currentWords.getOrNull(0)?.id }
    val rightWordRecording = recordings.firstOrNull { it.wordId == currentWords.getOrNull(1)?.id }
    val leftWordText = currentWords.getOrNull(0)?.text ?: "?"
    val rightWordText = currentWords.getOrNull(1)?.text ?: "?"
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
                    scope.launch {
                        status = "Kanaltest: $label…"
                        try {
                            playTwoWordsPerEar(sink, left.fileRef, right.fileRef, gain)
                            status = "Bereit"
                        } catch (e: Exception) {
                            status = "Fehler: ${e.message}"
                        }
                    }
                },
            ) {
                Text(label)
            }
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(currentWords) { word ->
            val recording = recordings.firstOrNull { it.wordId == word.id }
            Button(
                enabled = recording != null,
                onClick = {
                    val rec = recording ?: return@Button
                    scope.launch {
                        status = "Spiele „${word.text}“ (${rec.voiceId})…"
                        try {
                            playRecording(sink, rec.fileRef)
                            status = "Bereit"
                        } catch (e: Exception) {
                            status = "Fehler: ${e.message}"
                        }
                    }
                },
            ) {
                Text(word.text)
            }
        }
    }
}

private suspend fun playRecording(sink: AudioSink, fileRef: String) {
    withContext(Dispatchers.Default) {
        val bytes = Res.readBytes("files/corpus/$fileRef")
        val buffer = WavFile.decode(bytes)
        sink.play(buffer)
    }
}

/**
 * Plays [leftFileRef] panned to the left ear and [rightFileRef] panned to
 * the right ear at the same time, then applies [gain] on top (so e.g.
 * LEFT_ONLY silences the right word entirely, proving the ear that hears
 * something is the ear StereoGain intended).
 */
private suspend fun playTwoWordsPerEar(
    sink: AudioSink,
    leftFileRef: String,
    rightFileRef: String,
    gain: StereoGain,
) {
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
        sink.play(PcmBuffer(stereo, left.sampleRate, channels = 2))
    }
}
