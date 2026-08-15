package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.github.michaelbull.result.unwrap
import io.ktor.http.decodeURLQueryComponent
import it.maicol07.gamerlogue.AppEnvironment
import it.maicol07.gamerlogue.BuildConfig
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.safeRequest
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.data.UserStore
import org.koin.compose.koinInject

@Composable
internal fun AuthHandler(authCallbackUri: String?) {
    val authProvider = koinInject<AuthTokenProvider>()
    val authHandler = rememberAuthenticationHandler()
    val userStore = koinInject<UserStore>()
    val exceptionReporter = koinInject<ExceptionReporter>()

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
        userStore.getUser()?.let(authProvider::updateCurrentUser)
    }

    val accessToken by authProvider.accessToken.collectAsState()
    val currentUserId by authProvider.currentUserId.collectAsState()

    LaunchedEffect(accessToken, currentUserId) {
        // The token itself is never logged: this runs in release builds too.
        Logger.d("AuthState changed: authenticated=${accessToken != null}, userId=$currentUserId")
        if (accessToken != null) {
            if (authProvider.currentUser.value == null && currentUserId != null) {
                val result = exceptionReporter.safeRequest { User.find(currentUserId!!).data }
                if (result.isOk) {
                    val user = result.unwrap()
                    authProvider.updateCurrentUser(user)
                    userStore.saveUser(user)
                }
            }
        } else {
            authProvider.updateCurrentUser(null)
            authProvider.updateUserId(null)
            userStore.clear()
        }
    }
}
