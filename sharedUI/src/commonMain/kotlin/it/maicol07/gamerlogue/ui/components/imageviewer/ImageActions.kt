package it.maicol07.gamerlogue.ui.components.imageviewer

import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes

/** Whether the platform has a native share sheet; elsewhere the viewer offers "copy link" alone. */
expect val isShareSupported: Boolean

/** Opens the platform share sheet with the image [url][String]. */
@Composable
expect fun rememberShareUrl(): (url: String) -> Unit

/**
 * Saves image bytes as the given file name where the platform keeps user images: the gallery on
 * Android (asking for storage access first on Android 8-9), a save dialog on desktop, a download on
 * web. Returns false when the user cancelled or denied access; failures throw.
 */
@Composable
expect fun rememberSaveImage(): suspend (bytes: ByteArray, fileName: String) -> Boolean

/** Plain client for image bytes: none of the API clients' auth or JSON setup applies to the image CDN. */
private val imageHttpClient by lazy { HttpClient { expectSuccess = true } }

internal suspend fun downloadImageBytes(url: String): ByteArray = imageHttpClient.get(url).readRawBytes()

/** File name for a saved image: the last URL segment, e.g. `co1wyy.jpg`. */
internal fun imageFileName(url: String): String = url.substringAfterLast('/').substringBefore('?')
