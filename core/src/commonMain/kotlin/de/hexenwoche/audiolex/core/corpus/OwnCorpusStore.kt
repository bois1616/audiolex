package de.hexenwoche.audiolex.core.corpus

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val ownCorpusJson = Json { ignoreUnknownKeys = true }

/**
 * Parses the own-corpus metadata file's content into a list of [OwnEntry].
 * Platform-free (ADR-0012 Nachtrag), the "Lese-/Schreiblogik" the backlog
 * item asks for: the caller (`:composeApp`) supplies the file content as a
 * string, same split as [parseCorpus]/`CorpusLoading.kt`.
 *
 * [json] is null when the file doesn't exist yet -- first launch, nothing
 * recorded so far, not an error -- and yields an empty collection, same as
 * a genuinely empty `"[]"`. Same `ignoreUnknownKeys = true` posture as
 * [parseCorpus] so a later-added field never makes an older collection
 * unreadable.
 *
 * A malformed file (truncated, hand-edited, or anything else that isn't a
 * valid JSON array of [OwnEntry]) also yields an empty collection instead of
 * throwing: the document is parsed as a whole, so a corrupt file can't be
 * partially salvaged, and letting the exception propagate would crash a
 * screen whose entire purpose is damage control (ADR-0012 point 5 limits
 * damage, it doesn't claim to eliminate every possible failure). [AC2]'s
 * atomic write already keeps a crash mid-write from producing this file in
 * the first place; this is the second line of defence for whatever gets
 * past it (e.g. manual editing, ADR-0012 Nachtrag "im Notfall ein
 * Texteditor").
 */
fun parseOwnCorpus(json: String?): List<OwnEntry> {
    if (json == null) return emptyList()
    return parseOwnCorpusOrNull(json) ?: emptyList()
}

/**
 * The same parse as [parseOwnCorpus], but telling "damaged" apart from
 * "empty" by returning null instead of an empty list (Backlog "Sicherung
 * eigener Aufnahmen", AC3).
 *
 * [parseOwnCorpus]'s leniency is right for the on-device file, where an
 * empty collection is the honest reading of "nothing usable here" and the
 * screen's whole job is damage control. It is wrong when reading a *backup*:
 * a damaged metadata file would import zero entries and report success,
 * telling the user their backup was empty when it was in fact broken. That
 * is the one outcome a restore path must never produce, so the backup reader
 * takes the strict variant and says "unlesbar" out loud.
 */
fun parseOwnCorpusOrNull(json: String): List<OwnEntry>? = try {
    ownCorpusJson.decodeFromString<List<OwnEntry>>(json)
} catch (e: SerializationException) {
    null
} catch (e: IllegalArgumentException) {
    null
}

/**
 * Serializes [entries] back to the metadata file's full content. The caller
 * is responsible for writing it atomically (AC2, temp file + rename) --
 * this function only ever produces the bytes.
 */
fun encodeOwnCorpus(entries: List<OwnEntry>): String = ownCorpusJson.encodeToString(entries)
