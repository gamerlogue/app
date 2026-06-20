package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import it.maicol07.gamerlogue.data.User

@Composable
actual fun rememberAuthenticationHandler(): AuthenticationHandler = remember {
    object : AuthenticationHandler(object : AuthTokenProvider {
        override val accessToken: String? = "TODO"
        override val currentUserId: String? = "TODO"
        override var currentUser: User? = null

        override fun updateToken(token: String?) {
            // TODO
        }

        override fun updateUserId(userId: String?) {
            // TODO
        }
    }) {
        override fun login() {
            // TODO
        }
    }
}
