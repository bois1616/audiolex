package de.hexenwoche.audiolex.core.persistence

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Singleton settings row (Backlog M4 "Settings-Persistenz-Fundament"): fixed
 * primary key id=0, so [upsert][SettingsDao.upsert] always overwrites the
 * same row instead of accumulating rows. [themeMode]/[corpusMode] store enum
 * names, not ordinals -- same rationale as [ReviewRatingConverter]. More
 * fields are expected to land here as additional columns rather than a new
 * mechanism. [corpusMode] defaults to WOERTER so constructor call sites that
 * predate the column (and Room's own default-value handling) keep working.
 */
@Entity
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val themeMode: String,
    val corpusMode: String = "WOERTER",
)

@Dao
interface SettingsDao {
    @Upsert
    suspend fun upsert(settings: SettingsEntity)

    @Query("SELECT * FROM SettingsEntity WHERE id = 0")
    suspend fun get(): SettingsEntity?
}
