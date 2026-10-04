package it.maicol07.gamerlogue.ui.components.imageviewer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.saveImageToGallery
import kotlinx.coroutines.CompletableDeferred

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

/** The pending storage-permission answer, completed by the launcher callback. */
private class PermissionRequest {
    var answer: CompletableDeferred<Boolean>? = null
}

@Composable
actual fun rememberSaveImage(): suspend (bytes: ByteArray, fileName: String) -> Boolean {
    val context = LocalContext.current
    val request = remember { PermissionRequest() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        request.answer?.complete(granted)
    }
    return remember(context, launcher) {
        { bytes, fileName ->
            val allowed = !needsStoragePermission(context) || CompletableDeferred<Boolean>().let { answer ->
                request.answer = answer
                launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                answer.await()
            }
            if (allowed) FileKit.saveImageToGallery(bytes, fileName).getOrThrow()
            allowed
        }
    }
}

/** Below Android 10 the gallery is shared storage, writable only with the legacy storage permission. */
private fun needsStoragePermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
        PackageManager.PERMISSION_GRANTED
