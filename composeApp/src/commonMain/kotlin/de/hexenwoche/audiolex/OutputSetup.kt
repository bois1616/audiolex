package de.hexenwoche.audiolex

import androidx.compose.runtime.Composable
import de.hexenwoche.audiolex.core.audio.OutputSetup

/**
 * What the detection currently sees: the [OutputSetup] the app acts on, and
 * the raw device types it derived that from.
 *
 * [routedDevices] exists for one reason (F-Droid-Tester chivalry, 2026-08-27):
 * when someone reports that the channel selection does nothing, the first
 * question is whether the app classified their headset as a stereo pair at
 * all -- and no amount of guessing from here answers it. Android names dozens
 * of output device types and a USB-C headset may report `TYPE_USB_HEADSET`
 * on one phone and `TYPE_USB_DEVICE` on the next, so the app has to say what
 * it saw. The channel test shows this line; nothing else does, because it is
 * a diagnosis, not a setting.
 *
 * Entries are human-readable and carry the numeric constant
 * ("USB_HEADSET (22)"), so a screenshot from a stranger's phone is enough to
 * settle it. An empty list means the platform answered nothing.
 */
data class OutputDiagnosis(val setup: OutputSetup, val routedDevices: List<String>)

/**
 * Observable source for the currently active [OutputSetup] (Backlog M4
 * "Kopfhörer-Bogen Batch A" AC2, ADR-0011 point 2/4). `@Composable` on an
 * `expect fun` was confirmed to compile cleanly on both targets with a
 * throwaway stub before this real implementation was built on top of it
 * (`./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinDesktop`,
 * see Umsetzungslog 2026-08-06) -- the documented fallback (platform object
 * built in `MainActivity`/`main.kt`, passed through `App()` like
 * `database`/`clock`) was not needed.
 *
 * Android reacts to hot-plug via `AudioManager.registerAudioDeviceCallback`
 * (ADR-0011 point 4); Desktop returns a constant (ADR-0011 point 6, no
 * detection there). Only the reading, not the value, is
 * platform-dependent -- [OutputSetup] itself stays in `:core` since it's
 * plain, platform-free state (ADR-0011 point 1/3).
 */
@Composable
expect fun rememberOutputDiagnosis(): OutputDiagnosis

/**
 * The setup alone, for the callers that only ever needed that (the two
 * training screens and the settings line). Goes through the same query as
 * the diagnosis, so the number the channel test displays and the decision
 * the training screens act on cannot disagree -- a second query would be a
 * second opinion.
 */
@Composable
fun rememberOutputSetup(): OutputSetup = rememberOutputDiagnosis().setup
