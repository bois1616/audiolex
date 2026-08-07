package de.hexenwoche.audiolex

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * `MediaStore` with `RELATIVE_PATH` is the berechtigungsfreie way to put a
 * file into the shared Documents folder from API 29 on (ADR-0013 point 2).
 *
 * API levels checked against the SDK's own `api-versions.xml` rather than
 * assumed (Projektpraxis after the `getAudioDevicesForAttributes` `NewApi`
 * failure): `RELATIVE_PATH`, `IS_PENDING` and `VOLUME_EXTERNAL_PRIMARY` are
 * all `since=29`, exactly minSdk; `Environment.DIRECTORY_DOCUMENTS` is
 * `since=19` and `MediaStore.Files.getContentUri(String)` `since=11`. So the
 * primary route of AC1 holds and the `ACTION_CREATE_DOCUMENT` fallback the
 * item allows for is not needed.
 */
@Composable
actual fun rememberBackupActions(): BackupActions {
    val context = LocalContext.current
    // The picker's result arrives in a callback that can't take parameters,
    // so the caller's continuation is parked here for the duration.
    val pending = remember { mutableStateOf<((ByteArray?) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val onResult = pending.value
        pending.value = null
        val bytes = uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            }
        }
        onResult?.invoke(bytes)
    }

    return remember(context) {
        object : BackupActions {
            override suspend fun saveToDocuments(fileName: String, bytes: ByteArray): String? =
                withContext(Dispatchers.IO) { writeToDocuments(context, fileName, bytes) }

            override fun pickArchive(onResult: (ByteArray?) -> Unit) {
                pending.value = onResult
                // "*/*" rather than a ZIP filter on purpose: file providers
                // disagree about a .zip's MIME type (application/zip vs.
                // octet-stream), and a filter that greys out the user's own
                // backup is worse than opening the wrong file -- AC3 already
                // answers a wrong file with a quiet message.
                launcher.launch(arrayOf("*/*"))
            }
        }
    }
}

/**
 * Writes through `IS_PENDING`, so the file only becomes visible to other apps
 * once it is complete -- a half-written backup should never be something a
 * user can pick up and trust. On any failure the pending row is deleted
 * again rather than left behind as a zero-byte ghost in their Documents.
 */
private fun writeToDocuments(context: Context, fileName: String, bytes: ByteArray): String? {
    val resolver = context.contentResolver
    val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

    val uri = try {
        resolver.insert(collection, values)
    } catch (e: IllegalArgumentException) {
        null
    } ?: return null

    return try {
        val written = resolver.openOutputStream(uri)?.use { it.write(bytes); true } ?: false
        if (!written) {
            resolver.delete(uri, null, null)
            return null
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        "${Environment.DIRECTORY_DOCUMENTS}/$fileName"
    } catch (e: IOException) {
        resolver.delete(uri, null, null)
        null
    } catch (e: SecurityException) {
        resolver.delete(uri, null, null)
        null
    }
}
