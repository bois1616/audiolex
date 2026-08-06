package de.hexenwoche.audiolex

import android.content.Context
import java.io.File

/**
 * Android: app-private directory for own recordings and their metadata JSON
 * (Backlog Eigen-Korpus Batch B, ADR-0012 Nachtrag) -- exactly the
 * `getDatabaseBuilder` pattern, called once in `MainActivity` and the result
 * handed down through `App()`. `context.filesDir` (not the cache dir) so the
 * OS never reclaims it under storage pressure -- own recordings are the
 * app's only irreplaceable data.
 */
fun getOwnCorpusDir(context: Context): File =
    File(context.applicationContext.filesDir, "eigene-aufnahmen").apply { mkdirs() }
