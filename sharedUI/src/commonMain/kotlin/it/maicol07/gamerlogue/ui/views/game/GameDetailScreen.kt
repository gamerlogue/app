package it.maicol07.gamerlogue.ui.views.game

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__not_found
import it.maicol07.gamerlogue.ui.components.game.GameTopBar
import it.maicol07.gamerlogue.ui.components.game.LocalGameTopBarOverlayMode
import it.maicol07.gamerlogue.ui.views.game.components.GameToolbar
import it.maicol07.gamerlogue.ui.views.game.components.gameDetailContent
import it.maicol07.gamerlogue.ui.views.library.components.GameAddEditLibrarySheet
import org.jetbrains.compose.resources.stringResource

@OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun GameDetailScreen(
    gameId: Int,
    viewModel: GameDetailViewModel = GameDetailViewModel.inject(gameId),
    onGameClick: ((Game) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    var addToLibraryBottomSheetOpen by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.TopStart) {
        var expanded by remember { mutableStateOf(true) }
        CompositionLocalProvider(LocalGameTopBarOverlayMode provides mutableStateOf(true)) {
            GameTopBar(uiState.game?.name)
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .floatingToolbarVerticalNestedScroll(
                        expanded = expanded,
                        onExpand = { expanded = true },
                        onCollapse = { expanded = false },
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.game != null) {
                    gameDetailContent(uiState.game!!, onGameClick = onGameClick)
                } else if (uiState.isLoading) {
                    item {
                        Box(Modifier.fillMaxSize().animateItem(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    }
                } else {
                    item {
                        Box(Modifier.fillMaxSize().animateItem(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(Res.string.game__not_found),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (uiState.game != null) {
            GameToolbar(
                expanded,
                uiState.libraryEntry?.status,
                uiState.isBacklogButtonLoading,
                uiState.isPlayingButtonLoading,
                { viewModel.toggleGameBacklog() },
                { viewModel.toggleGamePlaying() }
            ) { addToLibraryBottomSheetOpen = true }
        }
    }

    if (addToLibraryBottomSheetOpen && uiState.game != null) {
        GameAddEditLibrarySheet(
            onDismiss = { addToLibraryBottomSheetOpen = false },
            existingData = uiState.libraryEntry,
            game = uiState.game!!,
            onDelete = { viewModel.loadLibraryEntry() }
        ) { viewModel.loadLibraryEntry() }
    }
}
