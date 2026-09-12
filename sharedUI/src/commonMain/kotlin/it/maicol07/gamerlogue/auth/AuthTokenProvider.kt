package it.maicol07.gamerlogue.auth

import it.maicol07.gamerlogue.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

/**
 * The session: the bearer tokens, the id of the signed-in user, and the user itself.
 *
 * State is exposed as [StateFlow] rather than Compose state because it is read from outside a
 * composition too (the Ktor client, the sync services), and only the platform-specific persistence
 * is left to subclasses.
 */
abstract class AuthTokenProvider {
    data class Session(
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val userId: String? = null,
        val user: User? = null,
        val accessExpiresAtEpochMillis: Long? = null,
        val refreshExpiresAtEpochMillis: Long? = null,
    ) {
        val isAuthenticated get() = userId != null
    }

    /**
     * The part of a bearer session that survives a restart, as stored by the platform.
     *
     * Fields are nullable and validated by [restore]: a store can hold a partial session (a token
     * without its expiry, say), and the decision of what to do with it belongs here, not in each
     * platform. An all-null instance means "no session" and, passed to [savePersistedSession], "clear".
     */
    data class PersistedSession(
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val userId: String? = null,
        val accessExpiresAtEpochMillis: Long? = null,
        val refreshExpiresAtEpochMillis: Long? = null,
    )

    val session: StateFlow<Session>
        field = MutableStateFlow(Session())

    protected abstract fun loadPersistedSession(): PersistedSession
    protected abstract fun savePersistedSession(persisted: PersistedSession)

    fun updateCredentials(
        accessToken: String,
        refreshToken: String,
        userId: String,
        accessExpiresAtEpochMillis: Long,
        refreshExpiresAtEpochMillis: Long,
    ) {
        require(accessToken.isNotBlank()) { "Access token must not be blank" }
        require(refreshToken.isNotBlank()) { "Refresh token must not be blank" }
        require(userId.isNotBlank()) { "Session user id must not be blank" }
        require(accessExpiresAtEpochMillis > 0) { "Access token expiry must be valid" }
        require(refreshExpiresAtEpochMillis > 0) { "Refresh expiry must be valid" }

        val persisted = PersistedSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            accessExpiresAtEpochMillis = accessExpiresAtEpochMillis,
            refreshExpiresAtEpochMillis = refreshExpiresAtEpochMillis,
        )
        savePersistedSession(persisted)
        val user = session.value.user?.takeIf { session.value.userId == userId }
        session.value = persisted.toSession(user)
    }

    fun setAuthenticatedUser(user: User) {
        requireNotNull(user.id) { "Authenticated user must have an id" }

        session.value = Session(userId = user.id, user = user)
    }

    fun clearSession() {
        savePersistedSession(PersistedSession())
        session.value = Session()
    }

    /** Discards a profile loaded with credentials that are no longer current. */
    fun updateUser(user: User?, expectedSession: Session) = session.update { current ->
        if (current === expectedSession) {
            current.copy(user = user)
        } else {
            current
        }
    }

    protected fun restore() {
        val persisted = loadPersistedSession()
        val restored = persisted.toSessionOrNull(Clock.System.now().toEpochMilliseconds())
        if (restored != null) {
            session.value = restored
        } else if (persisted != PersistedSession()) {
            clearSession()
        }
    }

    private fun PersistedSession.toSessionOrNull(nowEpochMillis: Long): Session? {
        val (storedAccessToken, storedRefreshToken, storedUserId, storedAccessExpiresAt, storedRefreshExpiresAt) = this
        val invalidCredentials = listOf(storedAccessToken, storedRefreshToken, storedUserId).any { it.isNullOrBlank() }
        val invalidAccessExpiry = storedAccessExpiresAt == null || storedAccessExpiresAt <= 0
        val invalidRefreshExpiry = storedRefreshExpiresAt == null ||
            storedRefreshExpiresAt <= nowEpochMillis
        if (invalidCredentials || invalidAccessExpiry || invalidRefreshExpiry) {
            return null
        }

        return toSession(null)
    }

    private fun PersistedSession.toSession(user: User?) = Session(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
        user = user,
        accessExpiresAtEpochMillis = accessExpiresAtEpochMillis,
        refreshExpiresAtEpochMillis = refreshExpiresAtEpochMillis,
    )
}
