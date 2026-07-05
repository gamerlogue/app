package it.maicol07.gamerlogue.services

import com.parkwoocheol.composewebview.WebView

actual fun isServiceSyncSupported(): Boolean = true

// CEF accepts third-party cookies by default; nothing to configure.
actual fun configureServiceWebView(webView: WebView) = Unit
