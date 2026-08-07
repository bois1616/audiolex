package de.hexenwoche.audiolex

import de.hexenwoche.audiolex.core.corpus.BackupContents
import de.hexenwoche.audiolex.core.corpus.packArchive
import de.hexenwoche.audiolex.core.corpus.readBackup
import de.hexenwoche.audiolex.core.corpus.unpackArchive
import de.hexenwoche.audiolex.core.persistence.SessionRepository
import de.hexenwoche.audiolex.core.session.sessionsToAdd

/**
 * Coordinates a backup across the three things that cannot be recreated
 * (ADR-0013 point 6, corrected by its two Nachträge): the own corpus, the
 * session history and the own noises (Backlog M4 "Eigene Störgeräusche",
 * AC5).
 *
 * This sits between the repositories rather than inside any of them. The own
 * corpus and the own noises live in files, the history in Room; none should
 * learn about the others just because a backup happens to contain all three.
 * SRS cards and settings stay out on purpose -- they re-form through use,
 * which is the part of decision 6 that survived. The bundled noise catalog
 * stays out too: it lives in the repository/build and is the one noise
 * inventory that can be restored (ADR-0013 zweiter Nachtrag).
 */
suspend fun exportBackup(
    ownCorpus: OwnCorpusRepository,
    ownNoises: OwnNoiseRepository,
    sessions: SessionRepository,
    nowEpochMillis: Long,
): ArchiveExport {
    // Noise bytes resolved here, up front, so the repositories' suspend APIs
    // stay untouched and the archive builder below receives a plain lookup.
    val noises = ownNoises.all()
    val noiseAudio = HashMap<String, ByteArray>()
    for (noise in noises) {
        ownNoises.bytes(noise.fileName)?.let { noiseAudio[noise.fileName] = it }
    }
    val plan = ownCorpus.buildArchive(nowEpochMillis, sessions.all(), noises) { noiseAudio[it] }
    return ArchiveExport(
        bytes = packArchive(plan.files),
        exported = plan.exported,
        skippedWithoutRecording = plan.skippedWithoutRecording,
        sessions = plan.sessions,
        noises = plan.noises,
    )
}

/**
 * Reads an archive and merges all three parts in (ADR-0013 point 5: adds,
 * never overwrites, never deletes -- applied to the own noises exactly as to
 * the own corpus, identity over the id, AC5).
 *
 * The archive is read and judged completely before anything is written, so an
 * unreadable file cannot leave a half-imported state behind. Sessions merge on
 * their start time rather than the Room id, which is `autoGenerate` and only
 * meaningful on the device that produced it.
 */
suspend fun importBackup(
    bytes: ByteArray,
    ownCorpus: OwnCorpusRepository,
    ownNoises: OwnNoiseRepository,
    sessions: SessionRepository,
): ArchiveImport {
    val archive = unpackArchive(bytes) ?: return ArchiveImport.Unreadable
    val contents = readBackup(archive, existing = ownCorpus.all(), existingNoises = ownNoises.all())
    if (contents !is BackupContents.Readable) return ArchiveImport.Unreadable

    val added = ownCorpus.applyImport(contents)
    val noisesAdded = ownNoises.applyImport(contents)
    val newSessions = sessionsToAdd(contents.sessions, sessions.all())
    for (session in newSessions) sessions.save(session)

    return ArchiveImport.Merged(
        added = added,
        alreadyPresent = contents.alreadyPresent,
        unusable = contents.unusable,
        sessionsAdded = newSessions.size,
        sessionsAlreadyPresent = contents.sessions.size - newSessions.size,
        noisesAdded = noisesAdded,
        noisesAlreadyPresent = contents.noisesAlreadyPresent,
        noisesUnusable = contents.noisesUnusable,
    )
}
