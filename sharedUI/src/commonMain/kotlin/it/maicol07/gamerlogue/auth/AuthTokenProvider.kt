package it.maicol07.gamerlogue.auth

import it.maicol07.gamerlogue.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The session: the bearer token, the id of the signed-in user and the user itself.
 *
 * State is exposed as [StateFlow] rather than Compose state because it is read from outside a
 * composition too (the Ktor client, the sync services), and only the platform-specific persistence
 * is left to subclasses.
 */
abstract class AuthTokenProvider {
    val accessToken: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    val currentUserId: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    val currentUser: StateFlow<User?>
        field = MutableStateFlow<User?>(null)

    protected abstract fun loadToken(): String?
    protected abstract fun saveToken(token: String?)
    protected abstract fun loadUserId(): String?
    protected abstract fun saveUserId(userId: String?)

    fun updateToken(token: String?) {
        saveToken(token)
        accessToken.value = token
    }

    fun updateUserId(userId: String?) {
        saveUserId(userId)
        currentUserId.value = userId
    }

    /** The signed-in user's profile; not persisted here (see [it.maicol07.gamerlogue.data.UserStore]). */
    fun updateCurrentUser(user: User?) {
        currentUser.value = user
    }

    open fun restore() {
        accessToken.value = loadToken()
        currentUserId.value = loadUserId()
    }
}
