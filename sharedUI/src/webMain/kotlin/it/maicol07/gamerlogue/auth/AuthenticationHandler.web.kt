package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.koinInject

@Composable
actual fun rememberAuthenticationHandler(): AuthenticationHandler {
    val authProvider = koinInject<AuthTokenProvider>()
    return remember(authProvider) {
        object : AuthenticationHandler(authProvider) {
            override fun login() {
                // TODO: web login (redirect to auth URL)
            }
        }
    }
}
