package it.maicol07.gamerlogue.auth

import java.util.prefs.Preferences

class JvmAuthTokenProvider : AuthTokenProvider {
    private val prefs = Preferences.userNodeForPackage(JvmAuthTokenProvider::class.java)
    private val TOKEN_KEY = "auth_token"

    init {
        // Load token on init
        AuthState.token = getToken()
    }

    override fun getToken(): String? {
        return prefs.get(TOKEN_KEY, null)
    }

    override fun setToken(token: String?) {
        if (token == null) {
            prefs.remove(TOKEN_KEY)
        } else {
            prefs.put(TOKEN_KEY, token)
        }
        AuthState.token = token
    }
}

