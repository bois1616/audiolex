package de.hexenwoche.audiolex

import android.content.Context
import java.io.File

/**
 * Android: app-private directory for the user's own noise loops and their
 * metadata JSON (Backlog M4 "Eigene Störgeräusche", AC1) -- the
 * `getOwnCorpusDir` pattern. `context.filesDir` (not the cache dir) so the
 * OS never reclaims it under storage pressure: own noises are irreplaceable
 * user content like the own corpus (ADR-0013 zweiter Nachtrag).
 */
fun getOwnNoiseDir(context: Context): File =
    File(context.applicationContext.filesDir, "eigene-stoergeraeusche").apply { mkdirs() }
