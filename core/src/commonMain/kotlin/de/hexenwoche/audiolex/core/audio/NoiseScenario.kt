package de.hexenwoche.audiolex.core.audio

import kotlinx.serialization.Serializable

/**
 * One noise-overlay scenario's metadata (Backlog M4 "Störgeräusch-Overlay",
 * ADR-0010). Mirrors `files/noise/noise.json`, one entry per bundled loop
 * (`verkehr`/`strassenbahn`/`restaurant`); deserialized the same way as
 * [de.hexenwoche.audiolex.core.corpus.AudioRecording]/`words.json`
 * (`Json { ignoreUnknownKeys = true }`) by the composeApp side, since only it
 * can reach `Res.readBytes` (this type itself stays platform-free). [source]/
 * [license] are provenance for the README table, not read by app logic.
 */
@Serializable
data class NoiseScenario(
    val id: String,
    val label: String,
    val fileRef: String,
    val source: String,
    val license: String,
)
