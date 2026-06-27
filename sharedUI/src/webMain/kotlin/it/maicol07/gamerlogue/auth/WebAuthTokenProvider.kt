package it.maicol07.gamerlogue.auth

import kotlinx.browser.window

class WebAuthTokenProvider : AuthTokenProvider() {
    init { restore() }

    override fun loadToken(): String? = window.localStorage.getItem("auth_token")
    override fun saveToken(token: String?) {
        if (token != null) window.localStorage.setItem("auth_token", token)
        else window.localStorage.removeItem("auth_token")
    }

    override fun loadUserId(): String? = window.localStorage.getItem("auth_user_id")
    override fun saveUserId(userId: String?) {
        if (userId != null) window.localStorage.setItem("auth_user_id", userId)
        else window.localStorage.removeItem("auth_user_id")
    }
}
