package de.hexenwoche.audiolex.core.persistence

import de.hexenwoche.audiolex.core.session.Session
import de.hexenwoche.audiolex.core.session.toDomain
import de.hexenwoche.audiolex.core.session.toEntity

/**
 * Persists completed session records (Backlog M3, Szenario S12). Callers
 * work against this facade, not the DAO/Entity directly.
 */
interface SessionRepository {
    suspend fun all(): List<Session>
    suspend fun save(session: Session)
}

class RoomSessionRepository(private val dao: SessionDao) : SessionRepository {
    override suspend fun all(): List<Session> = dao.all().map { it.toDomain() }
    override suspend fun save(session: Session) = dao.insert(session.toEntity())
}
