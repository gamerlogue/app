package it.maicol07.gamerlogue.auth

import it.maicol07.gamerlogue.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

/**
 * The session: the bearer token, the id of the signed-in user and the user itself.
 *
 * State is exposed as [StateFlow] rather than Compose state because it is read from outside a
 * composition too (the Ktor client, the sync services), and only the platform-specific persistence
 * is left to subclasses.
 */
abstract class AuthTokenProvider {
    data class Session(
        val accessToken: String? = null,
        val userId: String? = null,
        val user: User? = null,
        val expiresAtEpochMillis: Long? = null,
        val cookieBased: Boolean = false,
    ) {
        val isAuthenticated get() = userId != null && (accessToken != null || cookieBased)
    }

    val session: StateFlow<Session>
        field = MutableStateFlow(Session())

    protected abstract fun loadToken(): String?
    protected abstract fun saveToken(token: String?)
    protected abstract fun loadUserId(): String?
    protected abstract fun saveUserId(userId: String?)
    protected abstract fun loadExpiresAtEpochMillis(): Long?
    protected abstract fun saveExpiresAtEpochMillis(value: Long?)

    fun updateCredentials(token: String, userId: String, expiresAtEpochMillis: Long) {
        require(token.isNotBlank()) { "Session token must not be blank" }
        require(userId.isNotBlank()) { "Session user id must not be blank" }
        require(expiresAtEpochMillis > 0) { "Session expiry must be valid" }
        saveUserId(userId)
        saveExpiresAtEpochMillis(expiresAtEpochMillis)
        saveToken(token)
        session.value = Session(token, userId, expiresAtEpochMillis = expiresAtEpochMillis)
    }

    fun updateCookieSession(user: User) {
        requireNotNull(user.id) { "Authenticated user must have an id" }
        session.value = Session(userId = user.id, user = user, cookieBased = true)
    }

    fun clearSession() {
        saveToken(null)
        saveUserId(null)
        saveExpiresAtEpochMillis(null)
        session.value = Session()
    }

    /** Discards a profile loaded for credentials that are no longer current. */
    fun updateUser(user: User?, expectedSession: Session) = session.update { current ->
        if (current.accessToken == expectedSession.accessToken &&
            current.userId == expectedSession.userId &&
            current.cookieBased == expectedSession.cookieBased
        ) {
            current.copy(user = user)
        } else {
            current
        }
    }

    protected fun restore() {
        val token = loadToken()?.takeIf(String::isNotBlank)
        val userId = loadUserId()?.takeIf(String::isNotBlank)
        val expiresAt = loadExpiresAtEpochMillis()
        if (token != null && userId != null && expiresAt != null && expiresAt > Clock.System.now().toEpochMilliseconds()) {
            session.value = Session(token, userId, expiresAtEpochMillis = expiresAt)
        } else if (token != null || userId != null || expiresAt != null) {
            clearSession()
        }
    }

    fun clearIfExpired() {
        val expiresAt = session.value.expiresAtEpochMillis ?: return
        if (expiresAt <= Clock.System.now().toEpochMilliseconds()) clearSession()
    }
}
