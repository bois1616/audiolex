package de.hexenwoche.audiolex.core.session

import de.hexenwoche.audiolex.core.persistence.SessionEntity
import de.hexenwoche.audiolex.core.srs.ReviewRating

/**
 * Aggregate record of one completed Prüfmodus session (Szenario S12),
 * Room-free like [de.hexenwoche.audiolex.core.srs.ReviewCard] -- persistence
 * mapping happens at the boundary via [toEntity]/[toDomain]. Aggregates
 * only, no per-card results (backlog scope decision 2026-07-10).
 */
data class Session(
    val startedAtEpochMillis: Long,
    val zoneId: String,
    val mode: String,
    val ratedCount: Int,
    val ratingCounts: Map<ReviewRating, Int>,
)

fun SessionEntity.toDomain(): Session = Session(
    startedAtEpochMillis = startedAtEpochMillis,
    zoneId = zoneId,
    mode = mode,
    ratedCount = ratedCount,
    ratingCounts = mapOf(
        ReviewRating.AGAIN to countAgain,
        ReviewRating.SOON to countSoon,
        ReviewRating.LATER to countLater,
        ReviewRating.GOOD to countGood,
        ReviewRating.PERFECT to countPerfect,
    ),
)

fun Session.toEntity(): SessionEntity = SessionEntity(
    startedAtEpochMillis = startedAtEpochMillis,
    zoneId = zoneId,
    mode = mode,
    ratedCount = ratedCount,
    countAgain = ratingCounts[ReviewRating.AGAIN] ?: 0,
    countSoon = ratingCounts[ReviewRating.SOON] ?: 0,
    countLater = ratingCounts[ReviewRating.LATER] ?: 0,
    countGood = ratingCounts[ReviewRating.GOOD] ?: 0,
    countPerfect = ratingCounts[ReviewRating.PERFECT] ?: 0,
)
