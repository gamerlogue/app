package it.maicol07.gamerlogue.ui.components.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.await
import kotlin.js.Promise

private fun hasWebShare(): Boolean = js("typeof navigator.share === 'function'")

// A rejected share (the user closed the sheet) is not an error worth surfacing.
private fun webShare(url: String): Unit = js("navigator.share({ url: url }).catch(function () {})")

// The image CDN allows any origin, so the bytes can be fetched and handed to a download link.
private fun browserDownload(url: String, fileName: String): Promise<JsAny?> = js(
    """fetch(url).then(function (response) {
        if (!response.ok) throw new Error('HTTP ' + response.status + ' for ' + url);
        return response.blob();
    }).then(function (blob) {
        var link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = fileName;
        link.click();
        URL.revokeObjectURL(link.href);
    })"""
)

/** The Web Share API exists mostly on mobile browsers; elsewhere the viewer keeps "copy link" only. */
actual val isShareSupported: Boolean get() = hasWebShare()

@Composable
actual fun rememberShareUrl(): (url: String) -> Unit = remember { ::webShare }

@Composable
actual fun rememberSaveImage(): suspend (url: String, fileName: String) -> Boolean = remember { ::saveInBrowser }

private suspend fun saveInBrowser(url: String, fileName: String): Boolean {
    browserDownload(url, fileName).await<JsAny?>()
    return true
}
