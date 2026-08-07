package de.hexenwoche.audiolex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.io.IOException
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Desktop is the dev target (AGENTS.md §7), so this is the lean, honest
 * implementation AC7 asks for rather than a polished one: a plain file under
 * the user's Documents folder, and Swing's file chooser for picking. The
 * chooser runs on the calling (AWT event) thread because that is where Swing
 * dialogs belong; only reading the chosen file's bytes moves off it.
 */
@Composable
actual fun rememberBackupActions(): BackupActions {
    val scope = rememberCoroutineScope()
    return remember {
        object : BackupActions {
            override suspend fun saveToDocuments(fileName: String, bytes: ByteArray): String? =
                withContext(Dispatchers.IO) {
                    try {
                        val dir = documentsDir().apply { mkdirs() }
                        val file = File(dir, fileName)
                        file.writeBytes(bytes)
                        file.absolutePath
                    } catch (e: IOException) {
                        null
                    }
                }

            override fun pickArchive(onResult: (ByteArray?) -> Unit) {
                val chooser = JFileChooser(documentsDir()).apply {
                    dialogTitle = "Sicherung wählen"
                    fileFilter = FileNameExtensionFilter("AudioLex-Sicherung (*.zip)", "zip")
                }
                if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                    onResult(null)
                    return
                }
                val file = chooser.selectedFile
                scope.launch {
                    val bytes = withContext(Dispatchers.IO) {
                        try {
                            file.readBytes()
                        } catch (e: IOException) {
                            null
                        }
                    }
                    onResult(bytes)
                }
            }
        }
    }
}

/**
 * `~/Documents`, created if absent. Deliberately not chasing the localized
 * XDG name (`~/Dokumente` and friends): the export reports the absolute path
 * it actually used, so the user is never left guessing where the file went,
 * and this target exists for development rather than daily use.
 */
private fun documentsDir(): File = File(System.getProperty("user.home"), "Documents")
