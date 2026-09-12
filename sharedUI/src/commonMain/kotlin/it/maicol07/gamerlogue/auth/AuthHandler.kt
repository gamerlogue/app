package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.AppEnvironment
import it.maicol07.gamerlogue.BuildConfig
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.safeRequest
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.data.UserStore
import org.koin.compose.koinInject

@Composable
internal fun AuthHandler(
    authCallbackUri: String?,
    onAuthCallbackHandled: () -> Unit = {},
) {
    val authProvider = koinInject<AuthTokenProvider>()
    val authHandler = koinInject<AuthenticationHandler>()
    val userStore = koinInject<UserStore>()
    val exceptionReporter = koinInject<ExceptionReporter>()

    // Handle the login callback here, inside the Koin composition, so the token lands on the
    // same AuthTokenProvider singleton the Ktor client reads (the Activity has its own Koin-less scope).
    LaunchedEffect(authCallbackUri) {
        if (authCallbackUri != null) {
            exceptionReporter.safeRequest { authHandler.handleCallback(authCallbackUri) }
            // One-shot: tells the host to drop the URI so a recomposition does not replay it.
            onAuthCallbackHandled()
        }
    }

    LaunchedEffect(Unit) {
        if (BuildConfig.APP_ENV === AppEnvironment.LOCAL) {
            Logger.setMinSeverity(Severity.Verbose)
            Logger.i("Running in LOCAL environment")
        }
        val restoreResult = exceptionReporter.safeRequest { authHandler.restoreSession() }
        if (restoreResult.isErr) return@LaunchedEffect
        val session = authProvider.session.value
        val cachedUser = userStore.getUser()?.takeIf { it.id == session.userId }
        if (session.isAuthenticated) authProvider.updateUser(cachedUser, session) else userStore.clear()
    }

    val session by authProvider.session.collectAsStateWithLifecycle()

    LaunchedEffect(session.accessToken, session.userId) {
        // The token itself is never logged: this runs in release builds too.
        Logger.d("AuthState changed: authenticated=${session.isAuthenticated}, userId=${session.userId}")
        if (session.isAuthenticated && session.user == null) {
            val result = exceptionReporter.safeRequest { User.find(session.userId!!).data }
            if (result.isOk) {
                val user = result.unwrap()
                authProvider.updateUser(user, session)
                if (authProvider.session.value.user === user) userStore.saveUser(user)
            }
        } else if (!session.isAuthenticated) {
            userStore.clear()
        }
    }
}
