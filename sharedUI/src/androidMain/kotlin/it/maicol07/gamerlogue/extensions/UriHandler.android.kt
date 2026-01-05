package it.maicol07.gamerlogue.extensions

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri

@Throws(NoSuchFieldException::class)
actual fun UriHandler.openURL(url: String) {
    // Implementation for Android to open URL in a custom tab
    val customTabsIntent = CustomTabsIntent.Builder().build()
    if (this is AndroidUriHandler) {
        // Get context via reflection since it's internal
        val contextField = AndroidUriHandler::class.java.getDeclaredField("context")
        contextField.isAccessible = true
        val context = contextField.get(this) as android.content.Context
        customTabsIntent.launchUrl(context, url.toUri())
    }
}
