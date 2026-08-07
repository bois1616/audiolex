package de.hexenwoche.audiolex

import de.hexenwoche.audiolex.core.audio.OwnNoise
import de.hexenwoche.audiolex.core.audio.OwnNoiseSource
import de.hexenwoche.audiolex.core.audio.PcmBuffer
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.audio.encodeOwnNoises
import de.hexenwoche.audiolex.core.audio.parseOwnNoises
import de.hexenwoche.audiolex.core.corpus.BackupContents
import de.hexenwoche.audiolex.core.time.Clock
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Own-noise CRUD (Backlog M4 "Eigene Störgeräusche aufnehmen, importieren
 * und löschen", AC1): the orchestration on top of the raw [OwnNoiseFiles]
 * platform boundary, written once here in commonMain -- the exact split of
 * [OwnCorpusRepository]. The metadata list is rewritten in full on every
 * change via [OwnNoiseFiles.writeMetadataAtomic].
 *
 * Two differences to the own corpus, both by design:
 *
 * - A noise always has its recording. The only ways in are a finished
 *   recording or a validated import (AC4), so there is no text-without-audio
 *   half-state and no [OwnCorpusRepository.trainable]-style filter.
 * - Nothing is ever edited. Relabelling isn't part of the item, and changing
 *   a sound is delete + record again (Autor-Entscheid: „nicht sinnvoll").
 */
class OwnNoiseRepository(
    private val files: OwnNoiseFiles,
    private val clock: Clock,
) {
    suspend fun all(): List<OwnNoise> = withContext(Dispatchers.IO) { allBlocking() }

    /**
     * Raw WAV bytes by file name, the noise side of
     * [OwnCorpusRepository.recordingBytes]: what `NoiseMixing.kt`'s merged
     * `loadNoiseBuffer` reads for an own scenario (AC2), and what the backup
     * export packs (AC5).
     */
    suspend fun bytes(fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        files.readNoise(fileName)
    }

    /**
     * The decoded recording for [noise], or null if the file has gone missing
     * -- a quiet "not available", not a crash; used by the management list's
     * "Anhören" button.
     */
    suspend fun audioFor(noise: OwnNoise): PcmBuffer? = withContext(Dispatchers.IO) {
        files.readNoise(noise.fileName)?.let { WavFile.decode(it) }
    }

    /**
     * Adds a noise (AC1/AC4). [wavBytes] are written as-is -- for a
     * recording that is [WavFile.encode] of the taken buffer, for an import
     * the validated original file, byte-identical (no re-encode: an import
     * stays exactly what the user handed over). The file is written before
     * the metadata entry that references it, so a failure in between never
     * produces a ghost entry pointing at nothing.
     */
    suspend fun add(label: String, wavBytes: ByteArray, source: OwnNoiseSource): OwnNoise =
        withContext(Dispatchers.IO) {
            val id = newId()
            val fileName = fileNameFor(id)
            files.writeNoise(fileName, wavBytes)
            val noise = OwnNoise(
                id = id,
                label = label,
                fileName = fileName,
                createdAtEpochMillis = clock.nowEpochMillis(),
                source = source,
            )
            saveAllBlocking(allBlocking() + noise)
            noise
        }

    /**
     * Deletes both the metadata entry and its file. The file delete is
     * best-effort and doesn't gate removing the entry -- same reasoning as
     * [OwnCorpusRepository.delete]: a leftover WAV in an app-private
     * directory nobody sees is harmless, a ghost entry in the list is not.
     */
    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val noises = allBlocking()
        val noise = noises.firstOrNull { it.id == id }
        if (noise != null) {
            files.deleteNoise(noise.fileName)
            saveAllBlocking(noises - noise)
        }
    }

    /**
     * Writes the own-noise half of an already-read archive and answers how
     * many noises were added (AC5). The mirror of
     * [OwnCorpusRepository.applyImport]: each WAV goes down before the
     * metadata that references it, and judging what to write happened
     * earlier in `readBackup`.
     */
    suspend fun applyImport(contents: BackupContents.Readable): Int = withContext(Dispatchers.IO) {
        for (pending in contents.noisesToAdd) {
            files.writeNoise(pending.noise.fileName, pending.audio)
        }
        if (contents.noisesToAdd.isNotEmpty()) {
            saveAllBlocking(allBlocking() + contents.noisesToAdd.map { it.noise })
        }
        contents.noisesToAdd.size
    }

    private fun allBlocking(): List<OwnNoise> = parseOwnNoises(files.readMetadata())

    private fun saveAllBlocking(noises: List<OwnNoise>) = files.writeMetadataAtomic(encodeOwnNoises(noises))

    private fun fileNameFor(id: String) = "$id.wav"

    /** Timestamp plus a random suffix, the same collision-free schema as the own corpus (ADR-0013 Entscheidung 5). */
    private fun newId(): String = "noise-${clock.nowEpochMillis()}-${Random.nextInt(1_000_000)}"
}
