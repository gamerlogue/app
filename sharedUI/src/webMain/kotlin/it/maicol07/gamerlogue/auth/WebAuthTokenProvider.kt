package it.maicol07.gamerlogue.auth

import kotlinx.browser.window

class WebAuthTokenProvider : AuthTokenProvider() {
    init {
        // Remove credentials left by versions that exposed bearer tokens to JavaScript.
        window.localStorage.removeItem("auth_token")
        window.localStorage.removeItem("auth_user_id")
    }

    override fun loadToken(): String? = null
    override fun saveToken(token: String?) = Unit

    override fun loadUserId(): String? = null
    override fun saveUserId(userId: String?) = Unit
    override fun loadExpiresAtEpochMillis(): Long? = null
    override fun saveExpiresAtEpochMillis(value: Long?) = Unit
}
