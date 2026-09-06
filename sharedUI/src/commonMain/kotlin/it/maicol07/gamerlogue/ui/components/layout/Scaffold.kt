package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.showsNavigationSuite
import it.maicol07.gamerlogue.NavigationBarContrastEnforced
import org.jetbrains.compose.resources.StringResource

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalSnackbarHostState not provided")
}

/**
 * App shell: owns only the genuinely global chrome — adaptive navigation and the snackbar host.
 * Each screen renders its own top bar (see [ScreenScaffold]).
 *
 * Navigation state is read here so changes invalidate only the shell below the Koin root.
 */
@Composable
fun AppScaffold(
    content: @Composable (PaddingValues) -> Unit
) {
    val navigationState = LocalNavigationState.current
    val showNavigation = navigationState.backStack.last().showsNavigationSuite
    val navigationSuiteState = rememberNavigationSuiteScaffoldState()
    val snackbarHostState = remember { SnackbarHostState() }
    NavigationBarContrastEnforced(!showNavigation)
    LaunchedEffect(showNavigation) {
        if (showNavigation) navigationSuiteState.show() else navigationSuiteState.hide()
    }
    NavigationSuiteScaffold(
        navigationSuiteItems = { appNavigationItems(navigationState) },
        state = navigationSuiteState,
    ) {
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                content = content,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            )
        }
    }
}

/**
 * Per-screen chrome: an [AppTopBar] (with title, back button and the shared global-error action)
 * above the screen content. Wrap a destination's content in the nav layer so screens stay
 * navigation-free and each adaptive pane gets its own top bar.
 *
 * Pass [topBar] to replace the default bar entirely (e.g. with a search bar); [title] and
 * [actions] are then ignored.
 */
@Composable
fun ScreenScaffold(
    title: StringResource?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    topBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = { topBar?.invoke() ?: AppTopBar(title, actions = actions) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content() }
    }
}
