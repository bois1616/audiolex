package de.hexenwoche.audiolex

/**
 * Single source of truth for the app version, shown on the StartScreen so a
 * device test can always tell which build is running (Autor-Wunsch
 * 2026-07-13: eine Version je Batch, lückenlos nachvollziehbar).
 *
 * IMPORTANT: `composeApp/build.gradle.kts` carries the same two values as
 * literals (F-Droid's `checkupdates` reads them by regex and cannot evaluate
 * Gradle -- see the comment there) and asserts them against this file. Bump
 * *both* places; a mismatch fails the build before an APK exists, so the
 * displayed version and the APK version still cannot drift apart. Keep the
 * two `const val` lines below on their own line in this exact shape, and bump
 * per batch: VERSION_NAME one minor step (0.5.0 -> 0.6.0), VERSION_CODE +1
 * (Android requires it to increase monotonically).
 *
 * The patch place is for anything changed *after* a bump -- a device-test
 * finding, a correction to one's own work -- however small (0.26.0 ->
 * 0.26.1, Autor-Vorgabe 2026-08-07, AGENTS.md DoD §6). Two different builds
 * under one number make a device test unprovable, which is the one thing
 * this number exists to prevent.
 */
const val VERSION_NAME = "0.36.1"
const val VERSION_CODE = 46
