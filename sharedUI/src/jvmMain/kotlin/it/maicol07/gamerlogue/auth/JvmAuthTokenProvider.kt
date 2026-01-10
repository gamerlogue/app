package it.maicol07.gamerlogue.auth

import java.util.prefs.Preferences

class JvmAuthTokenProvider : BaseAuthTokenProvider() {
    private val prefs = Preferences.userNodeForPackage(JvmAuthTokenProvider::class.java)
    private val tokenKey = "auth_token"
    private val userIdKey = "auth_user_id"

    init {
        // Load token on init
        restore()
    }

    override fun loadToken(): String? {
        return prefs.get(tokenKey, null)
    }

    override fun saveToken(token: String?) {
        if (token == null) {
            prefs.remove(tokenKey)
        } else {
            prefs.put(tokenKey, token)
        }
    }

    override fun loadUserId(): String? {
        return prefs.get(userIdKey, null)
    }

    override fun saveUserId(userId: String?) {
        if (userId == null) {
            prefs.remove(userIdKey)
        } else {
            prefs.put(userIdKey, userId)
        }
    }
}
