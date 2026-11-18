package it.maicol07.gamerlogue.services

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.Window
import it.maicol07.gamerlogue.ui.components.WebSession
import it.maicol07.gamerlogue.ui.components.rememberServiceWebViewHost
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.cancellation.CancellationException
import java.awt.Window as AwtWindow

/**
 * Background syncs on the desktop. JCEF only runs a page's scripts in a window that is shown, so each run gets a
 * real window placed off-screen by [BackgroundSyncWindow], which the app composes next to its main window.
 */
object DesktopBackgroundWebHost : BackgroundWebHost {
    /** The run waiting for the off-screen window; null when none is. */
    internal val request = MutableStateFlow<(suspend (WebSession) -> Unit)?>(null)

    override suspend fun <T> withSession(block: suspend (WebSession) -> T): T {
        val result = CompletableDeferred<T>()
        request.value = { session ->
            // Hand the outcome, a failure included, back to the caller waiting on another coroutine.
            try {
                result.complete(block(session))
            } catch (error: CancellationException) {
                result.cancel(error)
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                result.completeExceptionally(error)
            }
        }
        return try {
            result.await()
        } finally {
            request.value = null
        }
    }
}

private const val OFFSCREEN = -4000
private const val WIDTH = 1024
private const val HEIGHT = 768

/**
 * The window of [DesktopBackgroundWebHost]: shown while a run needs it, off-screen, out of the taskbar and never
 * taking the focus.
 */
@Composable
fun BackgroundSyncWindow() {
    val run by DesktopBackgroundWebHost.request.collectAsState()
    run?.let { block ->
        key(block) {
            Window(
                create = {
                    ComposeWindow().apply {
                        type = AwtWindow.Type.UTILITY
                        isUndecorated = true
                        focusableWindowState = false
                        setBounds(OFFSCREEN, OFFSCREEN, WIDTH, HEIGHT)
                    }
                },
                dispose = ComposeWindow::dispose,
            ) {
                rememberServiceWebViewHost(ABOUT_BLANK, onClose = {}, flow = block).webView(Modifier.fillMaxSize())
            }
        }
    }
}

private const val ABOUT_BLANK = "about:blank"
