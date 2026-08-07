package de.hexenwoche.audiolex.core.corpus

/**
 * ZIP packing/unpacking as a platform boundary (Backlog "Sicherung eigener
 * Aufnahmen", AC8: the archive work belongs in `:core` and stays jvm-testable,
 * only file *access* is platform-specific).
 *
 * Both actuals wrap `java.util.zip` and are near-identical. That duplication
 * is the same trade this project already made for
 * [de.hexenwoche.audiolex.core.audio.AudioSink] and `OwnCorpusFiles`: there
 * is no shared jvm-ish source set between androidMain and jvmMain in this
 * target hierarchy, and a small amount of repeated code is cheaper than
 * restructuring the build for it.
 */
expect fun packArchive(files: List<ArchiveFile>): ByteArray

/**
 * Reads an archive's files, or null if [bytes] isn't a readable ZIP at all
 * (AC3: a foreign or damaged file gets a quiet answer, never an exception
 * escaping into a screen). An empty ZIP also yields null -- there is nothing
 * to distinguish it from "not an archive" that would help the user.
 */
expect fun unpackArchive(bytes: ByteArray): List<ArchiveFile>?
