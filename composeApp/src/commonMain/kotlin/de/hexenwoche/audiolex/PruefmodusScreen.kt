package de.hexenwoche.audiolex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.createAudioSink
import de.hexenwoche.audiolex.core.corpus.AudioRecording
import de.hexenwoche.audiolex.core.corpus.Word
import de.hexenwoche.audiolex.core.persistence.ReviewCardRepository
import de.hexenwoche.audiolex.core.persistence.SessionRepository
import de.hexenwoche.audiolex.core.persistence.allOrSeed
import de.hexenwoche.audiolex.core.session.ExamSession
import de.hexenwoche.audiolex.core.session.PlaybackQueue
import de.hexenwoche.audiolex.core.session.Session
import de.hexenwoche.audiolex.core.srs.FixedIntervalScheduler
import de.hexenwoche.audiolex.core.srs.ReviewQueue
import de.hexenwoche.audiolex.core.srs.ReviewRating
import de.hexenwoche.audiolex.core.time.Clock
import de.hexenwoche.audiolex.generated.resources.Res
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private val scheduler = FixedIntervalScheduler()

private sealed interface PruefmodusState {
    data object Loading : PruefmodusState
    /**
     * No cards at all to show. Since a round tops up with not-yet-due cards
     * (Autor-Entscheid 2026-07-13), this now only happens when the corpus
     * itself is empty -- not merely when nothing is due.
     */
    data object EmptyCorpus : PruefmodusState
    /**
     * [rated] separates rating from advancing (Autor-Finding 2026-07-10):
     * once true, the screen stays on this revealed card showing "Nächstes"
     * instead of the [RatingBar] -- [ExamSession.advance] (not [ExamSession.rate])
     * is what actually moves to the next card, on an explicit tap.
     */
    data class Running(val session: ExamSession, val rated: Boolean = false) : PruefmodusState
    data class Finished(val ratedCount: Int) : PruefmodusState
    data class Error(val message: String) : PruefmodusState
}

/**
 * Prüfmodus (Szenarien S3, S4, S5, S7; Gestalt: DESIGN.md "Trainings-Screens
 * im Detail", Komponenten RevealCard/RatingBar). Fälligkeiten are persisted
 * via [repository] (ADR-0004): a corpus word without a stored card yet is
 * seeded as immediately due (Szenario S10, see [allOrSeed]), and every
 * rating is saved right away so it survives leaving the session (S5).
 * Playback goes exclusively through [PlaybackQueue], same as [LernmodusScreen].
 */
