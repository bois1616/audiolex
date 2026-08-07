package de.hexenwoche.audiolex.core.corpus

import de.hexenwoche.audiolex.core.audio.OwnNoise
import de.hexenwoche.audiolex.core.audio.OwnNoiseSource
import de.hexenwoche.audiolex.core.audio.encodeOwnNoises
import de.hexenwoche.audiolex.core.session.Session
import de.hexenwoche.audiolex.core.srs.ReviewRating
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The merge rules of ADR-0013 point 5, tested without a filesystem or a ZIP:
 * [buildBackup] and [readBackup] are deliberately pure so the rules that
 * decide what happens to irreplaceable data can be checked directly. The
 * archive mechanics get their own roundtrip test on jvm (`ZipArchiveTest`).
 */
class OwnCorpusBackupTest {

    private fun entry(id: String, text: String = "Ball") = OwnEntry(
        id = id,
        text = text,
        kind = EntryKind.WORD,
        speaker = "Anna",
        fileName = "$id.wav",
        createdAtEpochMillis = 1_000L,
    )

    private fun noise(id: String, label: String = "Baustelle") = OwnNoise(
        id = id,
        label = label,
        fileName = "$id.wav",
        createdAtEpochMillis = 2_000L,
        source = OwnNoiseSource.AUFNAHME,
    )

    private fun archiveOf(entries: List<OwnEntry>, audioFor: (OwnEntry) -> ByteArray? = { byteArrayOf(1, 2, 3) }) =
        buildBackup(entries, nowEpochMillis = 5_000L) { fileName ->
            entries.firstOrNull { it.fileName == fileName }?.let(audioFor)
        }.files

    @Test
    fun `backup contains the manifest, the metadata file and one WAV per entry`() {
        val entries = listOf(entry("own-1"), entry("own-2"))

        val plan = buildBackup(entries, nowEpochMillis = 5_000L) { byteArrayOf(9) }

        assertEquals(
            listOf(
                BACKUP_MANIFEST_PATH,
                OWN_CORPUS_METADATA_PATH,
                "$OWN_CORPUS_FOLDER/own-1.wav",
                "$OWN_CORPUS_FOLDER/own-2.wav",
            ),
            plan.files.map { it.path },
        )
        assertEquals(2, plan.exported)
        assertEquals(0, plan.skippedWithoutRecording)
    }

    @Test
    fun `the manifest actually carries the format marker, not just the timestamp`() {
        // Regression guard for a real A53 finding: all of BackupManifest's
        // fields have defaults, and kotlinx omits those unless told not to --
        // so the format marker was missing from every exported file.
        val plan = buildBackup(listOf(entry("own-1")), nowEpochMillis = 5_000L) { byteArrayOf(9) }

        val manifest = plan.files.single { it.path == BACKUP_MANIFEST_PATH }.bytes.decodeToString()

        assertTrue(manifest.contains("\"format\""), "format fehlt im Manifest: $manifest")
        assertTrue(manifest.contains("\"app\""), "app fehlt im Manifest: $manifest")
        assertTrue(manifest.contains("5000"), "Zeitstempel fehlt im Manifest: $manifest")
    }

    @Test
    fun `an entry with no recording yet is left out and counted`() {
        val entries = listOf(entry("own-1"), entry("own-2", text = "nur Text, nie aufgenommen"))

        val plan = buildBackup(entries, nowEpochMillis = 5_000L) { fileName ->
            if (fileName == "own-1.wav") byteArrayOf(9) else null
        }

        assertEquals(1, plan.exported)
        assertEquals(1, plan.skippedWithoutRecording)
        assertTrue(plan.files.none { it.path.endsWith("own-2.wav") })
        // Its metadata is left out too, so the archive stays self-consistent.
        val metadata = plan.files.single { it.path == OWN_CORPUS_METADATA_PATH }
        assertEquals(listOf("own-1"), parseOwnCorpus(metadata.bytes.decodeToString()).map { it.id })
    }

    @Test
    fun `unknown ids are added and their audio comes along`() {
        val archive = archiveOf(listOf(entry("own-1"), entry("own-2")))

        val contents = readBackup(archive, existing = emptyList())

        val readable = assertIs<BackupContents.Readable>(contents)
        assertEquals(listOf("own-1", "own-2"), readable.toAdd.map { it.entry.id })
        assertContentEquals(byteArrayOf(1, 2, 3), readable.toAdd.first().audio)
        assertEquals(0, readable.alreadyPresent)
        assertEquals(0, readable.unusable)
    }

    @Test
    fun `a known id is skipped, never overwritten`() {
        val archive = archiveOf(listOf(entry("own-1", text = "Fassung aus der Sicherung"), entry("own-2")))

        val contents = readBackup(archive, existing = listOf(entry("own-1", text = "Fassung auf dem Gerät")))

        val readable = assertIs<BackupContents.Readable>(contents)
        assertEquals(listOf("own-2"), readable.toAdd.map { it.entry.id })
        assertEquals(1, readable.alreadyPresent)
    }

