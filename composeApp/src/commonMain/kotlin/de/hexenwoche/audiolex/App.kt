package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(currentWords) { word ->
            val recording = recordings.firstOrNull { it.wordId == word.id }
            Button(
                enabled = recording != null,
                onClick = {
                    val rec = recording ?: return@Button
                    scope.launch {
                        status = "Spiele „${word.text}“ (${rec.voiceId})…"
                        playRecording(sink, rec.fileRef)
                        status = "Bereit"
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
