package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common__back
import gamerlogue.sharedui.generated.resources.exception__action_show
import gamerlogue.sharedui.generated.resources.nav__notifications
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowBackW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.NotificationsW500Rounded
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.UnreadNotifications
import it.maicol07.gamerlogue.ui.navigation.AppNavigationState
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AppTopBar(
    title: StringResource?,
    modifier: Modifier = Modifier,
    navigationState: AppNavigationState = LocalNavigationState.current,
    actions: @Composable RowScope.() -> Unit = {}
) = AppTopBar(title?.let { stringResource(it) }, modifier, navigationState, subtitle = null, actions = actions)

/** [AppTopBar] with a runtime title, e.g. another user's nickname, and an optional [subtitle] under it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTopBar(
    title: String?,
    modifier: Modifier = Modifier,
    navigationState: AppNavigationState = LocalNavigationState.current,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { title?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        subtitle = { subtitle?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        modifier = modifier,
        navigationIcon = {
            if (navigationState.backStack.size > 1) {
                IconButton(
                    onClick = navigationState::navigateBack,
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(Icons.ArrowBackW500Rounded, stringResource(Res.string.common__back))
                }
            }
        },
        actions = {
            actions()
            NotificationsAction(navigationState)
            GlobalErrorAction()
        }
    )
}

/** The bell with the unread count, on every bar of a signed-in user except the notifications' own. */
@Composable
fun NotificationsAction(navigationState: AppNavigationState = LocalNavigationState.current) {
    val session by koinInject<AuthTokenProvider>().session.collectAsStateWithLifecycle()
    if (session.userId == null || navigationState.backStack.lastOrNull() == RootNavTree.Notifications) return
    val unread = koinInject<UnreadNotifications>()
    val count by unread.count.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { unread.refreshIfStale() }
    IconButton(
        onClick = { navigationState.backStack.add(RootNavTree.Notifications) },
        shapes = IconButtonDefaults.shapes()
    ) {
        BadgedBox(badge = { if (count > 0) Badge { Text(count.toString()) } }) {
            Icon(Icons.NotificationsW500Rounded, contentDescription = stringResource(Res.string.nav__notifications))
        }
    }
}

@Composable
fun GlobalErrorAction() {
    val reporter = koinInject<ExceptionReporter>()
    val errorState by reporter.state.collectAsStateWithLifecycle()
    AnimatedVisibility(errorState != null) {
        IconButton(onClick = { reporter.show() }, shapes = IconButtonDefaults.shapes()) {
            Icon(
                Icons.ErrorW500Rounded,
                contentDescription = stringResource(Res.string.exception__action_show),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
