package it.maicol07.gamerlogue.ui.views.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import org.koin.compose.koinInject

/** Whether a user is signed in, for screens whose chrome also depends on it. */
@Composable
internal fun isAuthenticated(): Boolean {
    val authProvider = koinInject<AuthTokenProvider>()
    val session by authProvider.session.collectAsStateWithLifecycle()
    return session.isAuthenticated
}

@Composable
internal fun AuthenticatedContent(content: @Composable () -> Unit) = if (isAuthenticated()) content() else LoginView()
