package de.hexenwoche.audiolex.core.audio

import kotlinx.serialization.Serializable

/**
 * One self-recorded or imported noise loop (Backlog M4 "Eigene Störgeräusche
 * aufnehmen, importieren und löschen", AC1). Structurally the own corpus's
 * [de.hexenwoche.audiolex.core.corpus.OwnEntry] with a different purpose:
 * metadata for a WAV file that underlays the training words as noise, kept
 * in a plain JSON file next to the WAVs, not in Room (ADR-0012 Nachtrag
 * applied to the third kind of user content, ADR-0013 zweiter Nachtrag).
 *
 * [fileName] is derived from [id], never from [label] (`"$id.wav"`, same
 * rule as OwnEntry): relabelling must not rename the file on disk. Unlike an
 * OwnEntry, a noise always has its recording -- the only ways in are a
 * finished recording or a validated import (AC4), so there is no
 * "metadata without audio" state.
 */
@Serializable
data class OwnNoise(
    val id: String,
    val label: String,
    val fileName: String,
    val createdAtEpochMillis: Long,
    val source: OwnNoiseSource = OwnNoiseSource.AUFNAHME,
)

/** How a noise entered the app (AC1): recorded here or imported from a WAV file. */
@Serializable
enum class OwnNoiseSource {
    AUFNAHME,
    IMPORT,
}
