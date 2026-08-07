package de.hexenwoche.audiolex

import de.hexenwoche.audiolex.core.audio.PcmBuffer
import de.hexenwoche.audiolex.core.audio.WavFile
import de.hexenwoche.audiolex.core.corpus.BackupContents
import de.hexenwoche.audiolex.core.corpus.BackupPlan
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.corpus.OwnEntry
import de.hexenwoche.audiolex.core.corpus.buildBackup
import de.hexenwoche.audiolex.core.corpus.encodeOwnCorpus
import de.hexenwoche.audiolex.core.corpus.parseOwnCorpus
import de.hexenwoche.audiolex.core.session.Session
import de.hexenwoche.audiolex.core.time.Clock
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Own-corpus CRUD (Backlog Eigen-Korpus Batch B, ADR-0012 Nachtrag): the
 * orchestration on top of the raw [OwnCorpusFiles] platform boundary, written
 * once here in commonMain -- same split as `CorpusLoading.kt`'s [loadCorpus]
 * calling into `:core`'s `parseCorpus`. The metadata list is rewritten in
 * full on every change via [OwnCorpusFiles.writeMetadataAtomic] (ADR-0012
 * Nachtrag: no transactions, no query optimizer, fine at this collection
 * size) -- crash-safety (AC2) is the file layer's job, this class only ever
 * hands it the complete new list.
 *
 * [OwnEntry.fileName] is always `"$id.wav"`, derived from the id and never
 * from the text (AC2) -- a filename doesn't change even if it was never
 * actually written (an entry saved with text but no recording yet) or gets
 * rewritten by [reRecord]. That's what lets [audioFor] and "entry has no
 * recording yet" share one code path: both are simply "the file for
 * [OwnEntry.fileName] doesn't exist right now".
 */
