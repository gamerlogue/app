package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import it.maicol07.gamerlogue.BuildConfig
import it.maicol07.gamerlogue.data.User
import it.maicol07.spraypaintkt.JsonApiException
import kotlinx.browser.window
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

class WebAuthenticationHandler(
    authProvider: AuthTokenProvider,
    authClient: HttpClient,
) : AuthenticationHandler(authProvider, authClient) {
    private val client = authClient

    override fun login() {
        window.location.href = "${BuildConfig.GAMERLOGUE_URL}/oidc/login"
    }

    override suspend fun handleCallback(query: String): Boolean = false

    override suspend fun restoreSession() {
        client.get("${BuildConfig.GAMERLOGUE_URL}/sanctum/csrf-cookie")
        val user = try {
            User.all().data.singleOrNull()
        } catch (e: JsonApiException) {
            if (e.statusCode != 401) throw e
            null
        }
        if (user == null) authProvider.clearSession() else authProvider.updateCookieSession(user)
    }
}

@Composable
actual fun rememberAuthenticationHandler(): AuthenticationHandler {
    val authProvider = koinInject<AuthTokenProvider>()
    val authClient = koinInject<HttpClient>(qualifier = named("AuthHttpClient"))
    return remember(authProvider, authClient) { WebAuthenticationHandler(authProvider, authClient) }
}
