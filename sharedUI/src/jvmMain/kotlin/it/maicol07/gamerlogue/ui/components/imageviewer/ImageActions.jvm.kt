package it.maicol07.gamerlogue.ui.components.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Desktop has no system share sheet: the viewer keeps "copy link" only. */
actual val isShareSupported: Boolean = false

@Composable
actual fun rememberShareUrl(): (url: String) -> Unit = remember { ::unsupportedShare }

@Composable
actual fun rememberSaveImage(): suspend (url: String, fileName: String) -> Boolean = remember { ::saveWithDialog }

private fun unsupportedShare(url: String): Unit =
    error("Sharing $url is not supported on desktop; check isShareSupported first")

private suspend fun saveWithDialog(url: String, fileName: String): Boolean {
    // Modal AWT dialog: it pumps events itself, so running it on the UI thread does not freeze the app.
    val dialog = FileDialog(null as Frame?, null, FileDialog.SAVE).apply {
        file = fileName
        isVisible = true
    }
    val directory = dialog.directory ?: return false
    val chosen = dialog.file ?: return false
    val bytes = downloadImageBytes(url)
    withContext(Dispatchers.IO) { File(directory, chosen).writeBytes(bytes) }
    return true
}
