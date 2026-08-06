package de.hexenwoche.audiolex

import java.io.File

actual fun createOwnCorpusFiles(dir: String): OwnCorpusFiles = JavaIoOwnCorpusFiles(dir)

/**
 * `java.io.File`-based implementation (Backlog Eigen-Korpus Batch B,
 * ADR-0012 Nachtrag) -- identical on Android and Desktop, see the
 * [OwnCorpusFiles] doc for why this isn't factored into one shared file.
 */
private class JavaIoOwnCorpusFiles(dirPath: String) : OwnCorpusFiles {
    private val dir = File(dirPath).apply { mkdirs() }
    private val metadataFile = File(dir, "eintraege.json")

    override fun readMetadata(): String? =
        if (metadataFile.exists()) metadataFile.readText() else null

    override fun writeMetadataAtomic(json: String) {
        // Written inside `dir` (not the system temp dir) so the rename below
        // stays on the same filesystem -- that's what makes it atomic (AC2):
        // a crash between these two lines leaves the old metadataFile
        // untouched, since the new content only ever exists under the temp
        // name until the rename completes.
        val tempFile = File.createTempFile("eintraege-", ".json.tmp", dir)
        tempFile.writeText(json)
        if (!tempFile.renameTo(metadataFile)) {
            tempFile.delete()
            error("could not write eintraege.json (rename failed)")
        }
    }

    override fun readRecording(fileName: String): ByteArray? {
        val file = File(dir, fileName)
        return if (file.exists()) file.readBytes() else null
    }

    override fun writeRecording(fileName: String, bytes: ByteArray) {
        File(dir, fileName).writeBytes(bytes)
    }

    override fun deleteRecording(fileName: String) {
        File(dir, fileName).delete()
    }

    override fun recordingExists(fileName: String): Boolean = File(dir, fileName).exists()
}
