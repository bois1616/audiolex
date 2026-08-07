package de.hexenwoche.audiolex.core.corpus

import de.hexenwoche.audiolex.core.audio.OwnNoise
import de.hexenwoche.audiolex.core.audio.encodeOwnNoises
import de.hexenwoche.audiolex.core.audio.parseOwnNoisesOrNull
import de.hexenwoche.audiolex.core.session.Session
import de.hexenwoche.audiolex.core.session.encodeSessions
import de.hexenwoche.audiolex.core.session.parseSessionsOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// encodeDefaults matters here and nowhere else in this file: every field of
// [BackupManifest] has a default, so without it kotlinx writes only the
// timestamp and the format marker -- the one thing the manifest exists for --
// silently isn't in the file. Caught on the A53 by looking into a real
// exported ZIP rather than trusting the type.
private val backupJson = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

/**
 * One file inside a backup archive: its path within the ZIP plus its bytes.
 *
 * Deliberately not a `data class`: [bytes] would give it an identity-based
 * `equals`, which reads as a value comparison at the call site and silently
 * isn't one. Tests compare [path] and content explicitly instead.
 */
class ArchiveFile(val path: String, val bytes: ByteArray)

/**
 * The archive layout (Backlog "Sicherung eigener Aufnahmen", Ergänzung
 * 2026-08-07). One folder per kind of content, each with its own metadata
 * file, rather than a flat pile of WAVs next to a single JSON at the root:
 *
 * ```
 * audiolex-sicherung.json          <- [BackupManifest], format marker
 * eigene-aufnahmen/
 *     eintraege.json               <- List<OwnEntry>, same format as on-device
 *     own-1234-5678.wav
 *     ...
 * sitzungen/
 *     verlauf.json                 <- List<Session> (ADR-0013 Nachtrag)
 * stoergeraeusche/
 *     geraeusche.json              <- List<OwnNoise> (ADR-0013 zweiter Nachtrag)
 *     noise-1234-5678.wav
 *     ...
 * ```
 *
 * The shape earned itself within a day: `sitzungen/` was added on 2026-08-07
 * without touching anything that reads `eigene-aufnahmen/`, and archives
 * written before it stay readable. The own noise recordings (`stoergeraeusche/`,
 * Backlog M4 "Eigene Störgeräusche", ADR-0013 zweiter Nachtrag) are the third
 * content kind and got the same treatment -- again purely additive, again
 * without a format-number change. Choosing this layout later would have meant
 * reading two formats forever, because by then backups sit in users' document
 * folders and every future version still has to open them.
 */
const val BACKUP_MANIFEST_PATH: String = "audiolex-sicherung.json"
const val OWN_CORPUS_FOLDER: String = "eigene-aufnahmen"
const val OWN_CORPUS_METADATA_PATH: String = "$OWN_CORPUS_FOLDER/eintraege.json"

/**
 * Session history (ADR-0013 Nachtrag 2026-08-07). The second content kind the
 * folder layout above was cut for -- added without touching the manifest's
 * format number, because the addition is purely additive: archives written
 * before this existed have no `sitzungen/` folder and stay valid.
 */
const val SESSIONS_FOLDER: String = "sitzungen"
const val SESSIONS_PATH: String = "$SESSIONS_FOLDER/verlauf.json"

/**
 * Own noise recordings (Backlog M4 "Eigene Störgeräusche", AC5; ADR-0013
 * zweiter Nachtrag). The third content kind: same additive rules as
 * `sitzungen/` -- every archive written before this existed lacks the folder
 * and stays valid, the format marker stays [BACKUP_FORMAT_VERSION].
 */
const val OWN_NOISE_FOLDER: String = "stoergeraeusche"
const val OWN_NOISE_METADATA_PATH: String = "$OWN_NOISE_FOLDER/geraeusche.json"

/** Bumped only if a future layout stops being readable by this code; additive folders don't need it. */
const val BACKUP_FORMAT_VERSION: Int = 1

@Serializable
data class BackupManifest(
    val format: Int = BACKUP_FORMAT_VERSION,
    val createdAtEpochMillis: Long = 0L,
    val app: String = "AudioLex",
)

/**
 * What [buildBackup] produced: the archive's [files] plus what was left out
 * and why, so the screen can say it rather than leaving it silent.
 *
 * [skippedWithoutRecording] counts entries that exist in the collection but
 * have no audio file yet -- Batch B deliberately allows saving a text before
 * recording it (Batch B, AC3). Their text is retypable in seconds; a
 * recording is not, and ADR-0013 point 6 draws exactly that line for what a
 * backup is for. Exporting them anyway would put metadata in the archive
 * that [readBackup] then has to skip on the way back in, which is a
 * self-inconsistent archive for no gain.
 */
class BackupPlan(
    val files: List<ArchiveFile>,
    val exported: Int,
    val skippedWithoutRecording: Int,
    val sessions: Int = 0,
    /** How many own noises made it into the archive (AC5); 0 means no `stoergeraeusche/` folder at all. */
    val noises: Int = 0,
)

