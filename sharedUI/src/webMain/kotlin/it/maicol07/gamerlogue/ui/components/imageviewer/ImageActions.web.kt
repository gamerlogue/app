package it.maicol07.gamerlogue.ui.components.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download

private fun hasWebShare(): Boolean = js("typeof navigator.share === 'function'")

// A rejected share (the user closed the sheet) is not an error worth surfacing.
private fun webShare(url: String): Unit = js("navigator.share({ url: url }).catch(function () {})")

/** The Web Share API exists mostly on mobile browsers; elsewhere the viewer keeps "copy link" only. */
actual val isShareSupported: Boolean get() = hasWebShare()

@Composable
actual fun rememberShareUrl(): (url: String) -> Unit = remember { ::webShare }

@Composable
actual fun rememberSaveImage(): suspend (bytes: ByteArray, fileName: String) -> Boolean = remember { ::download }

private suspend fun download(bytes: ByteArray, fileName: String): Boolean {
    FileKit.download(bytes, fileName)
    return true
}
