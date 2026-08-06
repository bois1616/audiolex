package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
import de.hexenwoche.audiolex.core.audio.NoiseLoop
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.corpus.LoadedCorpus
import de.hexenwoche.audiolex.core.session.LearningSession
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import de.hexenwoche.audiolex.core.settings.CorpusMode
import de.hexenwoche.audiolex.core.settings.entryKind
import de.hexenwoche.audiolex.generated.resources.Res

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
 *
 * [noiseEnabled]/[snrDb]/[noiseScenario] drive the noise overlay (Backlog M4
 * "Störgeräusch-Overlay", ADR-0010): mixed into the speech signal inside the
 * [PlaybackQueue] producer, after WAV decode, so both training modes get the
 * same treatment through the same mixer. The noise loop is loaded once in
 * the load block below (not per word) and cached in [noiseBuffer].
 */
@Composable
fun LernmodusScreen(
    corpusMode: CorpusMode,
    noiseEnabled: Boolean,
    snrDb: Int,
    noiseScenario: String,
    onBeenden: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sink = remember { createAudioSink() }
    var state by remember { mutableStateOf<LernmodusState>(LernmodusState.Loading) }
    val queue = remember {
        PlaybackQueue(sink, scope, onError = { e ->
            state = LernmodusState.Error("Wiedergabe fehlgeschlagen: ${e.message}")
        })
    }
    var corpus by remember { mutableStateOf<LoadedCorpus?>(null) }
    var noiseBuffer by remember { mutableStateOf<NoiseLoop?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            queue.stop()
            sink.close()
        }
    }

    LaunchedEffect(Unit) {
        try {
            // Only entries matching the current corpus mode enter the
            // session (Satz-Bogen Batch B, AC3) -- an empty filtered corpus
            // falls through to the existing EmptyCorpus state below.
            // Loading lives in loadCorpus/parseCorpus (Backlog
            // "Code-Qualität"), shared with Prüf- and Dev-Screen.
            val loaded = loadCorpus(corpusMode.entryKind())
            corpus = loaded
            // Loaded once per screen entry, not per word (AC6) -- a missing/
            // mismatched file or noise disabled all resolve to null, which
            // mixWithOptionalNoise treats as "play clean speech".
            noiseBuffer = loadNoiseBuffer(noiseEnabled, noiseScenario)
            state = if (loaded.words.isEmpty()) {
                LernmodusState.EmptyCorpus
            } else {
                // Shuffled once per session start, then fixed for the rest of
                // the session (Autor-Requirement 2026-07-12) -- so the word
                // order itself isn't what gets memorized, and "Vorheriges"
                // below steps back through a stable order.
                LernmodusState.Running(LearningSession(loaded.words.shuffled()))
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
        val loaded = corpus ?: return@LaunchedEffect
        playCurrentWord(running.session, loaded, queue, noiseBuffer, snrDb) { message ->
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
                // Fixed 280x160dp card, unified look with the Prüfmodus
                // RevealCard (Backlog M2 "Lernmodus: Zieltext in festem
                // Kartenrahmen analog Prüfmodus") -- no reveal/click here,
                // the text is always visible. The text metric itself (word
                // shrink-to-fit vs. sentence 3-line wrap + proportional
                // lineHeight) lives in [TargetTextCard], shared with
                // RevealCard's revealed state so it can't drift between the
                // two modes.
                val isSentence = session.currentWord.kind == EntryKind.SENTENCE
                TargetTextCard(text = session.currentWord.text, isSentence = isSentence)

                // Pushes the action buttons to the bottom of the screen, into
                // thumb reach on a one-handed phone grip (DESIGN.md Leitprinzip
                // 4 "Große Ziele, eine Hand"; Muster StartScreen) -- progress
                // line and card stay put above.
                Spacer(modifier = Modifier.weight(1f))

                // Hierarchy (DESIGN.md "Farbe trägt Bedeutung"): "Weiter" is
                // the primary action and stays the filled Button; "Wiederholen"
                // is secondary (FilledTonalButton, same choice as Prüfmodus --
                // an OutlinedButton read too close to the disabled state on
                // device, A53-Befund 2026-08-06); and "Beenden" recedes as a
                // TextButton in onSurfaceVariant, the same muted pattern the
                // StartScreen already uses for "App beenden".
                FilledTonalButton(onClick = {
                    val loaded = corpus ?: return@FilledTonalButton
                    playCurrentWord(session, loaded, queue, noiseBuffer, snrDb) { message ->
                        state = LernmodusState.Error(message)
                    }
                }) {
                    Text("Wiederholen")
                }

                FilledTonalButton(
                    enabled = !session.isFirstWord,
                    onClick = {
                        val previous = session.back() ?: return@FilledTonalButton
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

                TextButton(onClick = {
                    queue.stop()
                    onBeenden()
                }) {
                    Text(
                        "Beenden",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
    corpus: LoadedCorpus,
    queue: PlaybackQueue,
    noiseBuffer: NoiseLoop?,
    snrDb: Int,
    onError: (String) -> Unit,
) {
    val recording = corpus.recordingFor(session.currentWord.id)
    if (recording == null) {
        onError("Keine Aufnahme für „${session.currentWord.text}“ gefunden.")
        return
    }
    queue.play {
        val bytes = Res.readBytes("files/corpus/${recording.fileRef}")
        val speech = WavFile.decode(bytes)
        mixWithOptionalNoise(speech, noiseBuffer, snrDb)
    }
}
