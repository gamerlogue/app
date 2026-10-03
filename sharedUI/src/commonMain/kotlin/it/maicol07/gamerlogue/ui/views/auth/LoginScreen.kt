package it.maicol07.gamerlogue.ui.views.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.auth__login
import io.github.fopwoc.nav3ksp.annotation.Branch
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import org.koin.compose.koinInject

/**
 * Standalone login destination for actions that need a signed-in user outside the auth-gated tabs.
 *
 * It pops itself once the session turns authenticated, returning to the screen that asked for it.
 */
@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun Login() {
    val navigationState = LocalNavigationState.current
    val authProvider = koinInject<AuthTokenProvider>()
    val session by authProvider.session.collectAsStateWithLifecycle()
    LaunchedEffect(session.isAuthenticated) {
        if (session.isAuthenticated) navigationState.navigateBack()
    }
    ScreenScaffold(title = Res.string.auth__login) { LoginView() }
}
