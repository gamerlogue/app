package it.maicol07.gamerlogue.auth

import java.util.prefs.Preferences

class JvmAuthTokenProvider : AuthTokenProvider {
    private val prefs = Preferences.userNodeForPackage(JvmAuthTokenProvider::class.java)
    private val tokenKey = "auth_token"
    private val userIdKey = "auth_user_id"

    init {
        // Load token on init
        AuthState.token = getToken()
        AuthState.userId = getUserId()
    }

    override fun getToken(): String? {
        return prefs.get(tokenKey, null)
    }

    override fun setToken(token: String?) {
        if (token == null) {
            prefs.remove(tokenKey)
        } else {
            prefs.put(tokenKey, token)
        }
        AuthState.token = token
    }

    override fun getUserId(): String? {
        return prefs.get(userIdKey, null)
    }

    override fun setUserId(userId: String?) {
        if (userId == null) {
            prefs.remove(userIdKey)
        } else {
            prefs.put(userIdKey, userId)
        }
        AuthState.userId = userId
    }
}
