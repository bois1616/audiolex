package de.hexenwoche.audiolex.core.persistence

import de.hexenwoche.audiolex.core.srs.ReviewCard

/**
 * Persists [ReviewCard] fälligkeiten (Backlog M3, ADR-0004). Callers work
 * against this facade, not the DAO/Entity directly.
 */
interface ReviewCardRepository {
    suspend fun all(): List<ReviewCard>
    suspend fun save(card: ReviewCard)

    /** Batch variant of [save], one transaction instead of n round-trips. */
    suspend fun saveAll(cards: List<ReviewCard>)
}

class RoomReviewCardRepository(private val dao: ReviewCardDao) : ReviewCardRepository {
    override suspend fun all(): List<ReviewCard> = dao.all().map { it.toDomain() }
    override suspend fun save(card: ReviewCard) = dao.upsert(card.toEntity())
    override suspend fun saveAll(cards: List<ReviewCard>) = dao.upsertAll(cards.map { it.toEntity() })
}

/**
 * Returns a [ReviewCard] for every [wordIds] entry, seeding a fresh,
 * immediately-due card (Szenario S10: Erststart) for any id that has no
 * stored card yet -- newly seeded cards are persisted right away (as one
 * batch via [ReviewCardRepository.saveAll]) so a second load doesn't reseed
 * them with a different due time.
 */
suspend fun ReviewCardRepository.allOrSeed(wordIds: List<String>): List<ReviewCard> {
    val existing = all().associateBy { it.wordId }
    val seeded = wordIds.filter { it !in existing }
        .associate { it to ReviewCard(wordId = it, dueAtEpochMillis = 0L) }
    if (seeded.isNotEmpty()) saveAll(seeded.values.toList())
    return wordIds.map { existing[it] ?: seeded.getValue(it) }
}
