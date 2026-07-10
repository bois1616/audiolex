package de.hexenwoche.audiolex.core.persistence

import de.hexenwoche.audiolex.core.srs.ReviewCard

/**
 * Persists [ReviewCard] fälligkeiten (Backlog M3, ADR-0004). Callers work
 * against this facade, not the DAO/Entity directly.
 */
interface ReviewCardRepository {
    suspend fun all(): List<ReviewCard>
    suspend fun save(card: ReviewCard)
}

class RoomReviewCardRepository(private val dao: ReviewCardDao) : ReviewCardRepository {
    override suspend fun all(): List<ReviewCard> = dao.all().map { it.toDomain() }
    override suspend fun save(card: ReviewCard) = dao.upsert(card.toEntity())
}

/**
 * Returns a [ReviewCard] for every [wordIds] entry, seeding a fresh,
 * immediately-due card (Szenario S10: Erststart) for any id that has no
 * stored card yet -- newly seeded cards are persisted right away so a
 * second load doesn't reseed them with a different due time.
 */
suspend fun ReviewCardRepository.allOrSeed(wordIds: List<String>): List<ReviewCard> {
    val existing = all().associateBy { it.wordId }
    val seeded = wordIds.filter { it !in existing }.map { ReviewCard(wordId = it, dueAtEpochMillis = 0L) }
    seeded.forEach { save(it) }
    return wordIds.map { existing[it] ?: seeded.first { s -> s.wordId == it } }
}
