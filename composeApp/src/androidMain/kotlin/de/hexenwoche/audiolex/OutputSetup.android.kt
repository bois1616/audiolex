package de.hexenwoche.audiolex

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.hexenwoche.audiolex.core.audio.OutputSetup

/**
 * Android detection (Backlog M4 "Kopfhörer-Bogen Batch A" AC2/AC3,
 * ADR-0011 point 2/3/4): reads the current output devices once on entry and
 * again on every hot-plug event via `AudioManager.registerAudioDeviceCallback`,
 * so [de.hexenwoche.audiolex.EinstellungenScreen] reflects an inserted/removed
 * headphone live without the user leaving and re-entering the screen. The
 * callback is unregistered in the `DisposableEffect` cleanup so no callback
 * outlives the screen (the point the A53 sight-check explicitly probes).
 *
 * `:core` stays context-free (ADR-0004 precedent, `DatabaseBuilder.android.kt`)
 * -- this is the platform code ADR-0011 point 3 puts in `:composeApp`
 * instead.
 */
@Composable
actual fun rememberOutputSetup(): OutputSetup {
    val context = LocalContext.current
    var setup by remember { mutableStateOf(OutputSetup.HOERGERAET) }

    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        fun refresh() {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            setup = resolveOutputSetup(devices.asList())
        }
        refresh()

        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) = refresh()
            override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = refresh()
        }
        audioManager.registerAudioDeviceCallback(callback, /* handler = */ null)

        onDispose {
            audioManager.unregisterAudioDeviceCallback(callback)
        }
    }

    return setup
}

/**
 * Combines every currently reported output device into the single
 * governing [OutputSetup]. Taking "the first" reported device doesn't work:
 * Android practically always reports the built-in speaker alongside any
 * connected peripheral, so a naive first-match would see the speaker before
 * a plugged-in headphone on many devices. Instead: a real hearing aid
 * (`TYPE_HEARING_AID`) wins outright, even over a simultaneously reported
 * stereo-capable device -- ADR-0011's doubt rule ("im Zweifel HOERGERAET")
 * applies at this aggregate level too, so the rare case of a hearing aid
 * *and* e.g. wired headphones both being routable at once still doesn't
 * unlock channel work for the ear that's actually being trained. Failing
 * that, any stereo-capable device present is enough to call it
 * [OutputSetup.STEREO_KOPFHOERER]; with nothing but the speaker (or an
 * empty list) reported, it falls back to [OutputSetup.HOERGERAET].
 */
internal fun resolveOutputSetup(devices: List<AudioDeviceInfo>): OutputSetup {
    if (devices.any { it.type == AudioDeviceInfo.TYPE_HEARING_AID }) return OutputSetup.HOERGERAET
    val hasStereoCapableDevice = devices.any { classifyOutputDeviceType(it.type) == OutputSetup.STEREO_KOPFHOERER }
    return if (hasStereoCapableDevice) OutputSetup.STEREO_KOPFHOERER else OutputSetup.HOERGERAET
}

/**
 * Maps a single `AudioDeviceInfo.TYPE_*` constant to the [OutputSetup] it
 * implies in isolation -- exactly the detection table from ADR-0011 point 2.
 *
 * SDK-level check (AC3): `TYPE_HEARING_AID` is API 28, unconditionally safe
 * to reference at minSdk 29. The BLE types (`TYPE_BLE_HEADSET`,
 * `TYPE_BLE_SPEAKER`, `TYPE_BLE_BROADCAST`) are API 31/33 and map to
 * [OutputSetup.HOERGERAET] per ADR-0011 (LE-Audio earbuds and an LE-Audio
 * hearing aid can't be told apart, so this stays on the conservative side)
 * -- the same result the `else` branch below already returns for every
 * unlisted type, so their symbols are deliberately *not* referenced here:
 * doing so would need a `Build.VERSION.SDK_INT` guard for zero behavioural
 * gain.
 */
private fun classifyOutputDeviceType(type: Int): OutputSetup = when (type) {
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    -> OutputSetup.STEREO_KOPFHOERER

    else -> OutputSetup.HOERGERAET
}
