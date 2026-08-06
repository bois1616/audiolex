package de.hexenwoche.audiolex

import java.io.File

/**
 * Desktop: alongside the database, under `~/.audiolex/` (Backlog Eigen-Korpus
 * Batch B, ADR-0012 Nachtrag) -- same directory family as
 * `getDatabaseBuilder`, but a subfolder rather than the SQLite file itself.
 */
fun getOwnCorpusDir(): File =
    File(File(System.getProperty("user.home"), ".audiolex"), "eigene-aufnahmen").apply { mkdirs() }
