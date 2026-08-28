package de.hexenwoche.audiolex

import androidx.compose.runtime.Composable
import de.hexenwoche.audiolex.core.audio.OutputSetup

/**
 * Desktop: no detection (ADR-0011 point 6). Playback goes through `paplay`
 * (ADR-0003); a reliable device query would need extra external process
 * calls the dev-target doesn't warrant. Desktop is documented as pauschal
 * stereo-capable rather than silently guessing -- it's a dev target, not a
 * verification platform (device tests run on the Galaxy A53).
 */
@Composable
actual fun rememberOutputDiagnosis(): OutputDiagnosis = OutputDiagnosis(
    setup = OutputSetup.STEREO_KOPFHOERER,
    routedDevices = emptyList(),
    // Nothing is detected here, so nothing is claimed: `false` means "no
    // microphone headset known", not "there is none".
    headsetHasMicrophone = false,
)
