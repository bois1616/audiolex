package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.corpus.AudioRecording
import de.hexenwoche.audiolex.core.corpus.Word
import de.hexenwoche.audiolex.core.session.LearningSession
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import de.hexenwoche.audiolex.generated.resources.Res
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private sealed interface LernmodusState {
    data object Loading : LernmodusState
    data object EmptyCorpus : LernmodusState
    data class Running(val session: LearningSession) : LernmodusState
    data object Finished : LernmodusState
    data class Error(val message: String) : LernmodusState
}

/**
 * Lernmodus (Szenarien S1, S2, S5, S7; Gestalt: DESIGN.md "Trainings-Screens
 * im Detail"). Playback goes exclusively through [PlaybackQueue] -- a direct
 * `sink.play()` call (as in [DevPlaybackScreen], which predates the queue)
 * would let a fast double-tap on "Wiederholen" overlap two playbacks.
 */
@Composable
fun LernmodusScreen(onBeenden: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sink = remember { createAudioSink() }
    var state by remember { mutableStateOf<LernmodusState>(LernmodusState.Loading) }
    val queue = remember {
        PlaybackQueue(sink, scope, onError = { e ->
            state = LernmodusState.Error("Wiedergabe fehlgeschlagen: ${e.message}")
        })
    }
    var recordings by remember { mutableStateOf<List<AudioRecording>>(emptyList()) }

    DisposableEffect(Unit) {
        onDispose { queue.stop() }
    }

    LaunchedEffect(Unit) {
        try {
            val wordsJson = Res.readBytes("files/corpus/words.json").decodeToString()
            val recordingsJson = Res.readBytes("files/corpus/recordings.json").decodeToString()
            val words = json.decodeFromString<List<Word>>(wordsJson)
            recordings = json.decodeFromString<List<AudioRecording>>(recordingsJson)
            state = if (words.isEmpty()) {
                LernmodusState.EmptyCorpus
            } else {
                LernmodusState.Running(LearningSession(words))
            }
        } catch (e: Exception) {
            state = LernmodusState.Error("Korpus konnte nicht geladen werden: ${e.message}")
        }
    }

    // Plays the current word once whenever the session moves to a new word
    // (S1: happy path). Repeats (S2) are triggered explicitly by the button.
    LaunchedEffect(state) {
        val running = state as? LernmodusState.Running ?: return@LaunchedEffect
        playCurrentWord(running.session, recordings, queue) { message ->
            state = LernmodusState.Error(message)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val current = state) {
            is LernmodusState.Loading -> CircularProgressIndicator()

            is LernmodusState.EmptyCorpus -> {
                Text("Kein Wort im Korpus vorhanden.", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onBeenden) { Text("Zurück zum Start") }
            }

            is LernmodusState.Error -> {
                Text(current.message, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onBeenden) { Text("Zurück zum Start") }
            }

            is LernmodusState.Finished -> {
                Text(
                    "Fertig! Wörter durchlaufen.",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Button(onClick = onBeenden) { Text("Zurück zum Start") }
            }

            is LernmodusState.Running -> {
                val session = current.session
                Text(
                    "${session.progress} / ${session.total}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(session.currentWord.text, style = MaterialTheme.typography.displayLarge)

                Button(onClick = {
                    scope.launch {
                        playCurrentWord(session, recordings, queue) { message ->
                            state = LernmodusState.Error(message)
                        }
                    }
                }) {
                    Text("Wiederholen")
                }

                Button(onClick = {
                    val next = session.advance()
                    state = if (next == null) LernmodusState.Finished else LernmodusState.Running(next)
                }) {
                    Text("Weiter")
                }

                Button(onClick = {
                    queue.stop()
                    onBeenden()
                }) {
                    Text("Beenden")
                }
            }
        }
    }
}

/**
 * Loads and decodes the current word's recording, then hands it to [queue].
 * A missing recording or a corrupt WAV file fails here, synchronously, and
 * is reported via [onError]; a failure inside the sink itself (device
 * disconnected mid-playback) surfaces later through [PlaybackQueue]'s own
 * `onError` callback instead, since [PlaybackQueue.play] doesn't suspend.
 */
private suspend fun playCurrentWord(
    session: LearningSession,
    recordings: List<AudioRecording>,
    queue: PlaybackQueue,
    onError: (String) -> Unit,
) {
    val recording = recordings.firstOrNull { it.wordId == session.currentWord.id }
    if (recording == null) {
        onError("Keine Aufnahme für „${session.currentWord.text}“ gefunden.")
        return
    }
    try {
        val bytes = Res.readBytes("files/corpus/${recording.fileRef}")
        val buffer = WavFile.decode(bytes)
        queue.play(buffer)
    } catch (e: Exception) {
        onError("Wort konnte nicht geladen werden: ${e.message}")
    }
}
