package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.hexenwoche.audiolex.core.persistence.SessionRepository
import de.hexenwoche.audiolex.core.session.Session
import de.hexenwoche.audiolex.core.srs.ReviewRating

private sealed interface SitzungshistorieState {
    data object Loading : SitzungshistorieState
    data class Loaded(val sessions: List<Session>) : SitzungshistorieState
}

/**
 * Sitzungshistorie (Szenario S12): abgeschlossene Prüfmodus-Sitzungen als
 * Liste, neueste zuerst. Aggregate only, no per-card drilldown (backlog
 * scope decision) -- a dead end with "Zurück", same navigation pattern as
 * the other screens.
 */
@Composable
fun SitzungshistorieScreen(repository: SessionRepository, onBeenden: () -> Unit) {
    var state by remember { mutableStateOf<SitzungshistorieState>(SitzungshistorieState.Loading) }

    LaunchedEffect(Unit) {
        state = SitzungshistorieState.Loaded(repository.all())
    }

    // Title and "Zurück" stay pinned; only the session list between them
    // scrolls. Without this the list grew past the screen and pushed the
    // "Zurück" button out of view -- a dead end that forced an app restart
    // (Autor-Finding 2026-07-13, A53-Gerätetest).
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sitzungshistorie", style = MaterialTheme.typography.headlineLarge)

        when (val current = state) {
            is SitzungshistorieState.Loading -> CircularProgressIndicator()

            is SitzungshistorieState.Loaded -> {
                if (current.sessions.isEmpty()) {
                    Text("Noch keine Sitzungen.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Muted divider between entries, none after the last
                        // one (Backlog P3 "Sitzungshistorie: Trennung
                        // zwischen den Einträgen") -- the three text lines
                        // per session used to blur into each other once the
                        // list got long.
                        current.sessions.forEachIndexed { index, session ->
                            SessionRow(session)
                            if (index < current.sessions.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        Button(onClick = onBeenden) {
            Text("Zurück")
        }
    }
}

@Composable
private fun SessionRow(session: Session) {
    Column {
        Text(
            formatTimestamp(session.startedAtEpochMillis, session.zoneId),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(germanModeLabel(session.mode), style = MaterialTheme.typography.bodyMedium)
        Text(
            "${session.ratedCount} Karten bewertet — ${describeRatingCounts(session.ratingCounts)}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun germanModeLabel(mode: String): String = when (mode) {
    "PRUEFMODUS" -> "Prüfmodus"
    else -> mode
}

private fun describeRatingCounts(ratingCounts: Map<ReviewRating, Int>): String =
    ReviewRating.entries
        .mapNotNull { rating -> ratingCounts[rating]?.takeIf { it > 0 }?.let { "${germanRatingLabel(rating)} ×$it" } }
        .ifEmpty { listOf("keine Bewertungen") }
        .joinToString(", ")
