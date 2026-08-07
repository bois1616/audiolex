package de.hexenwoche.audiolex

import de.hexenwoche.audiolex.core.audio.NoiseLoop
import de.hexenwoche.audiolex.core.audio.NoiseScenario
import de.hexenwoche.audiolex.core.audio.OwnNoise
import de.hexenwoche.audiolex.core.audio.OwnNoiseSource
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.mixWithNoise
import de.hexenwoche.audiolex.core.audio.noiseGainForSnr
import de.hexenwoche.audiolex.core.audio.rms
import de.hexenwoche.audiolex.core.audio.toMono
import de.hexenwoche.audiolex.generated.resources.Res
import kotlinx.serialization.json.Json

/**
 * Shared noise-overlay loading/mixing (Backlog M4 "Störgeräusch-Overlay",
 * ADR-0010): both [LernmodusScreen] and [PruefmodusScreen] mix identically,
 * so the logic lives here once instead of drifting apart between two copies.
 */
private val noiseJson = Json { ignoreUnknownKeys = true }

/**
 * Loads the bundled noise-scenario catalog (`files/noise/noise.json`).
 * Returns an empty list if the file is missing/corrupt (Res.readBytes throws
 * on a fresh checkout without the gitignored assets) -- callers treat that
 * the same as "no scenarios available", not a crash. Since the own noises
 * exist (Backlog M4 "Eigene Störgeräusche", AC2) this is only the bundled
 * half of the catalog; [loadAllNoiseScenarios] is what screens and playback
 * actually use. Kept unchanged as the building block it was before.
 */
internal suspend fun loadNoiseScenarios(): List<NoiseScenario> =
    try {
        val bytes = Res.readBytes("files/noise/noise.json").decodeToString()
        noiseJson.decodeFromString<List<NoiseScenario>>(bytes)
    } catch (e: Exception) {
        emptyList()
    }

/** Prefix of every own noise's scenario id -- collision-free against bundled ids, which come from `noise.json`. */
internal const val OWN_NOISE_SCENARIO_PREFIX: String = "eigen-"

/**
 * One own noise as a [NoiseScenario] of the merged catalog (AC2): id
 * prefixed `eigen-` so it can never collide with a bundled id, [fileRef] is
 * the noise's own file name (what [OwnNoiseRepository.bytes] reads), and
 * [source]/[license] carry the provenance the bundled entries use theirs
 * for. The license of a self-recorded or self-imported sound is nobody's
 * but the user's, so it stays empty.
 */
private fun OwnNoise.asScenario(): NoiseScenario = NoiseScenario(
    id = "$OWN_NOISE_SCENARIO_PREFIX$id",
    label = label,
    fileRef = fileName,
    source = if (source == OwnNoiseSource.IMPORT) "Import" else "Eigene Aufnahme",
    license = "",
)

/**
 * The merged catalog (AC2): bundled scenarios plus the user's own noises,
 * in that order -- the bundled ones keep their familiar place, the own ones
 * simply follow. Used by the settings screen for the scenario choice and by
 * [loadNoiseBuffer] for playback, so choice and playback always agree on
 * what exists. The empty-/missing-file fallbacks stay the ones ADR-0010
 * point 4 established: an empty bundled half is not an error.
 */
internal suspend fun loadAllNoiseScenarios(ownNoiseRepository: OwnNoiseRepository?): List<NoiseScenario> =
    loadNoiseScenarios() + (ownNoiseRepository?.all() ?: emptyList()).map { it.asScenario() }

/**
 * Loads the noise loop selected by [noiseScenario], defensively, for the
 * mixing step in the training screens' playback producer. Returns null when
 * there is nothing to mix in: noise is off, the catalog is empty, or the
 * scenario's WAV is missing/unreadable -- all fall back to clean speech
 * (ADR-0010 point 4), never a crash. An unknown [noiseScenario] id (not in
 * the loaded catalog, e.g. an own noise deleted since the setting was saved)
 * resolves to the catalog's first entry rather than failing outright -- the
 * existing resolution, no extra logic for the own noises (AC2).
 *
 * Bundled bytes come from the Compose resources as before; an own scenario
 * (id prefix `eigen-`) reads its WAV through [ownNoiseRepository], the
 * [de.hexenwoche.audiolex.core.corpus.RecordingSource.EIGEN] branch of
 * `CorpusLoading.kt`'s [readRecordingBytes] applied to noise. Own
 * recordings and imports are validated to be PCM16 mono 22050 Hz before
 * they enter the collection (AC4), the exact format the mixer requires.
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
