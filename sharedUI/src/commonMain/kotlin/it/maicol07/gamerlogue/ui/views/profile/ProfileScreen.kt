package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.runtime.Composable
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    authProvider: AuthTokenProvider = koinInject<AuthTokenProvider>()
) {
    if (authProvider.accessToken == null) {
        LoginView()
    } else {
        Profile()
    }
}
