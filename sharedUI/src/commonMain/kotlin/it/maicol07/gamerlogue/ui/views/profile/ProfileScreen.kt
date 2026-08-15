package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    authProvider: AuthTokenProvider = koinInject<AuthTokenProvider>()
) {
    val accessToken by authProvider.accessToken.collectAsState()
    if (accessToken == null) {
        LoginView()
    } else {
        Profile()
    }
}
