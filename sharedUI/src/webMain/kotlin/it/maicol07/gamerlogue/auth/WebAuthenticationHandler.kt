package it.maicol07.gamerlogue.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import it.maicol07.gamerlogue.BuildConfig
import it.maicol07.gamerlogue.data.User
import it.maicol07.spraypaintkt.JsonApiException
import kotlinx.browser.window

class WebAuthenticationHandler(
    authProvider: AuthTokenProvider,
    private val authClient: HttpClient,
) : AuthenticationHandler(authProvider) {
    override fun login() {
        window.location.href = "${BuildConfig.GAMERLOGUE_URL}/oidc/login"
    }

    override suspend fun restoreSession() {
        authClient.get("${BuildConfig.GAMERLOGUE_URL}/sanctum/csrf-cookie")
        val user = try {
            User.all().data.singleOrNull()
        } catch (e: JsonApiException) {
            if (e.statusCode != HttpStatusCode.Unauthorized.value) throw e
            null
        }
        if (user == null) authProvider.clearSession() else authProvider.setAuthenticatedUser(user)
    }

    /**
     * The browser session is a server-side cookie, so clearing the local state alone would be undone by
     * the next [restoreSession]. The endpoint is dropped first; the local state is cleared regardless of
     * the outcome, so a failed call still signs the user out of this tab.
     */
    override suspend fun logout() {
        try {
            authClient.post("${BuildConfig.GAMERLOGUE_URL}/logout")
        } finally {
            authProvider.clearSession()
        }
    }
}
