package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalSnackbarHostState not provided")
}

@Composable
fun AppScaffold(
    currentNavKey: NavKey,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val topBarState = rememberTopBarState()
    val appUiState = rememberAppUiState()
    val snackbarHostState = remember { SnackbarHostState() }
    CompositionLocalProvider(
        LocalTopBarState provides topBarState,
        LocalAppUiState provides appUiState,
        LocalSnackbarHostState provides snackbarHostState
    ) {
        // Attach the static bridge for non-Compose reporting
        DisposableEffect(Unit) {
            AppUi.attach(appUiState)
            onDispose { AppUi.detach(appUiState) }
        }

        Scaffold(
            topBar = {
                AppTopBar(
                    currentNavKey,
                    canNavigateBack,
                    navigateUp
                )
            },
            bottomBar = {
                AppNavigationBar(currentNavKey)
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = content,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
