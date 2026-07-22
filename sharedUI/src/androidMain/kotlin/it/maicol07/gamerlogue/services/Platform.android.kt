package it.maicol07.gamerlogue.services

import android.webkit.CookieManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import com.parkwoocheol.composewebview.WebView

actual fun isServiceSyncSupported(): Boolean = true

actual fun configureServiceWebView(webView: WebView) {
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    // Let the WebView drive Compose nested scroll so it scrolls inside the draggable bottom sheet.
    webView.isNestedScrollingEnabled = true
}

@Composable
actual fun webViewNestedScrollModifier(): Modifier =
    Modifier.nestedScroll(rememberNestedScrollInteropConnection())
