package it.maicol07.gamerlogue.ui.views.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import org.koin.compose.koinInject

@Composable
internal fun AuthenticatedContent(content: @Composable () -> Unit) {
    val authProvider = koinInject<AuthTokenProvider>()
    val session by authProvider.session.collectAsStateWithLifecycle()
    if (!session.isAuthenticated) LoginView() else content()
}