/**
 * Builds the archive contents for [entries] (ADR-0013 points 1 and 6, as
 * corrected by its two Nachträge: everything that cannot be recreated --
 * the own corpus, the session history and the own noises -- and nothing
 * else; no database, no bundled corpus, no bundled noise catalog).
 *
 * [recordingBytes] resolves an [OwnEntry.fileName] to its WAV bytes and
 * returns null when the file is missing, matching
 * `OwnCorpusFiles.readRecording`; [noiseBytes] does the same for
 * [OwnNoise.fileName]. Keeping both as parameters is what lets this function
 * stay platform-free and testable without a filesystem.
 */
fun buildBackup(
    entries: List<OwnEntry>,
    nowEpochMillis: Long,
    sessions: List<Session> = emptyList(),
    noises: List<OwnNoise> = emptyList(),
    noiseBytes: (String) -> ByteArray? = { null },
    recordingBytes: (String) -> ByteArray?,
): BackupPlan {
    val withAudio = entries.mapNotNull { entry ->
        recordingBytes(entry.fileName)?.let { entry to it }
    }
    val manifest = ArchiveFile(
        path = BACKUP_MANIFEST_PATH,
        bytes = backupJson.encodeToString(BackupManifest(createdAtEpochMillis = nowEpochMillis)).encodeToByteArray(),
    )
    val metadata = ArchiveFile(
        path = OWN_CORPUS_METADATA_PATH,
        bytes = encodeOwnCorpus(withAudio.map { it.first }).encodeToByteArray(),
    )
    val recordings = withAudio.map { (entry, bytes) ->
        ArchiveFile(path = "$OWN_CORPUS_FOLDER/${entry.fileName}", bytes = bytes)
    }
    // No history, no folder: an empty file would be indistinguishable from
    // "this version didn't know about sessions yet" on the reading side, and
    // the reader has to tolerate the missing folder anyway.
    val history = if (sessions.isEmpty()) {
        emptyList()
    } else {
        listOf(ArchiveFile(path = SESSIONS_PATH, bytes = encodeSessions(sessions).encodeToByteArray()))
    }
    // No noises, no folder -- same rule as for sitzungen/ above, and for the
    // same reason. A noise always has its WAV (both ways in require one), so
    // the null branch of noiseBytes is a defensive leftover-only case, and a
    // noise whose file has gone missing is quietly left out like an entry's.
    val noisesWithAudio = noises.mapNotNull { noise ->
        noiseBytes(noise.fileName)?.let { noise to it }
    }
    val noiseFolder = if (noisesWithAudio.isEmpty()) {
        emptyList()
    } else {
        listOf(
            ArchiveFile(
                path = OWN_NOISE_METADATA_PATH,
                bytes = encodeOwnNoises(noisesWithAudio.map { it.first }).encodeToByteArray(),
            ),
        ) + noisesWithAudio.map { (noise, bytes) ->
            ArchiveFile(path = "$OWN_NOISE_FOLDER/${noise.fileName}", bytes = bytes)
        }
    }
    return BackupPlan(
        files = listOf(manifest, metadata) + recordings + history + noiseFolder,
        exported = withAudio.size,
        skippedWithoutRecording = entries.size - withAudio.size,
        sessions = sessions.size,
        noises = noisesWithAudio.size,
    )
}

/** An entry from a backup that is ready to be written: metadata plus the audio it points at. */
class PendingImport(val entry: OwnEntry, val audio: ByteArray)

/** An own noise from a backup that is ready to be written: metadata plus its WAV bytes (AC5). */
class PendingNoiseImport(val noise: OwnNoise, val audio: ByteArray)

sealed interface BackupContents {
    /**
     * [alreadyPresent] are entries whose id the collection already has --
     * skipped, never overwritten (ADR-0013 point 5). [unusable] are entries
     * the archive itself couldn't deliver: their WAV is missing from the ZIP,
     * or their file name isn't a plain file name. Both are reported rather
     * than swallowed, so a partial archive doesn't look like a clean restore.
     * [noisesToAdd]/[noisesAlreadyPresent]/[noisesUnusable] are the same
     * verdict for the archive's own noises (AC5), counted separately so the
     * screen can name recordings and noises apart.
     */
    class Readable(
        val toAdd: List<PendingImport>,
        val alreadyPresent: Int,
        val unusable: Int,
        /**
         * The archive's session history, unfiltered (ADR-0013 Nachtrag).
         * Merging against what's on the device happens in the caller, which
         * is the only side that can read the existing sessions --
         * `sessionsToAdd` does the comparison. Empty for archives written
         * before sessions were part of the format.
         */
        val sessions: List<Session> = emptyList(),
        val noisesToAdd: List<PendingNoiseImport> = emptyList(),
        val noisesAlreadyPresent: Int = 0,
        val noisesUnusable: Int = 0,
    ) : BackupContents

