package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import de.hexenwoche.audiolex.core.audio.OutputSetup
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import de.hexenwoche.audiolex.core.audio.StereoGain
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.concatWithGaps
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.audio.perEarStereo
import de.hexenwoche.audiolex.core.corpus.AudioRecording
import de.hexenwoche.audiolex.core.corpus.CorpusLanguage
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.corpus.LoadedCorpus
import de.hexenwoche.audiolex.core.i18n.Strings
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import de.hexenwoche.audiolex.core.settings.ChannelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The channel test: plays three words into one ear and three *different*
 * words into the other, simultaneously, with the channel selection applied
 * on top. Reachable by a long press on the version line (Backlog M4
 * "Kanaltest aus dem Regelbetrieb ausblenden, aber erreichbar halten").
 *
 * It answers one question and deliberately no others: **does the app put
 * sound where it says it does?** With `Nur links` the right channel carries
 * exact zeroes ([perEarStereo], unit-tested in `MixerTest`), so anything
 * still audible on the right is downstream of the app -- the device, the
 * headset, or a system effect. That makes it the falsifiable instrument for
 * every channel report, including the one from the F-Droid tester on
 * 2026-08-27 that this batch answers.
 *
 * Two things changed for that report (Autor-Auftrag 2026-08-27):
 * - **Three words per ear, not one.** A single word is over in half a
 *   second, which is too short to be sure *where* it came from when that is
 *   the very thing in question.
 * - **Both languages.** The screen used to be German-only on the grounds
 *   that an instrument is not a product screen (ADR-0015 Nicht-Ziele). That
 *   held while the only person holding the instrument read German; it stops
 *   holding the moment a test report comes back in English. See the Nachtrag
 *   in ADR-0015.
 *
 * Words come from the corpus language in the settings (ADR-0016), not from
 * the UI language: those are two questions, and the training drawer is the
 * one that decides which recordings exist.
 *
 * Playback goes through [PlaybackQueue], same as [LernmodusScreen]/
 * [PruefmodusScreen] (Backlog M4 "Dev-Kanaltest: überlappende Wiedergaben",
 * A53-Befund 2026-08-06): decode happens *inside* the queue's producer for
 * both playback paths, so a fast repeat tap cancels the previous playback
 * whole instead of stacking two overlapping `AudioTrack`s.
 */
