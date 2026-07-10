package de.hexenwoche.audiolex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import de.hexenwoche.audiolex.core.session.ExamSession
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import de.hexenwoche.audiolex.core.srs.FixedIntervalScheduler
import de.hexenwoche.audiolex.core.srs.ReviewCard
import de.hexenwoche.audiolex.core.srs.ReviewQueue
import de.hexenwoche.audiolex.core.srs.ReviewRating
import de.hexenwoche.audiolex.generated.resources.Res
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private val scheduler = FixedIntervalScheduler()

private sealed interface PruefmodusState {
    data object Loading : PruefmodusState
    data class NothingDue(val nextDueAtEpochMillis: Long?) : PruefmodusState
    data class Running(val session: ExamSession) : PruefmodusState
    data class Finished(val ratedCount: Int) : PruefmodusState
    data class Error(val message: String) : PruefmodusState
}

/**
 * Prüfmodus (Szenarien S3, S4, S5, S7; Gestalt: DESIGN.md "Trainings-Screens
 * im Detail", Komponenten RevealCard/RatingBar). Cards are in-memory only in
 * this iteration -- no persistence yet, every corpus word is treated as
 * immediately due (`dueAtEpochMillis = 0`) so the session is playable before
 * the SRS persistence item lands. Playback goes exclusively through
 * [PlaybackQueue], same as [LernmodusScreen].
 */
