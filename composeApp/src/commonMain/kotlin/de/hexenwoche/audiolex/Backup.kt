package de.hexenwoche.audiolex

import de.hexenwoche.audiolex.core.corpus.BackupContents
import de.hexenwoche.audiolex.core.corpus.packArchive
import de.hexenwoche.audiolex.core.corpus.readBackup
import de.hexenwoche.audiolex.core.corpus.unpackArchive
import de.hexenwoche.audiolex.core.persistence.SessionRepository
import de.hexenwoche.audiolex.core.session.sessionsToAdd

/**
 * Coordinates a backup across the two things that cannot be recreated
 * (ADR-0013 point 6 as corrected by its Nachtrag 2026-08-07): the own corpus
 * and the session history.
 *
 * This sits between the two repositories rather than inside either. The own
 * corpus lives in files, the history in Room; neither should learn about the
 * other just because a backup happens to contain both. SRS cards and settings
 * stay out on purpose -- they re-form through use, which is the part of
 * decision 6 that survived.
 */
suspend fun exportBackup(
    ownCorpus: OwnCorpusRepository,
    sessions: SessionRepository,
    nowEpochMillis: Long,
): ArchiveExport {
    val plan = ownCorpus.buildArchive(nowEpochMillis, sessions.all())
    return ArchiveExport(
        bytes = packArchive(plan.files),
        exported = plan.exported,
        skippedWithoutRecording = plan.skippedWithoutRecording,
        sessions = plan.sessions,
    )
}

/**
 * Reads an archive and merges both halves in (ADR-0013 point 5: adds, never
 * overwrites, never deletes).
 *
 * The archive is read and judged completely before anything is written, so an
 * unreadable file cannot leave a half-imported state behind. Sessions merge on
 * their start time rather than the Room id, which is `autoGenerate` and only
 * meaningful on the device that produced it.
 */
suspend fun importBackup(
    bytes: ByteArray,
    ownCorpus: OwnCorpusRepository,
    sessions: SessionRepository,
): ArchiveImport {
    val archive = unpackArchive(bytes) ?: return ArchiveImport.Unreadable
    val contents = readBackup(archive, ownCorpus.all())
    if (contents !is BackupContents.Readable) return ArchiveImport.Unreadable

    val added = ownCorpus.applyImport(contents)
    val newSessions = sessionsToAdd(contents.sessions, sessions.all())
    for (session in newSessions) sessions.save(session)

    return ArchiveImport.Merged(
        added = added,
        alreadyPresent = contents.alreadyPresent,
        unusable = contents.unusable,
        sessionsAdded = newSessions.size,
        sessionsAlreadyPresent = contents.sessions.size - newSessions.size,
    )
}
