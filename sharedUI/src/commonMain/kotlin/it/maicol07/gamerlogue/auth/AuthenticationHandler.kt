package it.maicol07.gamerlogue.auth

import io.ktor.client.plugins.auth.providers.BearerTokens

/** Platform login contract used by the UI and the authenticated HTTP client. */
abstract class AuthenticationHandler(
    protected val authProvider: AuthTokenProvider,
) {
    abstract fun login()
    abstract suspend fun logout()

    /** Only platforms that return through an in-app redirect handle callbacks. */
    open suspend fun handleCallback(query: String): Boolean = false

    /** Only platforms with an existing server-side session need restoration. */
    open suspend fun restoreSession() = Unit

    internal open suspend fun loadBearerTokens(): BearerTokens? = null

    internal open suspend fun refreshBearerTokens(requestAccessToken: String?): BearerTokens? = null
}
