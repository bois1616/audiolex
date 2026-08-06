package de.hexenwoche.audiolex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Desktop has no OS-level "app permission" concept for this app (AC3 is
 * Android-specific, ADR-0012) -- whether recording actually works here
 * depends only on whether a usable microphone input line exists at all,
 * which surfaces as an exception from
 * [de.hexenwoche.audiolex.core.audio.AudioSource.record] itself, not a
 * permission dialog. So this side always reports [RecordingPermissionStatus.GRANTED].
 */
@Composable
actual fun rememberRecordingPermissionState(): RecordingPermissionState =
    remember {
        object : RecordingPermissionState {
            override val status: RecordingPermissionStatus = RecordingPermissionStatus.GRANTED
            override fun request() = Unit
            override fun openSystemSettings() = Unit
        }
    }
