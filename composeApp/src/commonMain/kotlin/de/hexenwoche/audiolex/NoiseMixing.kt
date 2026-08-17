package de.hexenwoche.audiolex

import de.hexenwoche.audiolex.core.audio.NoiseLoop
import de.hexenwoche.audiolex.core.audio.NoiseScenario
import de.hexenwoche.audiolex.core.audio.OwnNoise
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.mixWithNoise
import de.hexenwoche.audiolex.core.audio.noiseGainForSnr
import de.hexenwoche.audiolex.core.audio.parseNoiseCatalog
import de.hexenwoche.audiolex.core.audio.rms
import de.hexenwoche.audiolex.core.audio.toMono
import de.hexenwoche.audiolex.generated.resources.Res

/**
 * Shared noise-overlay loading/mixing (Backlog M4 "Störgeräusch-Overlay",
 * ADR-0010): both [LernmodusScreen] and [PruefmodusScreen] mix identically,
 * so the logic lives here once instead of drifting apart between two copies.
 *
 * The catalog has two halves, bundled first, the user's own noises after.
 * What changed with v0.32.0/v0.33.0 is *what may be bundled*: the three
 * foreign loops (salamisound/Pixabay, "nicht-kommerziell") are gone for good
 * (ADR-0014), and what ships instead is the author's own recording -- content
 * whose redistribution nobody has to ask about. The mechanism is the same as
 * before; the license question is what disappeared.
 */

/**
 * Prefix of every own noise's scenario id -- collision-free against bundled
 * ids, which come from `noise.json`. These ids are *persisted*
 * ([de.hexenwoche.audiolex.core.settings.AppSettings.noiseScenario]), so the
 * prefix is also why a stored own-noise choice keeps working across versions.
 */
internal const val OWN_NOISE_SCENARIO_PREFIX: String = "eigen-"

/**
 * The bundled half of the catalog (`files/noise/noise.json`). Returns an
 * empty list when the file is missing or unparsable -- `Res.readBytes` throws
 * on a checkout whose resources were never generated, and an empty bundled
 * half is a valid state, not an error (ADR-0010 point 4). The parsing itself
 * lives in `:core` ([parseNoiseCatalog]) so it can be unit-tested without
 * Compose resources; only the reading is here.
 */
internal suspend fun loadBundledNoiseScenarios(): List<NoiseScenario> =
    try {
        parseNoiseCatalog(Res.readBytes("files/noise/noise.json").decodeToString())
    } catch (e: Exception) {
        emptyList()
    }

/**
 * One own noise as a [NoiseScenario] entry: id prefixed so it can never
 * collide with a bundled one, [fileRef] is the noise's own file name (what
 * [OwnNoiseRepository.bytes] reads).
 */
private fun OwnNoise.asScenario(): NoiseScenario = NoiseScenario(
    id = "$OWN_NOISE_SCENARIO_PREFIX$id",
    label = label,
    fileRef = fileName,
)

/**
 * The merged catalog: bundled scenarios first, then the user's own noises.
 * Used by the settings screen for the scenario choice and by [loadNoiseBuffer]
 * for playback, so choice and playback always agree on what exists. Both
 * halves may be empty -- then there is nothing to mix in and the ADR-0010
 * point 4 fallback (clean speech) applies.
 */
internal suspend fun loadAllNoiseScenarios(ownNoiseRepository: OwnNoiseRepository?): List<NoiseScenario> =
    loadBundledNoiseScenarios() + (ownNoiseRepository?.all() ?: emptyList()).map { it.asScenario() }

/**
 * Loads the noise loop selected by [noiseScenario], defensively, for the
 * mixing step in the training screens' playback producer. Returns null when
 * there is nothing to mix in: noise is off, the catalog is empty, or the
 * scenario's WAV is missing/unreadable -- all fall back to clean speech
 * (ADR-0010 point 4), never a crash. An unknown [noiseScenario] id (not in
 * the loaded catalog: an own noise deleted since the setting was saved, one
 * of the loops removed in v0.32.0, or the empty default of a fresh install)
 * resolves to the catalog's first entry rather than failing outright -- which
 * is also what makes the bundled entry the effective default without any id
 * being hard-coded anywhere.
 *
 * Where the bytes come from follows the id: a bundled scenario reads the
 * packed Compose resource, an own one (prefix [OWN_NOISE_SCENARIO_PREFIX])
 * goes through [ownNoiseRepository] -- the
 * [de.hexenwoche.audiolex.core.corpus.RecordingSource.EIGEN] branch of
 * `CorpusLoading.kt`'s [readRecordingBytes] applied to noise. Own recordings
 * and imports are validated to be PCM16 mono 22050 Hz before they enter the
 * collection (AC4), and a bundled loop is converted to that same format
 * (`files/noise/README.md`) -- it is what the mixer requires.
 *
 * The returned [NoiseLoop] carries its RMS precomputed right here, once per
 * load (Backlog "Code-Qualität": Noise-RMS einmalig berechnen statt pro
 * Wort) -- the loop's PCM never changes afterwards, so [mixWithOptionalNoise]
 * no longer recomputes it on every playback.
 */
internal suspend fun loadNoiseBuffer(
    noiseEnabled: Boolean,
    noiseScenario: String,
    ownNoiseRepository: OwnNoiseRepository?,
): NoiseLoop? {
    if (!noiseEnabled) return null
    return try {
        val scenarios = loadAllNoiseScenarios(ownNoiseRepository)
        val scenario = scenarios.firstOrNull { it.id == noiseScenario } ?: scenarios.firstOrNull() ?: return null
        val bytes = if (scenario.id.startsWith(OWN_NOISE_SCENARIO_PREFIX)) {
            ownNoiseRepository?.bytes(scenario.fileRef) ?: return null
        } else {
            Res.readBytes("files/noise/${scenario.fileRef}")
        }
        NoiseLoop(WavFile.decode(bytes).toMono())
    } catch (e: Exception) {
        null
    }
}

/**
 * Mixes [noiseLoop] into [speech] at [snrDb] if present; otherwise (noise
 * disabled, or [loadNoiseBuffer] came back empty-handed) returns [speech]
 * unchanged. Also swallows a sample-rate/channel-count mismatch or a fully
 * silent signal (both would make [mixWithNoise]/[noiseGainForSnr] throw) --
 * the noise overlay is defensive by design (ADR-0010 point 4, "kein
 * Resampling in Code"): a mismatch means clean speech, not a crashed session.
 * Speech RMS is still computed here, per word, since the speech signal
 * changes every playback; only the noise side is precomputed (see
 * [loadNoiseBuffer]).
 */
internal fun mixWithOptionalNoise(speech: PcmBuffer, noiseLoop: NoiseLoop?, snrDb: Int): PcmBuffer {
    if (noiseLoop == null) return speech
    return try {
        val gain = noiseGainForSnr(speech.rms(), noiseLoop.rms, snrDb.toDouble())
        mixWithNoise(speech, noiseLoop.buffer, gain)
    } catch (e: IllegalArgumentException) {
        speech
    }
}
