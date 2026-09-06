package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.nav__profile
import gamerlogue.sharedui.generated.resources.nav__settings
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SettingsW500Rounded
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun ProfileView() {
    val navigationState = LocalNavigationState.current
    val authProvider = koinInject<AuthTokenProvider>()
    val session by authProvider.session.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = Res.string.nav__profile,
        actions = {
            IconButton(onClick = { navigationState.backStack.add(RootNavTree.Settings) }) {
                Icon(Icons.SettingsW500Rounded, contentDescription = stringResource(Res.string.nav__settings))
            }
        }
    ) {
        if (!session.isAuthenticated) LoginView() else Profile()
    }
}
