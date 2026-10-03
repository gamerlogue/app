package it.maicol07.gamerlogue.ui.components.imageviewer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val ALBUM = "Gamerlogue"

actual val isShareSupported: Boolean = true

@Composable
actual fun rememberShareUrl(): (url: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
            context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
actual fun rememberSaveImage(): suspend (url: String, fileName: String) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { url, fileName ->
            val bytes = downloadImageBytes(url)
            withContext(Dispatchers.IO) { saveToPictures(context, bytes, fileName) }
            true
        }
    }
}

private fun saveToPictures(context: Context, bytes: ByteArray, fileName: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore refused to create $fileName")
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: throw IOException("Cannot open MediaStore output for $fileName")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    } else {
        // ponytail: Android 8-9 would need WRITE_EXTERNAL_STORAGE for the shared Pictures folder; the
        // app's own Pictures folder needs no permission and is still indexed by the media scanner.
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw IOException("External storage unavailable")
        val file = File(dir, fileName).apply { writeBytes(bytes) }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
    }
}
