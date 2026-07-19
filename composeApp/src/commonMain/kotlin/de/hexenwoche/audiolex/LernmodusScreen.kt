package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.corpus.AudioRecording
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.corpus.Word
import de.hexenwoche.audiolex.core.session.LearningSession
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import de.hexenwoche.audiolex.core.settings.CorpusMode
import de.hexenwoche.audiolex.core.settings.entryKind
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
 *
 * [corpusMode] selects which corpus entries the session runs on (Backlog M2
 * Satz-Bogen Batch B, ADR-0009): words only, or sentences only.
 */
@Composable
fun LernmodusScreen(corpusMode: CorpusMode, onBeenden: () -> Unit) {
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
            // Only entries matching the current corpus mode enter the
            // session (Satz-Bogen Batch B, AC3) -- an empty filtered corpus
            // falls through to the existing EmptyCorpus state below.
            val words = json.decodeFromString<List<Word>>(wordsJson)
                .filter { it.kind == corpusMode.entryKind() }
            recordings = json.decodeFromString<List<AudioRecording>>(recordingsJson)
            state = if (words.isEmpty()) {
                LernmodusState.EmptyCorpus
            } else {
                // Shuffled once per session start, then fixed for the rest of
                // the session (Autor-Requirement 2026-07-12) -- so the word
                // order itself isn't what gets memorized, and "Vorheriges"
                // below steps back through a stable order.
                LernmodusState.Running(LearningSession(words.shuffled()))
            }
        } catch (e: Exception) {
            state = LernmodusState.Error("Korpus konnte nicht geladen werden: ${e.message}")
        }
    }

    // Plays the current word once whenever the session moves to a new word
    // (S1: happy path). Repeats (S2) are triggered explicitly by the button.
    // Keyed on the word's own index, not on `state` as a whole -- keying on
    // `state` re-triggered this effect for every structural change to it
    // (e.g. the two state writes LaunchedEffect(Unit) above makes while
    // loading), which played the first word twice.
    val runningWordIndex = (state as? LernmodusState.Running)?.session?.currentIndex
    LaunchedEffect(runningWordIndex) {
        val running = state as? LernmodusState.Running ?: return@LaunchedEffect
        if (recordings.isEmpty()) return@LaunchedEffect
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
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Words keep the exact pre-Batch-B behaviour: one line,
                // shrink-to-fit instead of wrapping or clipping (DESIGN.md:
                // the target word is "groß und ruhig ... positionsstabil" --
                // position and line count must not change, only the font
                // size for overlength). Sentences may wrap up to three lines
                // within the same stable area -- a whole sentence on one
                // shrink-to-fit line would be illegibly small (Satz-Bogen
                // Batch B, AC5; the shared 24sp minimum keeps both kinds
                // grounded at the same floor).
                // Explicitly neutral (onSurface), never the accent color --
                // the accent is reserved for active elements (DESIGN.md).
                val displayLarge = MaterialTheme.typography.displayLarge
                BasicText(
                    text = session.currentWord.text,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    style = displayLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = if (session.currentWord.kind == EntryKind.SENTENCE) 3 else 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 24.sp,
                        maxFontSize = displayLarge.fontSize,
                    ),
                )

                Button(onClick = {
                    playCurrentWord(session, recordings, queue) { message ->
                        state = LernmodusState.Error(message)
                    }
                }) {
                    Text("Wiederholen")
                }

                Button(
                    enabled = !session.isFirstWord,
                    onClick = {
                        val previous = session.back() ?: return@Button
                        state = LernmodusState.Running(previous)
                    },
                ) {
                    Text("Vorheriges")
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
 * Enqueues the current word's recording for playback. The missing-recording
 * check is synchronous (not a race). The actual file read + WAV decode runs
 * *inside* the [PlaybackQueue] producer, so a fast double-tap on "Wiederholen"
 * cancels the previous decode+play atomically instead of racing two
 * AudioTracks to the sink (Autor-Finding 2026-07-13, "Kaffee" -> "Kakaffee").
 * A missing recording, a corrupt WAV, or a sink failure all surface via
 * [onError] -- decode/read errors through the queue's own onError path.
 */
private fun playCurrentWord(
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
    queue.play {
        val bytes = Res.readBytes("files/corpus/${recording.fileRef}")
        WavFile.decode(bytes)
    }
}
