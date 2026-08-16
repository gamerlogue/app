package it.maicol07.gamerlogue.ui.views.game

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_loading
import gamerlogue.sharedui.generated.resources.game__not_found
import it.maicol07.gamerlogue.ui.components.game.GameTopBar
import it.maicol07.gamerlogue.ui.components.game.LocalGameTopBarOverlayMode
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.views.game.components.GameDetailLoadingCover
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
    coverImageId: String? = null,
    gameName: String? = null,
    viewModel: GameDetailViewModel = GameDetailViewModel.inject(gameId),
    onGameClick: ((Game) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    var addToLibraryBottomSheetOpen by remember { mutableStateOf(false) }
    val loadingDescription = stringResource(Res.string.common_loading)

    Box(contentAlignment = Alignment.TopStart) {
        var expanded by remember { mutableStateOf(true) }
        val listState = rememberLazyListState()
        CompositionLocalProvider(LocalGameTopBarOverlayMode provides mutableStateOf(true)) {
            GameTopBar(uiState.game?.name)
            if (uiState.game != null) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                        .floatingToolbarVerticalNestedScroll(
                            expanded = expanded,
                            onExpand = { expanded = true },
                            onCollapse = { expanded = false },
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gameDetailContent(uiState.game!!, timeToBeat = uiState.timeToBeat, onGameClick = onGameClick)
                }
            } else if (uiState.isLoading) {
                Box(Modifier.fillMaxSize()) {
                    if (gameName != null) {
                        GameDetailLoadingCover(gameId, coverImageId, gameName)
                    }
                    LoadingIndicator(
                        Modifier.align(Alignment.Center).semantics {
                            contentDescription = loadingDescription
                        }
                    )
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(Res.string.game__not_found),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (uiState.game != null) {
            AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
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
