package de.hexenwoche.audiolex.core.audio

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val ownNoiseJson = Json { ignoreUnknownKeys = true }

/**
 * Parses the own-noise metadata file's content (`geraeusche.json`) into a
 * list of [OwnNoise] (Backlog M4 "Eigene Störgeräusche", AC1). The split is
 * the same as for the own corpus: platform-free parse/encode here in `:core`,
 * the caller (`:composeApp`) supplies the file content as a string.
 *
 * [json] is null when the file doesn't exist yet -- nothing recorded or
 * imported so far, not an error -- and yields an empty list, same as a
 * genuinely empty `"[]"`.
 *
 * A malformed file also yields an empty list instead of throwing, the same
 * defensive posture as `parseOwnCorpus`: the document is parsed as a whole,
 * so a corrupt file can't be partially salvaged, and an exception here would
 * crash the management screen. The atomic write on the other side keeps a
 * crash mid-write from producing this case in the first place; this is the
 * second line of defence for whatever gets past it.
 */
fun parseOwnNoises(json: String?): List<OwnNoise> {
    if (json == null) return emptyList()
    return parseOwnNoisesOrNull(json) ?: emptyList()
}

/**
 * The same parse as [parseOwnNoises], but telling "damaged" apart from
 * "empty" by returning null instead of an empty list -- needed by the backup
 * reader (AC5), where a damaged metadata file must make the archive
 * unreadable rather than importing as "no noises" (the lesson of v0.29.1,
 * ADR-0013 zweiter Nachtrag: fehlend ≠ beschädigt).
 */
fun parseOwnNoisesOrNull(json: String): List<OwnNoise>? = try {
    ownNoiseJson.decodeFromString<List<OwnNoise>>(json)
} catch (e: SerializationException) {
    null
} catch (e: IllegalArgumentException) {
    null
}

/**
 * Serializes [noises] to the metadata file's full content. The caller is
 * responsible for writing it atomically (temp file + rename) -- this
 * function only ever produces the string.
 */
fun encodeOwnNoises(noises: List<OwnNoise>): String = ownNoiseJson.encodeToString(noises)
