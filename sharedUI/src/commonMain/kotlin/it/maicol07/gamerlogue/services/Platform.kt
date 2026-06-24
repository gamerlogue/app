package it.maicol07.gamerlogue.services

/**
 * Whether the linked-services WebView automation works on this platform.
 *
 * False on web: the WebView is a cross-origin iframe there, so stores block embedding/JS injection.
 * Users are pointed to the Android/desktop app instead.
 */
expect fun isServiceSyncSupported(): Boolean
