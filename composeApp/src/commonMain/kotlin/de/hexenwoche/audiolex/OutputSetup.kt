package de.hexenwoche.audiolex

import androidx.compose.runtime.Composable
import de.hexenwoche.audiolex.core.audio.OutputSetup

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
expect fun rememberOutputSetup(): OutputSetup
