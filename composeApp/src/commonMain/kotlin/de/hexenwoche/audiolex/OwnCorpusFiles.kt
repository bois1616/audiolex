package de.hexenwoche.audiolex

/**
 * Thin platform boundary for the own-corpus file storage (Backlog
 * Eigen-Korpus Batch B, ADR-0012 Nachtrag): plain byte-level file operations
 * only, keyed by file name within the directory this instance was created
 * for. Everything above this -- JSON parsing, id/filename generation, WAV
 * encode/decode, the CRUD flows -- is shared logic in [OwnCorpusRepository],
 * written once in commonMain; only java.io.File access itself needs a
 * platform actual, mirroring the [de.hexenwoche.audiolex.core.audio.AudioSink]/
 * [de.hexenwoche.audiolex.core.audio.AudioSource] boundary (ADR-0003).
 * Android and Desktop actuals both just wrap `java.io.File` -- there is no
 * shared jvm-ish source set between them in this project's target hierarchy
 * (see `DatabaseBuilder.android.kt`/`.desktop.kt`, the same precedent), so a
 * small amount of near-identical code in two files is accepted here too
 * rather than restructuring the build for it.
 */
interface OwnCorpusFiles {
    /** The metadata file's full content, or null if it doesn't exist yet (first launch). */
    fun readMetadata(): String?

    /**
     * Writes [json] as the metadata file's entire new content, atomically
     * (AC2): written to a temporary file first, then renamed over the real
     * file, so a crash mid-write leaves either the old or the new content
     * intact -- never a truncated file. The collection is the app's only
     * irreplaceable data (ADR-0012 Nachtrag).
     */
    fun writeMetadataAtomic(json: String)

    /** Raw bytes of a recording, or null if the file is missing (Backlog AC5: quiet failure, not a crash). */
    fun readRecording(fileName: String): ByteArray?

    fun writeRecording(fileName: String, bytes: ByteArray)

    /** Best-effort delete; the caller doesn't distinguish "didn't exist" from "failed" (Backlog AC5). */
    fun deleteRecording(fileName: String)
}

/** [dir] is an absolute directory path, as produced by the platform-specific `getOwnCorpusDir`. */
expect fun createOwnCorpusFiles(dir: String): OwnCorpusFiles
