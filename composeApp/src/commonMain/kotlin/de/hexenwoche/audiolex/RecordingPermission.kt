package de.hexenwoche.audiolex

import androidx.compose.runtime.Composable

/**
 * Mirrors the [de.hexenwoche.audiolex.core.audio.AudioSink]/[de.hexenwoche.audiolex.core.audio.AudioSource]
 * platform-boundary pattern (ADR-0003) at the UI layer, same as
 * [rememberOutputSetup]: `RECORD_AUDIO` is an Android runtime permission
 * with no Desktop equivalent, and the APIs to request it
 * (`ActivityResultContracts`/`rememberLauncherForActivityResult`) are
 * Android-Compose-specific -- `:core` has no Compose dependency, so this
 * lives in `:composeApp` instead (Backlog Eigen-Korpus Batch A, AC3).
 */
interface RecordingPermissionState {
    val status: RecordingPermissionStatus

    /**
     * Triggers the system permission dialog. A no-op once
     * [RecordingPermissionStatus.PERMANENTLY_DENIED] -- Android stops
     * showing the dialog after a second denial, so callers should offer
     * [openSystemSettings] instead of calling [request] again.
     */
    fun request()

    /** Opens this app's system settings screen (only meaningful once [RecordingPermissionStatus.PERMANENTLY_DENIED]). */
    fun openSystemSettings()
}

enum class RecordingPermissionStatus {
    GRANTED,
    NOT_REQUESTED,
    DENIED,

    /** Second denial: Android no longer shows the dialog -- [RecordingPermissionState.openSystemSettings] is the only way forward. */
    PERMANENTLY_DENIED,
}

@Composable
expect fun rememberRecordingPermissionState(): RecordingPermissionState
