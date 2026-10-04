package it.maicol07.gamerlogue.ui.components.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

/** Desktop has no system share sheet: the viewer keeps "copy link" only. */
actual val isShareSupported: Boolean = false

@Composable
actual fun rememberShareUrl(): (url: String) -> Unit = remember { ::unsupportedShare }

@Composable
actual fun rememberSaveImage(): suspend (bytes: ByteArray, fileName: String) -> Boolean = remember { ::saveWithDialog }

private fun unsupportedShare(url: String): Unit =
    error("Sharing $url is not supported on desktop; check isShareSupported first")

private suspend fun saveWithDialog(bytes: ByteArray, fileName: String): Boolean {
    val file = FileKit.openFileSaver(
        suggestedName = fileName.substringBeforeLast('.'),
        defaultExtension = fileName.substringAfterLast('.', missingDelimiterValue = "jpg")
    ) ?: return false
    file.write(bytes)
    return true
}
