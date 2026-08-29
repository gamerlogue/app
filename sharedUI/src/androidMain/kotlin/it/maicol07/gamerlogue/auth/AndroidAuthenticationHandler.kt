package it.maicol07.gamerlogue.auth

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import org.koin.compose.koinInject

class AndroidAuthenticationHandler(
    private val context: Context,
    authProvider: AuthTokenProvider
) : AuthenticationHandler(authProvider) {
    override fun login() {
        val redirectUri = "gamerlogue://auth/callback"
        // A Custom Tab keeps the flow in a browser the user can inspect (URL bar, real cert state) and
        // shares the browser's cookie jar, instead of handing the auth URL to whatever app happens to
        // claim ACTION_VIEW. It falls back to the default browser when no provider supports it.
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .apply { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            .launchUrl(context, getAuthUrl(redirectUri).toUri())
    }
}

@Composable
actual fun rememberAuthenticationHandler(): AuthenticationHandler {
    val context = LocalContext.current
    val authProvider = koinInject<AuthTokenProvider>()
    return remember(context) { AndroidAuthenticationHandler(context, authProvider) }
}
