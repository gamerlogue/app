package it.maicol07.gamerlogue.services

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
