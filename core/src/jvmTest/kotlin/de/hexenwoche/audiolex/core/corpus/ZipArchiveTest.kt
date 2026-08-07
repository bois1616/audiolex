package de.hexenwoche.audiolex.core.corpus

import de.hexenwoche.audiolex.core.audio.OwnNoise
import de.hexenwoche.audiolex.core.audio.OwnNoiseSource
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The real archive mechanics against `java.util.zip` (AC8): the full
 * Export -> Import roundtrip, which is the only test that proves a backup can
 * actually be restored rather than merely built.
 */
class ZipArchiveTest {

    private fun entry(id: String) = OwnEntry(
        id = id,
        text = "Kaffeetasse",
        kind = EntryKind.WORD,
        speaker = "Anna",
        fileName = "$id.wav",
        createdAtEpochMillis = 1_000L,
    )

    @Test
    fun `export then import restores every entry and its audio byte for byte`() {
        val entries = listOf(entry("own-1"), entry("own-2"))
        val audio = entries.associate { it.fileName to Random(it.id.hashCode()).nextBytes(512) }

        val zip = packArchive(buildBackup(entries, nowEpochMillis = 5_000L) { audio[it] }.files)
        val unpacked = unpackArchive(zip)
        val contents = readBackup(unpacked!!, existing = emptyList())

        val readable = assertIs<BackupContents.Readable>(contents)
        assertEquals(entries, readable.toAdd.map { it.entry })
        for (pending in readable.toAdd) {
            assertContentEquals(audio.getValue(pending.entry.fileName), pending.audio)
        }
    }

    @Test
    fun `a restore onto a device that already has half the entries adds only the rest`() {
        val all = listOf(entry("own-1"), entry("own-2"), entry("own-3"))
        val zip = packArchive(buildBackup(all, nowEpochMillis = 5_000L) { byteArrayOf(1, 2) }.files)

        val readable = assertIs<BackupContents.Readable>(
            readBackup(unpackArchive(zip)!!, existing = listOf(entry("own-2"))),
        )

        assertEquals(listOf("own-1", "own-3"), readable.toAdd.map { it.entry.id })
        assertEquals(1, readable.alreadyPresent)
    }

    @Test
    fun `a file that is not a ZIP yields null instead of throwing`() {
        assertNull(unpackArchive("Dies ist ein Textdokument, kein Archiv.".encodeToByteArray()))
    }

    @Test
    fun `a truncated ZIP yields null instead of throwing`() {
        val zip = packArchive(buildBackup(listOf(entry("own-1")), 5_000L) { Random(1).nextBytes(4096) }.files)

        val truncated = zip.copyOfRange(0, zip.size / 2)

        // Either it reads nothing usable (null) or it salvages a prefix of
        // entries -- both are quiet outcomes; what must not happen is a throw.
        val unpacked = unpackArchive(truncated)
        if (unpacked != null) {
            assertTrue(readBackup(unpacked, existing = emptyList()) is BackupContents.Unreadable)
        }
    }

    @Test
    fun `an empty byte array yields null`() {
        assertNull(unpackArchive(ByteArray(0)))
    }

    @Test
    fun `a foreign ZIP is recognised as not being an AudioLex backup`() {
        val foreign = packArchive(
            listOf(
                ArchiveFile("notizen.txt", "Einkaufsliste".encodeToByteArray()),
                ArchiveFile("bilder/strand.jpg", byteArrayOf(0xFF.toByte(), 0xD8.toByte())),
            ),
        )

        assertEquals(BackupContents.Unreadable, readBackup(unpackArchive(foreign)!!, existing = emptyList()))
    }

    @Test
    fun `export then import restores own noises byte for byte alongside the entries`() {
        // The full roundtrip for the third content kind (Backlog M4 "Eigene
        // Störgeräusche", AC5): pack -> unpack -> read, entries and noises
        // each restored with their own audio.
        val entries = listOf(entry("own-1"))
        val noises = listOf(
            OwnNoise("noise-1", "Baustelle", "noise-1.wav", 2_000L, OwnNoiseSource.AUFNAHME),
            OwnNoise("noise-2", "Regen", "noise-2.wav", 3_000L, OwnNoiseSource.IMPORT),
        )
        val entryAudio = entries.associate { it.fileName to Random(it.id.hashCode()).nextBytes(512) }
        val noiseAudio = noises.associate { it.fileName to Random(it.id.hashCode()).nextBytes(8192) }

        val zip = packArchive(
            buildBackup(
                entries,
                nowEpochMillis = 5_000L,
                noises = noises,
                noiseBytes = { noiseAudio[it] },
            ) { entryAudio[it] }.files,
        )
        val contents = readBackup(unpackArchive(zip)!!, existing = emptyList())

        val readable = assertIs<BackupContents.Readable>(contents)
        assertEquals(entries, readable.toAdd.map { it.entry })
        assertEquals(noises, readable.noisesToAdd.map { it.noise })
        for (pending in readable.noisesToAdd) {
            assertContentEquals(noiseAudio.getValue(pending.noise.fileName), pending.audio)
        }
    }
}