@Composable
fun DevPlaybackScreen(ownCorpusRepository: OwnCorpusRepository, corpusLanguage: CorpusLanguage) {
    val strings = LocalStrings.current
    var corpus by remember { mutableStateOf<LoadedCorpus?>(null) }
    var status by remember { mutableStateOf(strings.channelTestLoading) }
    val scope = rememberCoroutineScope()
    val sink = remember { createAudioSink() }
    // Keyed on `strings` like the training screens: the capture cannot go
    // stale if switching the language mid-screen ever becomes reachable.
    val queue = remember(strings) {
        PlaybackQueue(sink, scope, onError = { e ->
            status = strings.playbackFailed(e.message)
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

    LaunchedEffect(corpusLanguage) {
        corpus = null
        status = strings.channelTestLoading
        // Words only, and only the chosen drawer: the sentences are seconds
        // long each, which would make a six-part sequence a paragraph.
        corpus = loadCorpus(EntryKind.WORD, ownCorpusRepository, language = corpusLanguage)
        status = strings.channelTestReady
    }

    Text(strings.channelTestTitle, style = MaterialTheme.typography.titleMedium)
    Text(strings.channelTestExplainer, style = MaterialTheme.typography.bodyMedium)

    // What the detection actually sees, device types included (chivalry,
    // 2026-08-27). The settings screen shows only the verdict, which is the
    // right amount there; here it is a bug report waiting to be screenshotted,
    // so the raw types come along. The test below pans regardless of this
    // line -- that independence is what makes the pair of them diagnostic:
    // separation here plus "Hörgerät erkannt" above means the audio path is
    // fine and the *classification* is wrong.
    val diagnosis = rememberOutputDiagnosis()
    Text(
        strings.channelTestOutput(diagnosis.setup, diagnosis.routedDevices),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (diagnosis.setup != OutputSetup.STEREO_KOPFHOERER) {
        Text(
            strings.channelTestSettingInactive,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Text(status, style = MaterialTheme.typography.bodyMedium)

    val currentCorpus = corpus
    if (currentCorpus == null) {
        CircularProgressIndicator()
        return
    }

    // Only words that actually have a recording -- an entry without one would
    // silently shorten one ear's sequence and make the test lie about what it
    // played.
    val playable = currentCorpus.words
        .mapNotNull { word -> currentCorpus.recordingFor(word.id)?.let { word.text to it } }
    val leftEar = playable.take(WORDS_PER_EAR)
    val rightEar = playable.drop(WORDS_PER_EAR).take(WORDS_PER_EAR)
    if (rightEar.size < WORDS_PER_EAR) {
        Text(strings.channelTestTooFewWords, style = MaterialTheme.typography.bodyMedium)
        return
    }

    Text(strings.channelTestLeftEar(leftEar.map { it.first }), style = MaterialTheme.typography.bodyMedium)
    Text(strings.channelTestRightEar(rightEar.map { it.first }), style = MaterialTheme.typography.bodyMedium)

    // FlowRow, not Row: three labels don't fit the A53's width in one line
    // (A53-Befund 2026-08-06, same overflow and same remedy as the speaker
    // chips). The labels are the ones the settings screen uses for the same
    // three choices -- a tester comparing "Nur links" here against "Nur
    // links" there should not have to translate between two wordings.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Left, both, right -- the ears' own order, not the enum's. The
        // settings screen lists "Beide" first because that is the default
        // there; here the row is a picture of the thing being tested.
        for (mode in listOf(ChannelMode.NUR_LINKS, ChannelMode.BEIDE, ChannelMode.NUR_RECHTS)) {
            val label = strings.channelModeLabel(mode)
            Button(
                onClick = {
                    status = strings.channelTestPlaying(label)
                    // Decode + mix happen inside the producer: a fast
                    // follow-up tap cancels this job, decode included,
                    // instead of two decodes racing to the sink.
                    queue.play {
                        buildWordsPerEar(
                            leftEar.map { it.second },
                            rightEar.map { it.second },
                            mode.stereoGain(),
                            ownCorpusRepository,
                            strings,
                        )
                    }
                },
            ) {
                Text(label)
            }
        }
    }

    // The six words on their own, mono, no panning: hearing one by itself is
    // the cross-check when a channel sounds wrong (AC2 of "Kanaltest aus dem
    // Regelbetrieb ausblenden" -- the screen is the channel tool, not a
    // corpus browser, so it lists exactly what the test above uses).
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(leftEar + rightEar) { (text, recording) ->
            Button(
                onClick = {
                    status = strings.channelTestPlayingWord(text, recording.voiceId)
                    queue.play { decodeRecording(recording, ownCorpusRepository, strings) }
                },
            ) {
                Text(text)
            }
        }
    }
}

/** Words per ear -- three is long enough to localize, short enough to stay one gesture. */
private const val WORDS_PER_EAR = 3

/** Silence between the words of a sequence, so three words stay three words. */
private const val WORD_GAP_MILLIS = 350

private suspend fun decodeRecording(
    recording: AudioRecording,
    ownCorpusRepository: OwnCorpusRepository,
    strings: Strings,
): PcmBuffer =
    withContext(Dispatchers.Default) {
        val bytes = readRecordingBytes(recording, ownCorpusRepository, strings)
        WavFile.decode(bytes)
    }

/**
 * Decodes both word sequences, joins each into one mono run, and hands them
 * to [perEarStereo] -- left sequence to the left ear, right sequence to the
 * right, [gain] on top. Only builds the buffer; playing it is the queue's
 * job. The arithmetic itself lives in `:core` and is unit-tested there.
 */
private suspend fun buildWordsPerEar(
    leftRecordings: List<AudioRecording>,
    rightRecordings: List<AudioRecording>,
    gain: StereoGain,
    ownCorpusRepository: OwnCorpusRepository,
    strings: Strings,
): PcmBuffer =
    withContext(Dispatchers.Default) {
        suspend fun sequence(recordings: List<AudioRecording>): PcmBuffer =
            concatWithGaps(
                recordings.map { WavFile.decode(readRecordingBytes(it, ownCorpusRepository, strings)) },
                WORD_GAP_MILLIS,
            )

        perEarStereo(sequence(leftRecordings), sequence(rightRecordings), gain)
    }