    /** Not an AudioLex backup, or damaged beyond reading (AC3): a quiet message, never a partial import. */
    data object Unreadable : BackupContents
}

/**
 * Reads an archive and works out what would be added to [existing] and
 * [existingNoises] (ADR-0013 point 5: merge, never overwrite, never delete).
 *
 * Nothing is written here and no exception escapes -- the caller gets a
 * complete verdict first and then writes, which is what keeps AC3's "no
 * half-imported state" true by construction rather than by carefulness. A
 * ZIP without the own-corpus metadata file isn't ours ([BackupContents.Unreadable]);
 * neither is one whose metadata doesn't parse, which is why this uses the
 * strict [parseOwnCorpusOrNull] rather than the lenient [parseOwnCorpus].
 *
 * Ids are collision-free by construction (`own-<Zeitstempel>-<Zufall>`,
 * ADR-0012), so "id already known" genuinely means "the same entry", and
 * skipping it needs no conflict dialog. The same holds for the own noises
 * (AC5): identity is the id, and the merge rule is identical.
 */
fun readBackup(
    files: List<ArchiveFile>,
    existing: List<OwnEntry>,
    existingNoises: List<OwnNoise> = emptyList(),
): BackupContents {
    val metadata = files.firstOrNull { it.path == OWN_CORPUS_METADATA_PATH } ?: return BackupContents.Unreadable
    val entries = parseOwnCorpusOrNull(metadata.bytes.decodeToString()) ?: return BackupContents.Unreadable

    // Absent history is normal, not an error: every archive written before
    // ADR-0013's Nachtrag has no sitzungen/ folder, and those must stay
    // importable. A *present but damaged* one is an error -- silently
    // importing zero sessions would claim the history was empty.
    val historyFile = files.firstOrNull { it.path == SESSIONS_PATH }
    val sessions = if (historyFile == null) {
        emptyList()
    } else {
        parseSessionsOrNull(historyFile.bytes.decodeToString()) ?: return BackupContents.Unreadable
    }

    // Same distinction as for sitzungen/ (AC5, ADR-0013 zweiter Nachtrag):
    // every archive written before the own noises existed lacks the folder
    // and stays valid; *present but damaged* metadata must not import as
    // "no noises" but make the whole archive unreadable.
    val noiseMetadata = files.firstOrNull { it.path == OWN_NOISE_METADATA_PATH }
    val noises = if (noiseMetadata == null) {
        emptyList()
    } else {
        parseOwnNoisesOrNull(noiseMetadata.bytes.decodeToString()) ?: return BackupContents.Unreadable
    }

    val audioByName = files
        .filter { it.path.startsWith("$OWN_CORPUS_FOLDER/") && it.path != OWN_CORPUS_METADATA_PATH }
        .associateBy { it.path.removePrefix("$OWN_CORPUS_FOLDER/") }
    val knownIds = existing.map { it.id }.toSet()

    var alreadyPresent = 0
    var unusable = 0
    val toAdd = mutableListOf<PendingImport>()
    for (entry in entries) {
        val audio = audioByName[entry.fileName]
        when {
            entry.id in knownIds -> alreadyPresent++
            !isPlainFileName(entry.fileName) || audio == null -> unusable++
            else -> toAdd += PendingImport(entry, audio.bytes)
        }
    }

    val noiseAudioByName = files
        .filter { it.path.startsWith("$OWN_NOISE_FOLDER/") && it.path != OWN_NOISE_METADATA_PATH }
        .associateBy { it.path.removePrefix("$OWN_NOISE_FOLDER/") }
    val knownNoiseIds = existingNoises.map { it.id }.toSet()

    var noisesAlreadyPresent = 0
    var noisesUnusable = 0
    val noisesToAdd = mutableListOf<PendingNoiseImport>()
    for (noise in noises) {
        val audio = noiseAudioByName[noise.fileName]
        when {
            noise.id in knownNoiseIds -> noisesAlreadyPresent++
            !isPlainFileName(noise.fileName) || audio == null -> noisesUnusable++
            else -> noisesToAdd += PendingNoiseImport(noise, audio.bytes)
        }
    }

    return BackupContents.Readable(
        toAdd = toAdd,
        alreadyPresent = alreadyPresent,
        unusable = unusable,
        sessions = sessions,
        noisesToAdd = noisesToAdd,
        noisesAlreadyPresent = noisesAlreadyPresent,
        noisesUnusable = noisesUnusable,
    )
}

/**
 * Guards the one place where a foreign file decides a path we then write to:
 * [OwnEntry.fileName] comes out of the archive's JSON, and the import writes
 * a file under that name into the app's own-corpus directory. A name
 * carrying `..` or a separator would escape that directory. Ours are always
 * `"$id.wav"` (`OwnCorpusRepository`), so this rejects nothing legitimate.
 */
private fun isPlainFileName(name: String): Boolean =
    name.isNotBlank() &&
        name != "." &&
        name != ".." &&
        !name.contains('/') &&
        !name.contains('\\') &&
        !name.contains(' ')
