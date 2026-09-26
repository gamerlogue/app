package it.maicol07.gamerlogue.extensions

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri
import co.touchlab.kermit.Logger

actual fun UriHandler.openURL(url: String) {
    // Implementation for Android to open URL in a custom tab
    val customTabsIntent = CustomTabsIntent.Builder().build()
    if (this !is AndroidUriHandler) {
        openUri(url)
        return
    }
    try {
        // Get context via reflection since it's internal
        val contextField = AndroidUriHandler::class.java.getDeclaredField("context")
        contextField.isAccessible = true
        val context = contextField.get(this) as android.content.Context
        customTabsIntent.launchUrl(context, url.toUri())
    } catch (e: NoSuchFieldException) {
        Logger.e(e) { "AndroidUriHandler has no context field anymore" }
        openUri(url)
    } catch (e: IllegalAccessException) {
        Logger.e(e) { "Failed to access context field in AndroidUriHandler" }
        openUri(url)
    } catch (e: ClassCastException) {
        Logger.e(e) { "Failed to cast context field in AndroidUriHandler" }
        openUri(url)
    }
}
