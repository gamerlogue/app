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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import org.jetbrains.compose.resources.StringResource

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalSnackbarHostState not provided")
}

/**
 * App shell: owns only the genuinely global chrome — the bottom navigation bar, the snackbar host,
 * and the static bridge for non-Compose error reporting. Each screen renders its own top bar
 * (see [ScreenScaffold]).
 */
@Composable
fun AppScaffold(
    currentNavKey: NavKey,
    content: @Composable (PaddingValues) -> Unit
) {
    val appUiState = rememberAppUiState()
    val snackbarHostState = remember { SnackbarHostState() }
    CompositionLocalProvider(
        LocalAppUiState provides appUiState,
        LocalSnackbarHostState provides snackbarHostState
    ) {
        // Attach the static bridge for non-Compose reporting
        DisposableEffect(Unit) {
            AppUi.attach(appUiState)
            onDispose { AppUi.detach(appUiState) }
        }

        Scaffold(
            bottomBar = { AppNavigationBar(currentNavKey) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = content,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}

/**
 * Per-screen chrome: an [AppTopBar] (with title, back button and the shared network-error action)
 * above the screen content. Wrap a destination's content in the nav layer so screens stay
 * navigation-free and each adaptive pane gets its own top bar.
 */
@Composable
fun ScreenScaffold(
    title: StringResource?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title, actions = actions) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content() }
    }
}
