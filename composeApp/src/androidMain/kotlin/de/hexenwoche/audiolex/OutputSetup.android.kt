package de.hexenwoche.audiolex

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
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
 * so [de.hexenwoche.audiolex.EinstellungenScreen] and the channel test
 * reflect an inserted/removed headphone live without the user leaving and re-entering the screen. The
 * callback is unregistered in the `DisposableEffect` cleanup so no callback
 * outlives the screen (the point the A53 sight-check explicitly probes).
 *
 * `:core` stays context-free (ADR-0004 precedent, `DatabaseBuilder.android.kt`)
 * -- this is the platform code ADR-0011 point 3 puts in `:composeApp`
 * instead.
 */
@Composable
actual fun rememberOutputDiagnosis(): OutputDiagnosis {
    val context = LocalContext.current
    var diagnosis by remember { mutableStateOf(OutputDiagnosis(OutputSetup.HOERGERAET, emptyList())) }

    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        fun refresh() {
            diagnosis = currentOutputDiagnosis(audioManager)
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

    return diagnosis
}

/**
 * The attributes the training playback actually uses (`AndroidAudioSink`
 * builds its `AudioTrack` with exactly these). Routing is per-attribute on
 * Android, so asking with anything else could answer for a different route
 * than the one the user will hear.
 */
private val MEDIA_SPEECH_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
    .build()

/**
 * Asks *where media audio would actually go* rather than *what is plugged
 * in* -- the distinction that the A53 device test of v0.18.0 forced
 * (ADR-0011, Nachtrag 2026-08-06).
 *
 * The first cut enumerated connected devices and let a hearing aid win
 * outright. That misread the situation the author is actually in: his
 * hearing aid stays *connected* over Bluetooth essentially all the time, so
 * plugging in headphones left the app reporting "Hörgerät" even though
 * Android had already given the wired output priority and the hearing aid
 * had gone silent. `getAudioDevicesForAttributes` reflects that priority,
 * so no hearing-aid tie-break is needed here -- the routed set already *is*
 * the answer.
 *
 * The API is public only from Android 13 (API 33) -- lint caught an initial
 * guess of API 30, so the guard below is the toolchain's answer, not an
 * assumption. Below that, the enumerating [resolveOutputSetup] stays as the
 * fallback with its conservative tie-break; at minSdk 29 that covers API
 * 29-32. The test device (A53, API 36) always takes the routing path.
 *
 * Known limit: the device callback that triggers this only fires on devices
 * appearing/disappearing. Re-routing without a device change (picking
 * another output in the system's media switcher) is not observed.
 */
private fun currentOutputDiagnosis(audioManager: AudioManager): OutputDiagnosis {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val routed = audioManager.getAudioDevicesForAttributes(MEDIA_SPEECH_ATTRIBUTES)
        if (routed.isNotEmpty()) {
            val stereoRouted = routed.any { classifyOutputDeviceType(it.type) == OutputSetup.STEREO_KOPFHOERER }
            return OutputDiagnosis(
                setup = if (stereoRouted) OutputSetup.STEREO_KOPFHOERER else OutputSetup.HOERGERAET,
                routedDevices = routed.map { describeOutputDeviceType(it.type) },
            )
        }
    }
    // Fallback path: the enumerated devices are what the decision is made
    // from here, so they are also what gets reported.
    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).asList()
    return OutputDiagnosis(
        setup = resolveOutputSetup(devices),
        routedDevices = devices.map { describeOutputDeviceType(it.type) },
    )
}

/**
 * "USB_HEADSET (22)" for `AudioDeviceInfo.TYPE_USB_HEADSET`, and a bare
 * "TYPE 11" for anything this list doesn't name yet.
 *
 * Why the raw number travels along: it is the one part that cannot be
 * misread on its way through a bug report. The named types are the ones a
 * phone can plausibly route media to -- the ones actually worth telling
 * apart when someone says the channel selection does nothing. This is
 * diagnosis text, deliberately untranslated (the constant names are
 * Android's, and a German rendering of `TYPE_USB_DEVICE` would help nobody).
 *
 * The BLE constants (API 31/33) stay referenced by number rather than by
 * symbol, same reason as in [classifyOutputDeviceType]: naming them would
 * need an SDK guard for a label.
 */
internal fun describeOutputDeviceType(type: Int): String {
    val name = when (type) {
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "WIRED_HEADPHONES"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB_DEVICE"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB_ACCESSORY"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BLUETOOTH_A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BLUETOOTH_SCO"
        AudioDeviceInfo.TYPE_HEARING_AID -> "HEARING_AID"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "BUILTIN_SPEAKER"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "BUILTIN_SPEAKER_SAFE"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "BUILTIN_EARPIECE"
        AudioDeviceInfo.TYPE_LINE_ANALOG -> "LINE_ANALOG"
        AudioDeviceInfo.TYPE_AUX_LINE -> "AUX_LINE"
        AudioDeviceInfo.TYPE_DOCK -> "DOCK"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        26 -> "BLE_HEADSET"
        27 -> "BLE_SPEAKER"
        30 -> "BLE_BROADCAST"
        else -> return "TYPE $type"
    }
    return "$name ($type)"
}

/**
 * Fallback for API 29-32, where [currentOutputDiagnosis] cannot ask about
 * routing.
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
internal fun classifyOutputDeviceType(type: Int): OutputSetup = when (type) {
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    -> OutputSetup.STEREO_KOPFHOERER

    else -> OutputSetup.HOERGERAET
}
