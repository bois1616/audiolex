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
 *
 * [noiseEnabled]/[snrDb]/[noiseScenario] (Backlog M4 "Störgeräusch-Overlay",
 * ADR-0010) hold the noise-overlay setting shared by both training modes.
 * [noiseScenario] stores the scenario's `id` from `files/noise/noise.json`
 * as a plain string, not an enum -- the catalog is data, not a compile-time
 * type -- so the "unknown id" fallback (resolve to the first catalog entry)
 * lives where the catalog is loaded (composeApp), not in this mapper.
 *
 * [channelMode] (Backlog M4 "Kopfhörer-Bogen Batch B", ADR-0011) stores the
 * `ChannelMode` enum name, same pattern as [themeMode]/[corpusMode].
 *
 * [corpusSource] (Backlog Eigen-Korpus Batch C, ADR-0012) stores the
 * `CorpusSource` enum name, same pattern again -- the column added by
 * [MIGRATION_6_7][de.hexenwoche.audiolex.core.persistence.MIGRATION_6_7]
 * instead of the destructive fallback that carried every earlier column
 * (see [de.hexenwoche.audiolex.core.persistence.createAudioLexDatabase]):
 * existing SRS cards and session history are no longer disposable test
 * data, so this is the first schema bump that must actually preserve them.
 */
@Entity
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val themeMode: String,
    val corpusMode: String = "WOERTER",
    val noiseEnabled: Boolean = false,
    val snrDb: Int = 5,
    val noiseScenario: String = "restaurant",
    val channelMode: String = "BEIDE",
    val corpusSource: String = "MITGELIEFERT",
)

@Dao
interface SettingsDao {
    @Upsert
    suspend fun upsert(settings: SettingsEntity)

    @Query("SELECT * FROM SettingsEntity WHERE id = 0")
    suspend fun get(): SettingsEntity?
}
