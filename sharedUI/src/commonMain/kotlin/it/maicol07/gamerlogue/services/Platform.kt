package it.maicol07.gamerlogue.services

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.parkwoocheol.composewebview.WebView

/**
 * Whether the linked-services WebView automation works on this platform.
 *
 * False on web: the WebView is a cross-origin iframe there, so stores block embedding/JS injection.
 * Users are pointed to the Android/desktop app instead.
 */
expect fun isServiceSyncSupported(): Boolean

/**
 * Platform hook run right after the service-sync WebView is created.
 *
 * Android disables third-party cookies by default, which breaks store OAuth logins that post across
 * origins (e.g. PSN sign-in from my.account.sony.com to ca.account.sony.com — surfaces as a CORS error).
 */
expect fun configureServiceWebView(webView: WebView)

/**
 * Modifier that lets the service-sync WebView cooperate with the surrounding draggable bottom sheet:
 * on Android it bridges the WebView's nested scrolling into Compose, so the page scrolls its own content
 * and only the leftover drag moves the sheet (keeping the sheet naturally draggable). No-op elsewhere.
 */
@Composable
expect fun webViewNestedScrollModifier(): Modifier
