package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable
import it.maicol07.gamerlogue.BuildConfig

abstract class AuthenticationHandler(protected val authProvider: AuthTokenProvider) {
    abstract fun login()

    fun handleCallback(query: String, decode: (String) -> String? = { it }): Boolean {
        val authData = parseAuthData(query, decode)
        return authData.token?.let {
            authProvider.updateToken(authData.token)
            if (authData.userId != null) {
                authProvider.updateUserId(authData.userId)
            }
            true
        } ?: false
    }

    data class AuthData(val token: String?, val userId: String?)

    companion object {
        fun getAuthUrl(redirectUri: String): String {
            return "${BuildConfig.GAMERLOGUE_URL}/sanctum/token?token_name=Gamerlogue&redirect_uri=$redirectUri"
        }

        fun parseAuthData(url: String, decode: (String) -> String? = { it }): AuthData {
            val query = if (url.contains("?")) url.substringAfter("?") else url
            val params = query.split("&").associate {
                val split = it.split("=")
                if (split.size == 2) split[0] to split[1] else split[0] to ""
            }

            val token = params["token"]?.let(decode)
            val userId = params["user_id"]

            return AuthData(token, userId)
        }
    }
}

@Composable
expect fun rememberAuthenticationHandler(): AuthenticationHandler
