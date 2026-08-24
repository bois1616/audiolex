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
import de.hexenwoche.audiolex.core.audio.OutputSetup
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.corpus.CorpusLanguage
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.corpus.LoadedCorpus
import de.hexenwoche.audiolex.core.i18n.Strings
import de.hexenwoche.audiolex.core.session.LearningSession
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import de.hexenwoche.audiolex.core.settings.ChannelMode
import de.hexenwoche.audiolex.core.settings.CorpusMode
import de.hexenwoche.audiolex.core.settings.entryKind

private sealed interface LernmodusState {
    data object Loading : LernmodusState
    /** [hint] is computed once, at load time (Backlog Eigen-Korpus Batch D, AC6) -- see [emptyCorpusHint]. */
    data class EmptyCorpus(val hint: String) : LernmodusState
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
 *
 * [channelMode] drives the channel selection (Backlog M4 "Kopfhörer-Bogen
 * Batch B", ADR-0011): applied via [applyChannelMode] in the same producer,
 * after the noise mix -- a no-op unless the live [rememberOutputSetup] result
 * is the stereo-headphone setup.
 *
 * [ownCorpusRepository] supplies the second corpus source (Backlog
 * Eigen-Korpus Batch C, ADR-0012): passed straight through to [loadCorpus]
 * and, for a recording that turns out to be
 * [de.hexenwoche.audiolex.core.corpus.RecordingSource.EIGEN], to
 * [readRecordingBytes] inside [playCurrentWord]. [excludedSpeakers] is the
 * Batch D contingent filter (ADR-0012 Nachtrag) that replaces Batch C's
 * `CorpusSource` -- applied inside [loadCorpus]/
 * [de.hexenwoche.audiolex.core.corpus.mergeCorpus], this screen only ever
 * passes it through.
 */
@Composable
fun LernmodusScreen(
    corpusMode: CorpusMode,
    corpusLanguage: CorpusLanguage,
    noiseEnabled: Boolean,
    snrDb: Int,
    noiseScenario: String,
    channelMode: ChannelMode,
    excludedSpeakers: Set<String>,
    ownCorpusRepository: OwnCorpusRepository,
    ownNoiseRepository: OwnNoiseRepository,
    onBeenden: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val sink = remember { createAudioSink() }
    val outputSetup = rememberOutputSetup()
    var state by remember { mutableStateOf<LernmodusState>(LernmodusState.Loading) }
    // `strings` is captured here rather than read at error time: the queue is
    // remembered for the life of the screen, and switching the language mid-
    // session is not reachable (the picker sits on the StartScreen). Keyed on
    // `strings` all the same, so the capture cannot go stale if that ever
    // changes.
    val queue = remember(strings) {
        PlaybackQueue(sink, scope, onError = { e ->
            state = LernmodusState.Error(strings.playbackFailed(e.message))
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
            val loaded = loadCorpus(corpusMode.entryKind(), ownCorpusRepository, excludedSpeakers, corpusLanguage)
            corpus = loaded
            // Loaded once per screen entry, not per word (AC6) -- a missing/
            // mismatched file or noise disabled all resolve to null, which
            // mixWithOptionalNoise treats as "play clean speech". The merged
            // catalog (bundled + own noises, Backlog M4 "Eigene
            // Störgeräusche", AC2) needs the noise repository to read an own
            // scenario's bytes.
            noiseBuffer = loadNoiseBuffer(noiseEnabled, noiseScenario, ownNoiseRepository)
            state = if (loaded.words.isEmpty()) {
                // AC6: only computed on this rare path, a second (unfiltered)
                // load to find out *why* it's empty -- excludedSpeakers vs.
                // availableSpeakers (Batch D) rather than the plain "kein
                // Wort im Korpus" text Batch C left behind.
                // One extra load, filtered by language but not by kind or
                // speaker: it answers both "is this drawer empty at all"
                // (ADR-0016) and "which speakers could this language offer"
                // (Batch D AC6). Only ever runs on this rare empty path.
                val inLanguage = loadCorpus(ownCorpusRepository = ownCorpusRepository, language = corpusLanguage)
                val availableSpeakers = inLanguage.recordings.map { it.voiceId }.toSet()
                LernmodusState.EmptyCorpus(
                    emptyCorpusHint(excludedSpeakers, availableSpeakers, inLanguage.words.isNotEmpty(), strings),
                )
            } else {
                // Shuffled once per session start, then fixed for the rest of
                // the session (Autor-Requirement 2026-07-12) -- so the word
                // order itself isn't what gets memorized, and "Vorheriges"
                // below steps back through a stable order.
                LernmodusState.Running(LearningSession(loaded.words.shuffled()))
            }
        } catch (e: Exception) {
            state = LernmodusState.Error(strings.corpusLoadFailed(e.message))
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
        playCurrentWord(
            running.session, loaded, queue, noiseBuffer, snrDb, channelMode, outputSetup, ownCorpusRepository, strings,
        ) { message ->
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
                Text(current.hint, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onBeenden) { Text(strings.backToStart) }
            }

            is LernmodusState.Error -> {
                Text(current.message, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onBeenden) { Text(strings.backToStart) }
            }

            is LernmodusState.Finished -> {
                Text(
                    strings.learningFinished,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Button(onClick = onBeenden) { Text(strings.backToStart) }
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
                    playCurrentWord(
                        session, loaded, queue, noiseBuffer, snrDb, channelMode, outputSetup, ownCorpusRepository,
                        strings,
                    ) { message ->
                        state = LernmodusState.Error(message)
                    }
                }) {
                    Text(strings.repeatPlayback)
                }

                FilledTonalButton(
                    enabled = !session.isFirstWord,
                    onClick = {
                        val previous = session.back() ?: return@FilledTonalButton
                        state = LernmodusState.Running(previous)
                    },
                ) {
                    Text(strings.previousEntry)
                }

                Button(onClick = {
                    val next = session.advance()
                    state = if (next == null) LernmodusState.Finished else LernmodusState.Running(next)
                }) {
                    Text(strings.nextEntry)
                }

                TextButton(onClick = {
                    queue.stop()
                    onBeenden()
                }) {
                    Text(
                        strings.quit,
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
    channelMode: ChannelMode,
    outputSetup: OutputSetup,
    ownCorpusRepository: OwnCorpusRepository,
    strings: Strings,
    onError: (String) -> Unit,
) {
    val recording = corpus.recordingFor(session.currentWord.id)
    if (recording == null) {
        onError(strings.noRecordingFound(session.currentWord.text))
        return
    }
    queue.play {
        val bytes = readRecordingBytes(recording, ownCorpusRepository, strings)
        val speech = WavFile.decode(bytes)
        val mixed = mixWithOptionalNoise(speech, noiseBuffer, snrDb)
        applyChannelMode(mixed, channelMode, outputSetup)
    }
}
