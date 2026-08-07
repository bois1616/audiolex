package de.hexenwoche.audiolex

import androidx.compose.runtime.Composable

/**
 * Where a backup file goes and comes from (ADR-0013 points 1 and 2), as a
 * platform boundary at the UI layer -- same shape as
 * [rememberRecordingPermissionState]: the APIs involved (`MediaStore`,
 * `ActivityResultContracts`) are Android-Compose-specific and `:core` has no
 * Compose dependency, so this lives in `:composeApp`.
 *
 * The archive's *contents* are none of this interface's business: building
 * and merging happen in `:core` (`buildBackup`/`readBackup`) where they are
 * testable without a filesystem. This only moves bytes in and out.
 */
interface BackupActions {
    /**
     * Writes [bytes] as [fileName] into the user's documents folder, outside
     * the app-private area (ADR-0013 point 2: a backup that disappears with
     * the app isn't one). Returns a location to show the user, or null if it
     * failed. No permission is required for this on Android 29+, which is
     * minSdk here.
     */
    suspend fun saveToDocuments(fileName: String, bytes: ByteArray): String?

    /**
     * Opens the system file picker and hands the chosen file's bytes to
     * [onResult] -- null when the user cancelled or the file couldn't be
     * read. A picker is unavoidable on the import side: the app cannot guess
     * which backup is meant (ADR-0013, Alternativen).
     */
    fun pickArchive(onResult: (ByteArray?) -> Unit)
}

@Composable
expect fun rememberBackupActions(): BackupActions

/** `audiolex-sicherung-2026-08-07-1432.zip` -- timestamped so a second backup never overwrites the first (AC1). */
fun backupFileName(nowEpochMillis: Long): String =
    "audiolex-sicherung-${formatBackupTimestamp(nowEpochMillis)}.zip"