    @Test
    fun `an entry whose WAV is missing from the archive is skipped, not added as a ghost`() {
        // Metadata lists two entries, the archive only carries one WAV --
        // a hand-edited or truncated backup.
        val entries = listOf(entry("own-1"), entry("own-2"))
        val archive = listOf(
            ArchiveFile(OWN_CORPUS_METADATA_PATH, encodeOwnCorpus(entries).encodeToByteArray()),
            ArchiveFile("$OWN_CORPUS_FOLDER/own-1.wav", byteArrayOf(7)),
        )

        val contents = readBackup(archive, existing = emptyList())

        val readable = assertIs<BackupContents.Readable>(contents)
        assertEquals(listOf("own-1"), readable.toAdd.map { it.entry.id })
        assertEquals(1, readable.unusable)
    }

    @Test
    fun `a file name that would escape the own-corpus directory is refused`() {
        val escaping = entry("own-1").copy(fileName = "../../etc/passwd")
        val archive = listOf(
            ArchiveFile(OWN_CORPUS_METADATA_PATH, encodeOwnCorpus(listOf(escaping)).encodeToByteArray()),
            ArchiveFile("$OWN_CORPUS_FOLDER/../../etc/passwd", byteArrayOf(7)),
        )

        val contents = readBackup(archive, existing = emptyList())

        val readable = assertIs<BackupContents.Readable>(contents)
        assertTrue(readable.toAdd.isEmpty())
        assertEquals(1, readable.unusable)
    }

    @Test
    fun `a ZIP without the own-corpus metadata file is not an AudioLex backup`() {
        val foreign = listOf(ArchiveFile("urlaubsfotos/strand.jpg", byteArrayOf(1)))

        assertEquals(BackupContents.Unreadable, readBackup(foreign, existing = emptyList()))
    }

    @Test
    fun `damaged metadata reports unreadable instead of importing nothing quietly`() {
        // The distinction that matters: reporting "0 Einträge übernommen" for
        // a broken backup would tell the user their backup was empty.
        val damaged = listOf(
            ArchiveFile(OWN_CORPUS_METADATA_PATH, """[ { "id": "own-1", "fileNa""".encodeToByteArray()),
        )

        assertEquals(BackupContents.Unreadable, readBackup(damaged, existing = emptyList()))
    }

    @Test
    fun `session history rides along as its own folder`() {
        val sessions = listOf(
            Session(1_000L, "Europe/Vienna", "PRUEFMODUS", 3, mapOf(ReviewRating.GOOD to 3)),
        )

        val plan = buildBackup(listOf(entry("own-1")), nowEpochMillis = 5_000L, sessions = sessions) {
            byteArrayOf(9)
        }

        assertTrue(plan.files.any { it.path == SESSIONS_PATH })
        assertEquals(1, plan.sessions)
        val readable = assertIs<BackupContents.Readable>(readBackup(plan.files, existing = emptyList()))
        assertEquals(sessions, readable.sessions)
    }

    @Test
    fun `an archive without a sessions folder stays importable`() {
        // Every backup written before ADR-0013's Nachtrag has no sitzungen/
        // folder. Those must keep working -- a missing history is normal,
        // not damage.
        val archive = archiveOf(listOf(entry("own-1")))

        val readable = assertIs<BackupContents.Readable>(readBackup(archive, existing = emptyList()))

        assertTrue(readable.sessions.isEmpty())
        assertEquals(listOf("own-1"), readable.toAdd.map { it.entry.id })
    }

    @Test
    fun `no sessions means no sessions folder at all`() {
        val plan = buildBackup(listOf(entry("own-1")), nowEpochMillis = 5_000L) { byteArrayOf(9) }

        assertTrue(plan.files.none { it.path.startsWith("$SESSIONS_FOLDER/") })
    }

    @Test
    fun `a damaged session history makes the whole archive unreadable`() {
        // The opposite of the missing-folder case: present but broken must
        // not import as "no sessions", which would look like a clean restore
        // of an empty history.
        val archive = archiveOf(listOf(entry("own-1"))) +
            ArchiveFile(SESSIONS_PATH, """[ { "startedAtEpochMillis": 1, "zoneI""".encodeToByteArray())

        assertEquals(BackupContents.Unreadable, readBackup(archive, existing = emptyList()))
    }

    @Test
    fun `import adds nothing when the backup only holds entries already present`() {
        val existing = listOf(entry("own-1"), entry("own-2"))
        val archive = archiveOf(existing)

        val readable = assertIs<BackupContents.Readable>(readBackup(archive, existing))

        assertTrue(readable.toAdd.isEmpty())
        assertEquals(2, readable.alreadyPresent)
    }

    // --- Own noises, the third content kind (Backlog M4 "Eigene
    // Störgeräusche", AC5; ADR-0013 zweiter Nachtrag) ---

    private fun archiveWithNoises(
        entries: List<OwnEntry> = listOf(entry("own-1")),
        noises: List<OwnNoise> = listOf(noise("noise-1"), noise("noise-2")),
    ) = buildBackup(
        entries,
        nowEpochMillis = 5_000L,
        noises = noises,
        noiseBytes = { byteArrayOf(4, 5, 6) },
    ) { byteArrayOf(1, 2, 3) }.files

