package it.maicol07.gamerlogue.extensions

import androidx.compose.ui.platform.UriHandler

actual fun UriHandler.openURL(url: String) = openUri(url)
