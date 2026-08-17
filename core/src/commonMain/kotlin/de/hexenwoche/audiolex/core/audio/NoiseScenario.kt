package de.hexenwoche.audiolex.core.audio

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One noise-overlay scenario's metadata (Backlog M4 "Störgeräusch-Overlay",
 * ADR-0010). Two kinds of entry end up here: the bundled ones from
 * `files/noise/noise.json` (parsed by [parseNoiseCatalog]) and the user's own
 * noises ([OwnNoise], mapped in `NoiseMixing.kt`). [fileRef] is the file name
 * the PCM is read from -- under the packed Compose resources for a bundled
 * entry, under the own-noise directory for an own one; which of the two it is
 * follows from the id prefix (`OWN_NOISE_SCENARIO_PREFIX`).
 *
 * The `source`/`license` fields this carried until v0.31.1 are gone. They
 * documented the provenance of three foreign loops that no longer exist
 * (ADR-0014); everything bundled from now on is the author's own recording,
 * and its provenance belongs in `files/noise/README.md`, where a human -- an
 * F-Droid reviewer among them -- actually reads it, not in a JSON field no
 * code ever looks at.
 */
@Serializable
data class NoiseScenario(
    val id: String,
    val label: String,
    val fileRef: String,
)

private val noiseCatalogJson = Json { ignoreUnknownKeys = true }

/**
 * Parses the bundled noise catalog (`files/noise/noise.json`), platform-free
 * so it is jvm-testable -- same split as `parseCorpus`/`loadCorpus`: the
 * parsing lives here, reading the Compose resource stays in composeApp.
 *
 * Defensive like every other catalog parse in this codebase: anything that
 * isn't a JSON array of scenarios -- truncated, hand-edited, an object where
 * an array belongs -- yields an empty list instead of throwing. An empty
 * catalog is a valid state (ADR-0010 point 4: no noise means clean speech),
 * so there is no error case that needs its own handling. Unknown fields are
 * ignored, which keeps a hand-written `noise.json` carrying an extra comment
 * or provenance field valid.
 */
fun parseNoiseCatalog(json: String): List<NoiseScenario> =
    try {
        noiseCatalogJson.decodeFromString<List<NoiseScenario>>(json)
    } catch (e: Exception) {
        emptyList()
    }
