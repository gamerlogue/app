package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    authProvider: AuthTokenProvider = koinInject<AuthTokenProvider>()
) {
    val session by authProvider.session.collectAsStateWithLifecycle()
    if (!session.isAuthenticated) {
        LoginView()
    } else {
        Profile()
    }
}
