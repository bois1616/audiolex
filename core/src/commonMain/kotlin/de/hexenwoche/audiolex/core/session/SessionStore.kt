package de.hexenwoche.audiolex.core.session

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val sessionJson = Json { ignoreUnknownKeys = true }

/**
 * Serializes the session history for a backup archive (ADR-0013 Nachtrag
 * 2026-08-07). Mirrors `encodeOwnCorpus`/`parseOwnCorpusOrNull` in
 * `core/corpus` -- the caller supplies and receives strings, so this stays
 * platform-free and testable without a filesystem.
 */
fun encodeSessions(sessions: List<Session>): String = sessionJson.encodeToString(sessions)

/**
 * Strict counterpart to [encodeSessions]: null when the content is damaged,
 * never a silently empty list.
 *
 * The reason is the same one that produced `parseOwnCorpusOrNull`: reporting
 * "0 Sitzungen übernommen" for a broken backup would tell the user their
 * history was empty when it was in fact unreadable. On a restore path that
 * is the one outcome that must not happen.
 *
 * `ignoreUnknownKeys` covers the forward case -- a [de.hexenwoche.audiolex.core.srs.ReviewRating]
 * added later must not make today's backups unreadable. The reverse (a
 * rating missing from an older file) is already handled by
 * [Session.ratingCounts] being a map: absent keys simply aren't there, and
 * every reader of it uses a default.
 */
fun parseSessionsOrNull(json: String): List<Session>? = try {
    sessionJson.decodeFromString<List<Session>>(json)
} catch (e: SerializationException) {
    null
} catch (e: IllegalArgumentException) {
    null
}

/**
 * Which of [fromBackup] aren't in [existing] yet, keyed on
 * [Session.startedAtEpochMillis] (ADR-0013 Nachtrag: the Room id is
 * `autoGenerate` and device-local, so it cannot serve as identity across
 * devices). Adds only -- never overwrites, never deletes, same rule as the
 * own corpus (ADR-0013 point 5).
 */
fun sessionsToAdd(fromBackup: List<Session>, existing: List<Session>): List<Session> {
    val known = existing.map { it.startedAtEpochMillis }.toSet()
    return fromBackup.filter { it.startedAtEpochMillis !in known }
}
