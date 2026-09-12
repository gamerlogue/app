package it.maicol07.gamerlogue.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlin.time.Clock
import kotlin.time.Instant

/** Shared PKCE and bearer-token behavior for Android and desktop. */
abstract class NativeAuthenticationHandler(
    authProvider: AuthTokenProvider,
    authClient: HttpClient,
) : AuthenticationHandler(authProvider) {
    private val tokenMutex = Mutex()
    private val tokenClient = SanctumTokenClient(authClient)

    final override fun login() = launchLogin(createPkceLoginAttempt())

    protected abstract fun launchLogin(attempt: PkceLoginAttempt)

    final override suspend fun logout() = tokenMutex.withLock {
        authProvider.session.value.refreshToken?.let { tokenClient.revoke(it) }
        authProvider.clearSession()
    }

    final override suspend fun loadBearerTokens(): BearerTokens? = tokenMutex.withLock {
        val current = authProvider.session.value
        val expiresAt = current.accessExpiresAtEpochMillis ?: return@withLock null
        if ((expiresAt - Clock.System.now().toEpochMilliseconds()) > REFRESH_MARGIN_MILLIS) {
            return@withLock current.bearerTokens()
        }
        refresh(current)
    }

    final override suspend fun refreshBearerTokens(requestAccessToken: String?): BearerTokens? = tokenMutex.withLock {
        val current = authProvider.session.value
        if (current.accessToken != requestAccessToken) return@withLock current.bearerTokens()
        refresh(current)
    }

    protected suspend fun exchangeCallback(query: String, pending: PendingLogin): Boolean {
        val code = authorizationCode(query)
            ?.takeIf { callbackMatchesState(query, pending.state) }
            ?: return false
        val tokens = tokenClient.exchange(code, pending.verifier)
        authProvider.updateCredentials(tokens)
        return true
    }

    private suspend fun refresh(current: AuthTokenProvider.Session): BearerTokens {
        val refreshToken = current.refreshToken
        val refreshExpiresAt = current.refreshExpiresAtEpochMillis
        if (refreshToken == null || refreshExpiresAt == null) throw NotAuthenticatedException()
        if (refreshExpiresAt <= Clock.System.now().toEpochMilliseconds()) {
            authProvider.clearSession()
            throw AuthenticationLostException("refresh_token_invalid", HttpStatusCode.Unauthorized)
        }

        return withContext(NonCancellable) { refresh(refreshToken) }
    }

    private suspend fun refresh(refreshToken: String): BearerTokens = try {
        val tokens = tokenClient.refresh(refreshToken)
        authProvider.updateCredentials(tokens)
        tokens.bearerTokens()
    } catch (e: SerializationException) {
        authProvider.clearSession()
        throw AuthenticationLostException("invalid_response", HttpStatusCode.OK, e)
    } catch (e: IllegalArgumentException) {
        authProvider.clearSession()
        throw AuthenticationLostException("invalid_response", HttpStatusCode.OK, e)
    } catch (e: AuthenticationLostException) {
        authProvider.clearSession()
        throw e
    }

    private companion object {
        const val REFRESH_MARGIN_MILLIS = 60_000L
    }
}

private fun AuthTokenProvider.Session.bearerTokens(): BearerTokens? {
    if (accessToken == null || refreshToken == null) return null
    return BearerTokens(accessToken, refreshToken)
}

private fun AuthTokenProvider.updateCredentials(response: TokenResponse) = updateCredentials(
    accessToken = response.accessToken,
    refreshToken = response.refreshToken,
    userId = response.userId,
    accessExpiresAtEpochMillis = Instant.parse(response.accessExpiresAt).toEpochMilliseconds(),
    refreshExpiresAtEpochMillis = Instant.parse(response.refreshExpiresAt).toEpochMilliseconds(),
)

private fun TokenResponse.bearerTokens() = BearerTokens(accessToken, refreshToken)
