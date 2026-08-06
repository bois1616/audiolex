package de.hexenwoche.audiolex

import de.hexenwoche.audiolex.core.audio.NoiseLoop
import de.hexenwoche.audiolex.core.audio.NoiseScenario
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
 * Loads the noise-scenario catalog (`files/noise/noise.json`). Used both to
 * populate the "Szenario" choice in [EinstellungenScreen] and to resolve a
 * scenario id to its [NoiseScenario.fileRef] for playback. Returns an empty
 * list if the file is missing/corrupt (Res.readBytes throws on a fresh
 * checkout without the gitignored assets) -- callers treat that the same as
 * "no scenarios available", not a crash.
 */
internal suspend fun loadNoiseScenarios(): List<NoiseScenario> =
    try {
        val bytes = Res.readBytes("files/noise/noise.json").decodeToString()
        noiseJson.decodeFromString<List<NoiseScenario>>(bytes)
    } catch (e: Exception) {
        emptyList()
    }

/**
 * Loads the noise loop selected by [noiseScenario], defensively, for the
 * mixing step in the training screens' playback producer. Returns null when
 * there is nothing to mix in: noise is off, the catalog is empty, or the
 * scenario's WAV is missing/unreadable -- all fall back to clean speech
 * (ADR-0010 point 4), never a crash. An unknown [noiseScenario] id (not in
 * the loaded catalog, e.g. a scenario removed since the setting was saved)
 * resolves to the catalog's first entry rather than failing outright.
 *
 * The returned [NoiseLoop] carries its RMS precomputed right here, once per
 * load (Backlog "Code-Qualität": Noise-RMS einmalig berechnen statt pro
 * Wort) -- the loop's PCM never changes afterwards, so [mixWithOptionalNoise]
 * no longer recomputes it on every playback.
 */
internal suspend fun loadNoiseBuffer(noiseEnabled: Boolean, noiseScenario: String): NoiseLoop? {
    if (!noiseEnabled) return null
    return try {
        val scenarios = loadNoiseScenarios()
        val scenario = scenarios.firstOrNull { it.id == noiseScenario } ?: scenarios.firstOrNull() ?: return null
        val bytes = Res.readBytes("files/noise/${scenario.fileRef}")
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
