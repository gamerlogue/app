package it.maicol07.gamerlogue.services

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.parkwoocheol.composewebview.WebView

// Stores block cross-origin iframe embedding / JS injection on the web — feature unavailable here.
actual fun isServiceSyncSupported(): Boolean = false

// Service sync is unsupported on web; no WebView to configure.
actual fun configureServiceWebView(webView: WebView) = Unit

// Feature unavailable on web; nothing to bridge.
@Composable
actual fun webViewNestedScrollModifier(): Modifier = Modifier
