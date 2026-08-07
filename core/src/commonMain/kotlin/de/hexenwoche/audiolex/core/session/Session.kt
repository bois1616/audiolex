package de.hexenwoche.audiolex.core.session

import de.hexenwoche.audiolex.core.persistence.SessionEntity
import de.hexenwoche.audiolex.core.srs.ReviewRating
import kotlinx.serialization.Serializable

/**
 * Aggregate record of one completed Prüfmodus session (Szenario S12),
 * Room-free like [de.hexenwoche.audiolex.core.srs.ReviewCard] -- persistence
 * mapping happens at the boundary via [toEntity]/[toDomain]. Aggregates
 * only, no per-card results (backlog scope decision 2026-07-10).
 *
 * `@Serializable` because this is also the backup's wire format (ADR-0013
 * Nachtrag 2026-08-07), same as [de.hexenwoche.audiolex.core.corpus.OwnEntry]
 * is for the own corpus. Deliberately *this* type rather than the Room
 * entity: [SessionEntity.id] is `autoGenerate` and therefore device-local,
 * which makes it useless as an identity across devices -- the backup merges
 * on [startedAtEpochMillis] instead (two sessions of one user don't start in
 * the same millisecond).
 */
@Serializable
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
