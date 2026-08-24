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
 * [noiseScenario] stores the chosen scenario's `id` as a plain string, not an
 * enum -- the catalog is data, not a compile-time type -- so the "unknown id"
 * fallback (resolve to the first catalog entry) lives where the catalog is
 * loaded (composeApp), not in this mapper. It defaults to empty ("keins
 * gewählt") since the bundled loops were removed (ADR-0014, v0.32.0); rows
 * written by older versions still carry a bundled id like `restaurant` and
 * are resolved by that same fallback. Only the Kotlin default changed, never
 * the column, so the schema and its version are untouched -- the CREATE TABLE
 * statements deliberately declare no SQL defaults (see [MIGRATION_7_8]).
 *
 * [channelMode] (Backlog M4 "Kopfhörer-Bogen Batch B", ADR-0011) stores the
 * `ChannelMode` enum name, same pattern as [themeMode]/[corpusMode].
 *
 * [excludedSpeakers] (Backlog Eigen-Korpus Batch D, ADR-0012 Nachtrag)
 * replaces Batch C's `corpusSource` column with a JSON array of excluded
 * speaker names -- a plain string, not an enum, since which speakers exist
 * is data, not a compile-time type (same rationale as [noiseScenario]).
 * Default `"[]"` means "nothing excluded" = "alle", matching
 * [de.hexenwoche.audiolex.core.settings.AppSettings.excludedSpeakers]'s
 * empty-set default. The column swap is carried by
 * [MIGRATION_7_8][de.hexenwoche.audiolex.core.persistence.MIGRATION_7_8],
 * not the destructive fallback -- same reasoning as
 * [MIGRATION_6_7][de.hexenwoche.audiolex.core.persistence.MIGRATION_6_7]:
 * existing SRS cards and session history must survive the jump.
 *
 * [uiLanguage] (ADR-0015) stores the
 * [de.hexenwoche.audiolex.core.i18n.UiLanguage] enum name, same pattern as
 * [themeMode]/[corpusMode]/[channelMode]. Default `"SYSTEM"` means "follow
 * the device language", which is why the upgrade needs no data written: an
 * install that was German because the app only spoke German stays German on
 * a German device. Carried by
 * [MIGRATION_8_9][de.hexenwoche.audiolex.core.persistence.MIGRATION_8_9], a
 * plain column append like [MIGRATION_6_7] rather than the table rebuild
 * [MIGRATION_7_8] needed.
 */
@Entity
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val themeMode: String,
    val corpusMode: String = "WOERTER",
    val noiseEnabled: Boolean = false,
    val snrDb: Int = 5,
    val noiseScenario: String = "",
    val channelMode: String = "BEIDE",
    val excludedSpeakers: String = "[]",
    val uiLanguage: String = "SYSTEM",
)

@Dao
interface SettingsDao {
    @Upsert
    suspend fun upsert(settings: SettingsEntity)

    @Query("SELECT * FROM SettingsEntity WHERE id = 0")
    suspend fun get(): SettingsEntity?
}
