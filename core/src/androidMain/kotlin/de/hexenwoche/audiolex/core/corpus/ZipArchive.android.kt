package de.hexenwoche.audiolex.core.corpus

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

actual fun packArchive(files: List<ArchiveFile>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        for (file in files) {
            zip.putNextEntry(ZipEntry(file.path))
            zip.write(file.bytes)
            zip.closeEntry()
        }
    }
    return out.toByteArray()
}

actual fun unpackArchive(bytes: ByteArray): List<ArchiveFile>? = try {
    val files = mutableListOf<ArchiveFile>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            // readBytes() on a ZipInputStream stops at the current entry's
            // end, not the stream's -- that's what makes the loop work.
            if (!entry.isDirectory) files += ArchiveFile(entry.name, zip.readBytes())
            entry = zip.nextEntry
        }
    }
    files.takeIf { it.isNotEmpty() }
} catch (e: IOException) {
    // ZipException (bad header, corrupt entry) is an IOException; a file that
    // simply isn't a ZIP usually yields no entries at all and falls out via
    // the isNotEmpty() check above rather than by throwing.
    null
}
