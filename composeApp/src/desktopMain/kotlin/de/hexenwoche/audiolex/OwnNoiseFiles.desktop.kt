package de.hexenwoche.audiolex

import java.io.File

actual fun createOwnNoiseFiles(dir: String): OwnNoiseFiles = JavaIoOwnNoiseFiles(dir)

/**
 * `java.io.File`-based implementation (Backlog M4 "Eigene Störgeräusche",
 * AC1) -- identical on Android and Desktop, see the [OwnNoiseFiles] doc for
 * why this isn't factored into one shared file.
 */
private class JavaIoOwnNoiseFiles(dirPath: String) : OwnNoiseFiles {
    private val dir = File(dirPath).apply { mkdirs() }
    private val metadataFile = File(dir, "geraeusche.json")

    override fun readMetadata(): String? =
        if (metadataFile.exists()) metadataFile.readText() else null

    override fun writeMetadataAtomic(json: String) {
        // Written inside `dir` (not the system temp dir) so the rename below
        // stays on the same filesystem -- that's what makes it atomic: a
        // crash between these two lines leaves the old metadataFile
        // untouched, since the new content only ever exists under the temp
        // name until the rename completes.
        val tempFile = File.createTempFile("geraeusche-", ".json.tmp", dir)
        tempFile.writeText(json)
        if (!tempFile.renameTo(metadataFile)) {
            tempFile.delete()
            error("could not write geraeusche.json (rename failed)")
        }
    }

    override fun readNoise(fileName: String): ByteArray? {
        val file = File(dir, fileName)
        return if (file.exists()) file.readBytes() else null
    }

    override fun writeNoise(fileName: String, bytes: ByteArray) {
        File(dir, fileName).writeBytes(bytes)
    }

    override fun deleteNoise(fileName: String) {
        File(dir, fileName).delete()
    }
}
