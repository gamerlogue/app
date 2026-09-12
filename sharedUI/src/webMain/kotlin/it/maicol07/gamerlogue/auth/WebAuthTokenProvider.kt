package it.maicol07.gamerlogue.auth

import kotlinx.browser.window

class WebAuthTokenProvider : AuthTokenProvider() {
    init {
        // Remove credentials left by versions that exposed bearer tokens to JavaScript.
        window.localStorage.removeItem("auth_token")
        window.localStorage.removeItem("auth_user_id")
    }

    // The browser session lives in an HttpOnly cookie: there is nothing for this class to persist.
    override fun loadPersistedSession() = PersistedSession()
    override fun savePersistedSession(persisted: PersistedSession) = Unit
}
