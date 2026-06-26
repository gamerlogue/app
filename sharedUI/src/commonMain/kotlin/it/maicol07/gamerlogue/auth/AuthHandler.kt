package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.github.michaelbull.result.unwrap
import io.ktor.http.decodeURLQueryComponent
import it.maicol07.gamerlogue.AppEnvironment
import it.maicol07.gamerlogue.BuildConfig
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.data.UserStore
import it.maicol07.gamerlogue.safeRequest
import org.koin.compose.koinInject

@Composable
internal fun AuthHandler(authCallbackUri: String?) {
    val authProvider = koinInject<AuthTokenProvider>()
    val authHandler = rememberAuthenticationHandler()
    val userStore = remember { UserStore() }

    // Handle the login callback here, inside the Koin composition, so the token lands on the
    // same AuthTokenProvider singleton the Ktor client reads (the Activity has its own Koin-less scope).
    LaunchedEffect(authCallbackUri) {
        if (authCallbackUri != null) {
            // Backend double-URL-encodes the token, so Sanctum's "id|hash" arrives as "id%257Chash".
            // ponytail: fully decode (safe: token has no literal '%'); single decode if the backend stops double-encoding.
            authHandler.handleCallback(authCallbackUri) { raw ->
                var value = raw
                while (true) {
                    val decoded = value.decodeURLQueryComponent(plusIsSpace = true)
                    if (decoded == value) break
                    value = decoded
                }
                value
            }
        }
    }

    LaunchedEffect(Unit) {
        if (BuildConfig.APP_ENV === AppEnvironment.LOCAL) {
            Logger.setMinSeverity(Severity.Verbose)
            Logger.i("Running in LOCAL environment")
        }
        val savedUser = userStore.getUser()
        if (savedUser != null) {
            authProvider.currentUser = savedUser
        }
    }

    LaunchedEffect(authProvider.accessToken, authProvider.currentUserId) {
        Logger.d("AuthState changed: token=${authProvider.accessToken}, userId=${authProvider.currentUserId}")
        if (authProvider.accessToken != null) {
            if (authProvider.currentUser == null && authProvider.currentUserId != null) {
                val result = safeRequest { User.find(authProvider.currentUserId!!).data }
                if (result.isOk) {
                    val user = result.unwrap()
                    authProvider.currentUser = user
                    userStore.saveUser(user)
                }
            }
        } else {
            authProvider.currentUser = null
            authProvider.updateUserId(null)
            userStore.clear()
        }
    }
}
