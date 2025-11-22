package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.navigation3.runtime.NavKey

@Composable
fun AppScaffold(
    currentNavKey: NavKey,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val topBarState = rememberTopBarState()
    val appUiState = rememberAppUiState()
    CompositionLocalProvider(LocalTopBarState provides topBarState, LocalAppUiState provides appUiState) {
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
            content = content,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
