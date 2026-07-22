package it.maicol07.gamerlogue.services

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.parkwoocheol.composewebview.WebView

actual fun isServiceSyncSupported(): Boolean = true

// CEF accepts third-party cookies by default; nothing to configure.
actual fun configureServiceWebView(webView: WebView) = Unit

// No Compose↔native nested-scroll interop for the CEF surface; leave the sheet swipe as-is.
@Composable
actual fun webViewNestedScrollModifier(): Modifier = Modifier