@Composable
fun PruefmodusScreen(
    repository: ReviewCardRepository,
    sessionRepository: SessionRepository,
    clock: Clock,
    onBeenden: () -> Unit,
    onZumLernmodus: () -> Unit,
) {
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
    var isRating by remember { mutableStateOf(false) }
    // Distribution and session start (Szenario S12, Sitzungshistorie) -- only
    // set once the session actually starts running, and only ever written
    // to, never used for SRS due-date math (that stays clock.nowEpochMillis()
    // read fresh at rate() time above).
    var startedAtEpochMillis by remember { mutableStateOf<Long?>(null) }
    val ratingCounts = remember { mutableStateOf(emptyMap<ReviewRating, Int>()) }
    var sessionLogged by remember { mutableStateOf(false) }
    // Bumped by "Neue Prüfrunde" on the Finished screen to re-run the load
    // effect below: it re-queries due cards (now including whatever the just-
    // finished round rescheduled as due again) and starts a fresh session,
    // without leaving the screen (Autor-Finding 2026-07-13).
    var reloadKey by remember { mutableStateOf(0) }

    suspend fun logSessionIfAnyRatings() {
        val startedAt = startedAtEpochMillis
        if (sessionLogged || startedAt == null || ratedCount == 0) return
        sessionLogged = true
        sessionRepository.save(
            Session(
                startedAtEpochMillis = startedAt,
                zoneId = clock.zoneId(),
                mode = "PRUEFMODUS",
                ratedCount = ratedCount,
                ratingCounts = ratingCounts.value,
            )
        )
    }

    DisposableEffect(Unit) {
        onDispose { queue.stop() }
    }

    LaunchedEffect(reloadKey) {
        try {
            // Reset the per-round session bookkeeping so a re-run (reloadKey
            // bumped) starts a clean session rather than carrying the last
            // round's counts into the new Sitzungshistorie entry.
            ratedCount = 0
            ratingCounts.value = emptyMap()
            startedAtEpochMillis = null
            sessionLogged = false

            val wordsJson = Res.readBytes("files/corpus/words.json").decodeToString()
            val recordingsJson = Res.readBytes("files/corpus/recordings.json").decodeToString()
            words = json.decodeFromString<List<Word>>(wordsJson)
            recordings = json.decodeFromString<List<AudioRecording>>(recordingsJson)

            val cards = repository.allOrSeed(words.map { it.id })
            // A round is up to 15 cards: due ones first, topped up with random
            // not-yet-due cards so there's always something to practise
            // (Autor-Entscheid 2026-07-13). Empty only when the corpus itself
            // has no cards -- then there's genuinely nothing to show.
            val round = ReviewQueue.roundOf(cards, clock.nowEpochMillis())
            state = if (round.isEmpty()) {
                PruefmodusState.EmptyCorpus
            } else {
                startedAtEpochMillis = clock.nowEpochMillis()
                PruefmodusState.Running(ExamSession(round))
            }
        } catch (e: Exception) {
            state = PruefmodusState.Error("Korpus konnte nicht geladen werden: ${e.message}")
        }
    }

    // Plays the current card's word once whenever the session moves to a new
    // (unrevealed) card -- heard-only until reveal (S3). Keyed on the card's
    // own word id, not on `state` as a whole -- keying on `state` re-fired
    // this effect on every structural change to it, including reveal()
    // (which produces a new Running(revealed = true) state), replaying the
    // word just from tapping the card to uncover it.
    val runningCardWordId = (state as? PruefmodusState.Running)?.session?.currentCard?.wordId
    LaunchedEffect(runningCardWordId) {
        val running = state as? PruefmodusState.Running ?: return@LaunchedEffect
        if (words.isEmpty() || recordings.isEmpty()) return@LaunchedEffect
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

            is PruefmodusState.EmptyCorpus -> {
                Text("Kein Wort im Korpus.", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Es gibt noch keine Karten zum Üben.",
                    style = MaterialTheme.typography.bodyLarge,
                )
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
                // Re-queries due cards and starts a fresh session without
                // leaving the screen (Autor-Finding 2026-07-13). The just-
                // finished session is already logged (logSessionIfAnyRatings
                // ran on the transition into Finished), so bumping reloadKey
                // resets the per-round counters cleanly for the new round.
                Button(onClick = { reloadKey += 1 }) { Text("Neue Prüfrunde") }
                Button(onClick = onBeenden) { Text("Zurück zum Start") }
            }

            is PruefmodusState.Running -> {
                val session = current.session
                Text(
                    "${session.progress} / ${session.total}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                // Only meaningful before reveal (replay the still-hidden
                // word) -- once rated, "Nächstes" below is the way forward
                // instead (Autor-Finding 2026-07-10: "Wiederholen" made no
                // sense once the word was already visible).
                if (!current.rated) {
                    Button(onClick = {
                        playCurrentCard(session, words, recordings, queue) { message ->
                            state = PruefmodusState.Error(message)
                        }
                    }) {
                        Text("Wiederholen")
                    }
                }

                if (session.revealed && !current.rated) {
                    RatingBar(
                        enabled = !isRating,
                        onRate = { rating ->
                            // Locks synchronously on the first tap (before
                            // the launch below even starts), so a fast
                            // double-tap can't trigger rate()/save() twice
                            // for the same card while the state switch is
                            // still pending.
                            isRating = true
                            val result = session.rate(rating, clock.nowEpochMillis(), scheduler)
                            scope.launch {
                                // Persisted before the state switch (Szenario
                                // S5: an already-submitted rating must
                                // survive leaving the session, not just live
                                // in memory).
                                repository.save(result.ratedCard)
                                ratedCount += 1
                                ratingCounts.value = ratingCounts.value.toMutableMap().apply {
                                    this[rating] = (this[rating] ?: 0) + 1
                                }
                                isRating = false
                                // Stays on this revealed, now-rated card --
                                // advancing is a separate, explicit action via
                                // "Nächstes" below, not an automatic jump.
                                state = PruefmodusState.Running(session, rated = true)
                            }
                        },
                    )
                }

                if (current.rated) {
                    Button(onClick = {
                        val next = session.advance()
                        if (next == null) {
                            scope.launch {
                                logSessionIfAnyRatings()
                                state = PruefmodusState.Finished(ratedCount)
                            }
                        } else {
                            state = PruefmodusState.Running(next)
                        }
                    }) {
                        Text("Nächstes")
                    }
                }

                Button(onClick = {
                    queue.stop()
                    scope.launch {
                        // Szenario S5: leaving mid-session still counts as a
                        // completed session for the history, provided at
                        // least one card was actually rated.
                        logSessionIfAnyRatings()
                        onBeenden()
                    }
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
 *
 * The revealed word stays on one line and shrinks to fit (DESIGN.md: the
 * word is "groß und ruhig ... positionsstabil" -- it must not wrap or move,
 * only scale down for longer words). The "tap to reveal" hint is secondary
 * text, not the target word, so it's set smaller instead of competing with
 * it at displayLarge (DESIGN.md: secondary content recedes).
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
            if (revealed) {
                // Explicitly neutral (onSurface), never the accent color --
                // the accent is reserved for active elements (DESIGN.md).
                val displayLarge = MaterialTheme.typography.displayLarge
                BasicText(
                    text = text ?: "?",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    style = displayLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 24.sp,
                        maxFontSize = displayLarge.fontSize,
                    ),
                )
            } else {
                Text("Antippen zum Aufdecken", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Five equally-weighted rating buttons with interval hints (SOUL.md/DESIGN.md:
 * the scale steers repetition, it doesn't grade -- no traffic-light colors).
 * FlowRow instead of a fixed Row so the buttons wrap onto a second line on
 * narrow phone widths (A53) instead of getting squeezed vertically.
 */
@Composable
private fun RatingBar(enabled: Boolean, onRate: (ReviewRating) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (rating in ReviewRating.entries) {
            Button(enabled = enabled, onClick = { onRate(rating) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(germanRatingLabel(rating))
                    Text(germanIntervalHint(rating), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

internal fun germanRatingLabel(rating: ReviewRating): String = when (rating) {
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

private fun playCurrentCard(
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
    // Decode inside the queue producer so a fast double-tap on "Wiederholen"
    // cancels the previous decode+play atomically instead of racing two
    // AudioTracks (same fix as Lernmodus, Autor-Finding 2026-07-13).
    queue.play {
        val bytes = Res.readBytes("files/corpus/${recording.fileRef}")
        WavFile.decode(bytes)
    }
}
