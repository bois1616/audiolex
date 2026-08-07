package de.hexenwoche.audiolex

import java.io.File

/**
 * Desktop: alongside the database and the own corpus, under `~/.audiolex/`
 * (Backlog M4 "Eigene Störgeräusche", AC1) -- same directory family as
 * `getOwnCorpusDir`, its own subfolder.
 */
fun getOwnNoiseDir(): File =
    File(File(System.getProperty("user.home"), ".audiolex"), "eigene-stoergeraeusche").apply { mkdirs() }
