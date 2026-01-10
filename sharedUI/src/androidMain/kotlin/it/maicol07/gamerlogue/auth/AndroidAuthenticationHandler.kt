package it.maicol07.gamerlogue.auth

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import org.koin.compose.koinInject

class AndroidAuthenticationHandler(
    private val context: Context,
    authProvider: AuthTokenProvider
) : AuthenticationHandler(authProvider) {
    override fun login() {
        val redirectUri = "gamerlogue://auth/callback"
        val intent = Intent(
            Intent.ACTION_VIEW,
            getAuthUrl(redirectUri).toUri()
        )
        context.startActivity(intent)
    }
}

@Composable
actual fun rememberAuthenticationHandler(): AuthenticationHandler {
    val context = LocalContext.current
    val authProvider = koinInject<AuthTokenProvider>()
    return remember(context) { AndroidAuthenticationHandler(context, authProvider) }
}