    @Test
    fun `noises ride along in their own folder`() {
        val noises = listOf(noise("noise-1"), noise("noise-2", label = "Regen"))

        val plan = buildBackup(
            listOf(entry("own-1")),
            nowEpochMillis = 5_000L,
            noises = noises,
            noiseBytes = { byteArrayOf(9) },
        ) { byteArrayOf(1) }

        assertEquals(
            listOf(
                BACKUP_MANIFEST_PATH,
                OWN_CORPUS_METADATA_PATH,
                "$OWN_CORPUS_FOLDER/own-1.wav",
                "$OWN_NOISE_FOLDER/geraeusche.json",
                "$OWN_NOISE_FOLDER/noise-1.wav",
                "$OWN_NOISE_FOLDER/noise-2.wav",
            ),
            plan.files.map { it.path },
        )
        assertEquals(2, plan.noises)

        val readable = assertIs<BackupContents.Readable>(readBackup(plan.files, existing = emptyList()))
        assertEquals(noises, readable.noisesToAdd.map { it.noise })
        assertContentEquals(byteArrayOf(9), readable.noisesToAdd.first().audio)
    }

    @Test
    fun `no noises means no noise folder at all`() {
        // Same rule as sitzungen/: absent is normal and must stay
        // indistinguishable from "written before this existed".
        val plan = buildBackup(listOf(entry("own-1")), nowEpochMillis = 5_000L) { byteArrayOf(9) }

        assertTrue(plan.files.none { it.path.startsWith("$OWN_NOISE_FOLDER/") })
        assertEquals(0, plan.noises)
    }

    @Test
    fun `an archive without the noise folder stays importable`() {
        // Every backup written before this change has no stoergeraeusche/
        // folder -- fehlend ≠ beschädigt (ADR-0013 zweiter Nachtrag).
        val archive = archiveOf(listOf(entry("own-1")))

        val readable = assertIs<BackupContents.Readable>(readBackup(archive, existing = emptyList()))

        assertTrue(readable.noisesToAdd.isEmpty())
        assertEquals(0, readable.noisesAlreadyPresent)
        assertEquals(listOf("own-1"), readable.toAdd.map { it.entry.id })
    }

    @Test
    fun `damaged noise metadata makes the whole archive unreadable`() {
        // The counterpart of the missing-folder case: present but broken must
        // not import as "no noises", which would look like a clean restore
        // (Lehre aus v0.29.1).
        val damaged = archiveWithNoises()
            .filter { it.path != OWN_NOISE_METADATA_PATH } +
            ArchiveFile(OWN_NOISE_METADATA_PATH, """[ { "id": "noise-1", "labe""".encodeToByteArray())

        assertEquals(BackupContents.Unreadable, readBackup(damaged, existing = emptyList()))
    }

    @Test
    fun `a known noise id is skipped, an unknown one is added`() {
        // Identity is the id, merge rule identical to the own corpus
        // (Entscheidung 5 gilt entsprechend).
        val archive = archiveWithNoises(noises = listOf(noise("noise-1", label = "Fassung aus der Sicherung"), noise("noise-2")))

        val readable = assertIs<BackupContents.Readable>(
            readBackup(archive, existing = emptyList(), existingNoises = listOf(noise("noise-1", label = "Fassung auf dem Gerät"))),
        )

        assertEquals(listOf("noise-2"), readable.noisesToAdd.map { it.noise.id })
        assertEquals(1, readable.noisesAlreadyPresent)
    }

    @Test
    fun `a noise whose WAV is missing from the archive is skipped, not added as a ghost`() {
        val noises = listOf(noise("noise-1"), noise("noise-2"))
        val archive = listOf(
            ArchiveFile(OWN_CORPUS_METADATA_PATH, encodeOwnCorpus(emptyList()).encodeToByteArray()),
            ArchiveFile(OWN_NOISE_METADATA_PATH, encodeOwnNoises(noises).encodeToByteArray()),
            ArchiveFile("$OWN_NOISE_FOLDER/noise-1.wav", byteArrayOf(7)),
        )

        val readable = assertIs<BackupContents.Readable>(readBackup(archive, existing = emptyList()))

        assertEquals(listOf("noise-1"), readable.noisesToAdd.map { it.noise.id })
        assertEquals(1, readable.noisesUnusable)
    }

    @Test
    fun `a noise file name that would escape the directory is refused`() {
        val escaping = noise("noise-1").copy(fileName = "../../etc/passwd")
        val archive = listOf(
            ArchiveFile(OWN_CORPUS_METADATA_PATH, encodeOwnCorpus(emptyList()).encodeToByteArray()),
            ArchiveFile(OWN_NOISE_METADATA_PATH, encodeOwnNoises(listOf(escaping)).encodeToByteArray()),
            ArchiveFile("$OWN_NOISE_FOLDER/../../etc/passwd", byteArrayOf(7)),
        )

        val readable = assertIs<BackupContents.Readable>(readBackup(archive, existing = emptyList()))

        assertTrue(readable.noisesToAdd.isEmpty())
        assertEquals(1, readable.noisesUnusable)
    }
}
