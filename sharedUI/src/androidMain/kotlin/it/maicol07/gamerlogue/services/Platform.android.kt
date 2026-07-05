package it.maicol07.gamerlogue.services

import android.webkit.CookieManager
import com.parkwoocheol.composewebview.WebView

actual fun isServiceSyncSupported(): Boolean = true

actual fun configureServiceWebView(webView: WebView) {
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
}
