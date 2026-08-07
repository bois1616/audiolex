package de.hexenwoche.audiolex

/**
 * Desktop is the development target (AGENTS.md §7) and is not distributed --
 * there is no release/debug distinction to make here, and hiding the
 * diagnostic tool on the one target used for diagnosing would be backwards.
 */
actual fun isDebugBuild(): Boolean = true