@Composable
fun PruefmodusScreen(onBeenden: () -> Unit, onZumLernmodus: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sink = remember { createAudioSink() }
    var state by remember { mutableStateOf<PruefmodusState>(PruefmodusState.Loading) }
    val queue = remember {
        PlaybackQueue(sink, scope, onError = { e ->
            state = PruefmodusState.Error("Wiedergabe fehlgeschlagen: ${e.message}")
        })
    }
    var words by remember { mutableStateOf<List<Word>>(emptyList()) }
    var recordings by remember { mutableStateOf<List<AudioRecording>>(emptyList()) }
    var ratedCount by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        onDispose { queue.stop() }
    }

    LaunchedEffect(Unit) {
        try {
            val wordsJson = Res.readBytes("files/corpus/words.json").decodeToString()
            val recordingsJson = Res.readBytes("files/corpus/recordings.json").decodeToString()
            words = json.decodeFromString<List<Word>>(wordsJson)
            recordings = json.decodeFromString<List<AudioRecording>>(recordingsJson)

            // No persistence yet (separate backlog item): every word starts
            // immediately due so the session is playable end-to-end.
            val cards = words.map { ReviewCard(wordId = it.id, dueAtEpochMillis = 0L) }
            val due = ReviewQueue.due(cards, nowEpochMillis = 0L)
            state = if (due.isEmpty()) {
                PruefmodusState.NothingDue(cards.minOfOrNull { it.dueAtEpochMillis })
            } else {
                PruefmodusState.Running(ExamSession(due))
            }
        } catch (e: Exception) {
            state = PruefmodusState.Error("Korpus konnte nicht geladen werden: ${e.message}")
        }
    }

    // Plays the current card's word once whenever the session moves to a new
    // (unrevealed) card -- heard-only until reveal (S3).
    LaunchedEffect(state) {
        val running = state as? PruefmodusState.Running ?: return@LaunchedEffect
        playCurrentCard(running.session, words, recordings, queue) { message ->
            state = PruefmodusState.Error(message)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val current = state) {
            is PruefmodusState.Loading -> CircularProgressIndicator()

            is PruefmodusState.NothingDue -> {
                Text("Nichts fällig.", style = MaterialTheme.typography.headlineSmall)
                Text(describeNextDue(current.nextDueAtEpochMillis), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onZumLernmodus) { Text("Stattdessen Lernmodus") }
                Button(onClick = onBeenden) { Text("Zurück zum Start") }
            }

            is PruefmodusState.Error -> {
                Text(current.message, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onBeenden) { Text("Zurück zum Start") }
            }

            is PruefmodusState.Finished -> {
                Text("Fertig!", style = MaterialTheme.typography.headlineSmall)
                Text("${current.ratedCount} Karten bewertet.", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onBeenden) { Text("Zurück zum Start") }
            }

            is PruefmodusState.Running -> {
                val session = current.session
                Text(
                    "${session.progress} / ${session.total}",
                    style = MaterialTheme.typography.bodyMedium,
                )

                val word = words.firstOrNull { it.id == session.currentCard.wordId }
                RevealCard(
                    text = word?.text,
                    revealed = session.revealed,
                    onClick = {
                        if (!session.revealed) {
                            state = PruefmodusState.Running(session.reveal())
                        }
                    },
                )

                Button(onClick = {
                    scope.launch {
                        playCurrentCard(session, words, recordings, queue) { message ->
                            state = PruefmodusState.Error(message)
                        }
                    }
                }) {
                    Text("Wiederholen")
                }

                if (session.revealed) {
                    RatingBar(onRate = { rating ->
                        val result = session.rate(rating, nowEpochMillis = 0L, scheduler)
                        val nextSession = result.nextSession
                        ratedCount += 1
                        state = if (nextSession == null) {
                            PruefmodusState.Finished(ratedCount)
                        } else {
                            PruefmodusState.Running(nextSession)
                        }
                    })
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
 * Constant-size card whose silhouette doesn't give away the word's length
 * (DESIGN.md): shows nothing until [revealed], then the word. A large tap
 * area doubles as the reveal gesture.
 */
@Composable
private fun RevealCard(text: String?, revealed: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(280.dp).height(160.dp).clickable(enabled = !revealed, onClick = onClick),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (revealed) (text ?: "?") else "Antippen zum Aufdecken",
                style = MaterialTheme.typography.displayLarge,
            )
        }
    }
}

/**
 * Five equally-weighted rating buttons with interval hints (SOUL.md/DESIGN.md:
 * the scale steers repetition, it doesn't grade -- no traffic-light colors).
 */
@Composable
private fun RatingBar(onRate: (ReviewRating) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (rating in ReviewRating.entries) {
            Button(onClick = { onRate(rating) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(germanRatingLabel(rating))
                    Text(germanIntervalHint(rating), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun germanRatingLabel(rating: ReviewRating): String = when (rating) {
    ReviewRating.AGAIN -> "Sofort"
    ReviewRating.SOON -> "Bald"
    ReviewRating.LATER -> "Später"
    ReviewRating.GOOD -> "Gut"
    ReviewRating.PERFECT -> "Perfekt"
}

private fun germanIntervalHint(rating: ReviewRating): String = when (rating) {
    ReviewRating.AGAIN -> "1 min"
    ReviewRating.SOON -> "10 min"
    ReviewRating.LATER -> "1 Tag"
    ReviewRating.GOOD -> "1 Woche"
    ReviewRating.PERFECT -> "1 Monat"
}

private fun describeNextDue(nextDueAtEpochMillis: Long?): String {
    if (nextDueAtEpochMillis == null) return "Kein Wort im Korpus vorhanden."
    val remainingMillis = nextDueAtEpochMillis
    if (remainingMillis <= 0) return "Die nächste Karte ist bereits fällig."
    val minutes = remainingMillis / 60_000
    return when {
        minutes < 1 -> "Nächste Karte in weniger als einer Minute fällig."
        minutes < 60 -> "Nächste Karte in $minutes Minute(n) fällig."
        minutes < 24 * 60 -> "Nächste Karte in ${minutes / 60} Stunde(n) fällig."
        else -> "Nächste Karte in ${minutes / (24 * 60)} Tag(en) fällig."
    }
}

private suspend fun playCurrentCard(
    session: ExamSession,
    words: List<Word>,
    recordings: List<AudioRecording>,
    queue: PlaybackQueue,
    onError: (String) -> Unit,
) {
    val word = words.firstOrNull { it.id == session.currentCard.wordId }
    if (word == null) {
        onError("Wort zu Karte „${session.currentCard.wordId}“ nicht gefunden.")
        return
    }
    val recording = recordings.firstOrNull { it.wordId == word.id }
    if (recording == null) {
        onError("Keine Aufnahme für „${word.text}“ gefunden.")
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
