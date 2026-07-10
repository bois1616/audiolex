package de.hexenwoche.audiolex.core.persistence

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Aggregate record of one completed Prüfmodus session (Szenario S12): no
 * per-card results, just counts -- see the Sitzungshistorie backlog item for
 * the scope decision (aggregates only, Prüfmodus only). [zoneId] is stored
 * purely for display formatting (Sitzungshistorie), never used in SRS
 * due-date math (ADR-0008).
 */
@Entity
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMillis: Long,
    val zoneId: String,
    val mode: String,
    val ratedCount: Int,
    val countAgain: Int,
    val countSoon: Int,
    val countLater: Int,
    val countGood: Int,
    val countPerfect: Int,
)

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM SessionEntity ORDER BY startedAtEpochMillis DESC")
    suspend fun all(): List<SessionEntity>
}
