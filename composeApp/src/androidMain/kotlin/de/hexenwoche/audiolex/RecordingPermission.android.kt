package de.hexenwoche.audiolex

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * `ActivityResultContracts.RequestPermission` is the standard way to request
 * a dangerous runtime permission from Compose -- there's no existing pattern
 * for this in the project, since this is the app's first permission of any
 * kind (ADR-0012).
 *
 * Distinguishing [RecordingPermissionStatus.DENIED] (dialog can still be
 * shown again) from [RecordingPermissionStatus.PERMANENTLY_DENIED] (Android
 * stopped showing it after a second denial) needs
 * `Activity.shouldShowRequestPermissionRationale`, which returns `false` for
 * *both* "never asked" and "permanently denied" -- so [hasRequestedBefore]
 * tracks the difference locally (Autor-Vorgabe 2026-08-06: a permanently
 * denied user must be pointed at the system settings, not left with a
 * button that visibly does nothing).
 *
 * [hasRequestedBefore] is plain `remember`, not `rememberSaveable` -- it
 * resets on a config change/process restart, which only means a
 * permanently-denied user briefly sees "request" again instead of the
 * settings link until they tap it once (`checkSelfPermission`/`granted`
 * itself is always read fresh, so it never misreports as granted). Accepted
 * for a Dev-only raw test; not worth a new dependency for.
 */
@Composable
actual fun rememberRecordingPermissionState(): RecordingPermissionState {
    val context = LocalContext.current
    var hasRequestedBefore by remember { mutableStateOf(false) }
    // Bumped after every dialog result so this composable re-reads
    // checkSelfPermission() below -- that call itself isn't observable
    // Compose state, so without this the UI wouldn't notice a grant/denial.
    var recheckToken by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasRequestedBefore = true
        recheckToken++
    }

    recheckToken

    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    val activity = context.findActivity()
    val canShowRationale = activity
        ?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        ?: false

    val status = when {
        granted -> RecordingPermissionStatus.GRANTED
        !hasRequestedBefore -> RecordingPermissionStatus.NOT_REQUESTED
        canShowRationale -> RecordingPermissionStatus.DENIED
        else -> RecordingPermissionStatus.PERMANENTLY_DENIED
    }

    return remember(status) {
        object : RecordingPermissionState {
            override val status: RecordingPermissionStatus = status

            override fun request() {
                hasRequestedBefore = true
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }

            override fun openSystemSettings() {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                if (activity != null) {
                    activity.startActivity(intent)
                } else {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
    }
}

/** Unwraps a possibly-wrapped [Context] (e.g. a themed Compose context) to find its [Activity], if any. */
private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
