package de.hexenwoche.audiolex

/**
 * Thin platform boundary for the own-noise file storage (Backlog M4 "Eigene
 * Störgeräusche", AC1), the exact mirror of [OwnCorpusFiles]: plain
 * byte-level file operations only, keyed by file name within the directory
 * this instance was created for (`eigene-stoergeraeusche`). Everything above
 * this -- JSON parsing, id/filename generation, WAV encode/decode, the CRUD
 * flows -- is shared logic in [OwnNoiseRepository]; only java.io.File access
 * itself needs a platform actual, and the two actuals are near-identical for
 * the same accepted reason as [OwnCorpusFiles]' (no shared jvm-ish source
 * set in this project's target hierarchy).
 */
interface OwnNoiseFiles {
    /** The metadata file's full content, or null if it doesn't exist yet (nothing recorded or imported so far). */
    fun readMetadata(): String?

    /**
     * Writes [json] as the metadata file's entire new content, atomically:
     * temp file + rename, same crash-safety contract as
     * [OwnCorpusFiles.writeMetadataAtomic].
     */
    fun writeMetadataAtomic(json: String)

    /** Raw bytes of a noise recording, or null if the file is missing (quiet failure, not a crash). */
    fun readNoise(fileName: String): ByteArray?

    fun writeNoise(fileName: String, bytes: ByteArray)

    /** Best-effort delete; the caller doesn't distinguish "didn't exist" from "failed". */
    fun deleteNoise(fileName: String)
}

/** [dir] is an absolute directory path, as produced by the platform-specific `getOwnNoiseDir`. */
expect fun createOwnNoiseFiles(dir: String): OwnNoiseFiles