class OwnCorpusRepository(
    private val files: OwnCorpusFiles,
    private val clock: Clock,
) {
    suspend fun all(): List<OwnEntry> = withContext(Dispatchers.IO) { allBlocking() }

    /**
     * The subset of [all] that's actually trainable right now (Backlog
     * Eigen-Korpus Batch C, AC4). Batch B deliberately allows saving a
     * half-finished entry -- with a recording but no text yet, or with text
     * but no recording yet (Batch B, AC3) -- and both halves are required
     * before an entry can carry a training round:
     *
     * - No recording (never made, or the file has gone missing since): there
     *   is nothing to play. Lernmodus would quit with "Keine Aufnahme für …
     *   gefunden", Prüfmodus would show an unplayable card.
     * - No text: there is nothing to *show*. Lernmodus exists to pair sound
     *   with written word, and Prüfmodus's reveal card would come up blank,
     *   leaving nothing to self-rate against. This is also what the entry
     *   mask already promises the user in so many words ("Ohne Text lässt
     *   sich der Eintrag nicht trainieren", v0.23.0) -- letting such an entry
     *   into a round would make the app contradict its own hint.
     *
     * The recording half needs file access ([OwnCorpusFiles.recordingExists]),
     * which is why this lives here rather than in `:core`'s merge logic
     * ([de.hexenwoche.audiolex.core.corpus.mergeCorpus]), which stays
     * platform-free.
     */
    suspend fun trainable(): List<OwnEntry> = withContext(Dispatchers.IO) {
        allBlocking().filter { it.text.isNotBlank() && files.recordingExists(it.fileName) }
    }

    /**
     * Raw bytes for a recording by file name (Backlog Eigen-Korpus Batch C,
     * AC5): the merged-corpus playback path's counterpart to [audioFor],
     * used by `CorpusLoading.kt`'s shared read function. Kept separate from
     * [audioFor] (which additionally decodes, for "Eigene Aufnahmen"'s own
     * "Anhören" button) because a training screen only ever has an
     * [de.hexenwoche.audiolex.core.corpus.AudioRecording.fileRef] from the
     * merged corpus, not the matching [OwnEntry].
     */
    suspend fun recordingBytes(fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        files.readRecording(fileName)
    }

    /**
     * Adds a new entry (AC3). [audio] is nullable: AC3 only requires *one*
     * of a recording or a non-blank text to enable "Speichern", so a
     * text-only entry is a valid (if incomplete) save -- its file simply
     * doesn't exist yet, handled like any other missing recording (AC5).
     * The file is written before the metadata entry that references it, so
     * a failure in between never produces a ghost entry pointing at nothing.
     */
    suspend fun add(text: String, kind: EntryKind, speaker: String, audio: PcmBuffer?): OwnEntry =
        withContext(Dispatchers.IO) {
            val id = newId()
            val fileName = fileNameFor(id)
            if (audio != null) files.writeRecording(fileName, WavFile.encode(audio))
            val entry = OwnEntry(
                id = id,
                text = text,
                kind = kind,
                speaker = speaker,
                fileName = fileName,
                createdAtEpochMillis = clock.nowEpochMillis(),
            )
            saveAllBlocking(allBlocking() + entry)
            entry
        }

    /** Text correction without touching the recording (AC5). */
    suspend fun updateText(id: String, newText: String) = withContext(Dispatchers.IO) {
        saveAllBlocking(allBlocking().map { if (it.id == id) it.copy(text = newText) else it })
    }

    /**
     * Re-records without losing the text (AC5): [OwnEntry.fileName] is
     * stable, so only the file's bytes change -- the metadata list itself
     * doesn't need rewriting.
     */
    suspend fun reRecord(id: String, audio: PcmBuffer) = withContext(Dispatchers.IO) {
        files.writeRecording(fileNameFor(id), WavFile.encode(audio))
    }

    /**
     * Deletes both the metadata entry and its file (AC5). The file delete is
     * best-effort and doesn't gate removing the entry: the file most likely
     * being "not found" (already gone) is the common failure case, and a
     * metadata entry surviving a delete action would show up in the list as
     * a permanently unplayable ghost -- worse than a harmless leftover WAV
     * in an app-private directory nobody ever sees again.
     */
    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val entries = allBlocking()
        val entry = entries.firstOrNull { it.id == id }
        if (entry != null) {
            files.deleteRecording(entry.fileName)
            saveAllBlocking(entries - entry)
        }
    }

    /**
     * The recorded audio for [entry], or null if there is none yet or the
     * file has gone missing (AC5: a quiet "not available", not a crash --
     * same contract as [de.hexenwoche.audiolex.core.audio.AudioSource]).
     */
    suspend fun audioFor(entry: OwnEntry): PcmBuffer? = withContext(Dispatchers.IO) {
        files.readRecording(entry.fileName)?.let { WavFile.decode(it) }
    }

    /**
     * Builds the archive's contents (ADR-0013 point 1). [sessions] arrives as
     * plain data rather than as a repository on purpose: this class owns the
     * own corpus, not the session history, and it should stay that way even
     * though both end up in the same file. The coordination lives in
     * `Backup.kt` (Backlog "Sitzungshistorie mitsichern", AC4).
     */
    suspend fun buildArchive(nowEpochMillis: Long, sessions: List<Session>): BackupPlan =
        withContext(Dispatchers.IO) {
            buildBackup(allBlocking(), nowEpochMillis, sessions) { files.readRecording(it) }
        }

    /**
     * Writes the own-corpus half of an already-read archive and answers how
     * many entries were added.
     *
     * Each WAV goes down before the metadata that references it -- the same
     * order as [add], and for the same reason: a failure in between leaves an
     * unreferenced file, never an entry pointing at nothing. Judging what to
     * write happened earlier in `readBackup`, which is what keeps "no
     * half-imported state" (AC3) a property of the construction.
     */
    suspend fun applyImport(contents: BackupContents.Readable): Int = withContext(Dispatchers.IO) {
        for (pending in contents.toAdd) {
            files.writeRecording(pending.entry.fileName, pending.audio)
        }
        if (contents.toAdd.isNotEmpty()) {
            saveAllBlocking(allBlocking() + contents.toAdd.map { it.entry })
        }
        contents.toAdd.size
    }

    private fun allBlocking(): List<OwnEntry> = parseOwnCorpus(files.readMetadata())

    private fun saveAllBlocking(entries: List<OwnEntry>) = files.writeMetadataAtomic(encodeOwnCorpus(entries))

    private fun fileNameFor(id: String) = "$id.wav"

    /**
     * Timestamp plus a random suffix, collision-free enough for a
     * single-user, low-volume collection (ADR-0012 Nachtrag: tens to a few
     * hundred entries) without adding a UUID dependency.
     */
    private fun newId(): String = "own-${clock.nowEpochMillis()}-${Random.nextInt(1_000_000)}"
}

/**
 * A packed backup plus what it does and doesn't contain.
 * [skippedWithoutRecording] are entries that have a text but no recording
 * yet -- reported rather than silently omitted, so "12 Aufnahmen gesichert"
 * out of 13 entries never looks like a loss.
 */
class ArchiveExport(
    val bytes: ByteArray,
    val exported: Int,
    val skippedWithoutRecording: Int,
    val sessions: Int,
)

sealed interface ArchiveImport {
    /**
     * [sessionsAlreadyPresent] is counted against what the *archive* carried,
     * so an old backup without a `sitzungen/` folder reports zero on both
     * session counts and the message stays silent about them rather than
     * claiming "0 Sitzungen".
     */
    class Merged(
        val added: Int,
        val alreadyPresent: Int,
        val unusable: Int,
        val sessionsAdded: Int,
        val sessionsAlreadyPresent: Int,
    ) : ArchiveImport

    /** Not an AudioLex backup, or damaged (AC3) -- nothing was written. */
    data object Unreadable : ArchiveImport
}
