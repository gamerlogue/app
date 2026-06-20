package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation3.runtime.NavKey
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SettingsW500Rounded
import it.maicol07.gamerlogue.NavKeys
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.ui.components.layout.LocalTopBarState
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    authProvider: AuthTokenProvider = koinInject<AuthTokenProvider>(),
    navigateTo: (NavKey) -> Unit // Updated to accept NavKey
) {
    val topBarState = LocalTopBarState.current

    // Set custom actions for the Top Bar
    DisposableEffect(Unit) {
        topBarState.customActions = {
            IconButton(onClick = { navigateTo(NavKeys.Settings) }) {
                Icon(Icons.SettingsW500Rounded, contentDescription = "Settings")
            }
        }
        onDispose {
            topBarState.customActions = null
        }
    }

    if (authProvider.accessToken == null) {
        LoginView()
    } else {
        Profile()
    }
}
