package it.maicol07.gamerlogue.ui.views.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_loading
import gamerlogue.sharedui.generated.resources.game__load_error
import gamerlogue.sharedui.generated.resources.game__not_found
import gamerlogue.sharedui.generated.resources.game__retry
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AndroidWifi3BarAlertW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RefreshW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SearchOffW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.detailNavKey
import it.maicol07.gamerlogue.ui.components.StatusMessage
import it.maicol07.gamerlogue.ui.components.game.GameTopBar
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.views.game.components.GameDetailLoadingCover
import it.maicol07.gamerlogue.ui.views.game.components.GameIgdbMenu
import it.maicol07.gamerlogue.ui.views.game.components.GameToolbar
import it.maicol07.gamerlogue.ui.views.game.components.gameDetailContent
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.gamerlogue.ui.views.library.components.GameAddEditLibrarySheet
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun GameDetailView(
    gameId: Int,
    coverImageId: String? = null,
    gameName: String? = null,
) {
    val navigationState = LocalNavigationState.current
    val viewModel = koinViewModel<GameDetailViewModel> { parametersOf(gameId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val game = uiState.game
    var titleVisible by remember { mutableStateOf(true) }

    var addToLibraryBottomSheetOpen by remember { mutableStateOf(false) }
    // Signing out drops the library entry the sheet edits, so the sheet goes with it.
    LaunchedEffect(uiState.isAuthenticated) {
        if (!uiState.isAuthenticated) addToLibraryBottomSheetOpen = false
    }

    // Library actions need a signed-in user; signed out, each one leads to the login screen instead.
    fun requiringLogin(action: () -> Unit): () -> Unit =
        if (uiState.isAuthenticated) action else ({ navigationState.backStack.add(RootNavTree.Login) })

    Box {
        // Overlay only above the banner: the status screens have none, and white icons would vanish on them.
        GameTopBar(
            game?.name,
            isOverlayMode = game != null && titleVisible,
            actions = { game?.url?.takeIf { it.isNotBlank() }?.let { GameIgdbMenu(it) } }
        )
        when {
            game != null -> {
                var expanded by remember { mutableStateOf(true) }
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                        .floatingToolbarVerticalNestedScroll(
                            expanded = expanded,
                            onExpand = { expanded = true },
                            onCollapse = { expanded = false },
                        ),
                ) {
                    gameDetailContent(
                        game,
                        timeToBeat = uiState.timeToBeat,
                        onTitleVisibilityChange = { titleVisible = it },
                        onGameClick = { navigationState.backStack.add(it.detailNavKey) },
                        onPresetClick = { navigationState.backStack.add(RootNavTree.GameList(null, null, null, it, null)) }
                    )
                }
                AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                GameToolbar(
                    expanded = expanded,
                    currentGameStatus = uiState.libraryEntry?.status,
                    pendingStatus = uiState.pendingStatus,
                    onBacklogClick = requiringLogin { viewModel.toggleStatus(GameLibraryStatus.BACKLOG) },
                    onPlayingClick = requiringLogin { viewModel.toggleStatus(GameLibraryStatus.PLAYING) },
                    onAddClick = requiringLogin { addToLibraryBottomSheetOpen = true },
                )
            }
            uiState.isLoading -> GameDetailLoading(gameId, coverImageId, gameName)
            uiState.isLoadError -> GameDetailError(onRetry = { viewModel.loadGameDetails() })
            else -> GameDetailNotFound()
        }
    }

    if (addToLibraryBottomSheetOpen && game != null) {
        GameAddEditLibrarySheet(
            onDismiss = { addToLibraryBottomSheetOpen = false },
            existingData = uiState.libraryEntry,
            game = game,
            onDelete = { viewModel.loadLibraryEntry() }
        ) { viewModel.loadLibraryEntry() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameDetailLoading(gameId: Int, coverImageId: String?, gameName: String?) = Box(Modifier.fillMaxSize()) {
    val loadingDescription = stringResource(Res.string.common_loading)
    if (gameName != null) {
        GameDetailLoadingCover(gameId, coverImageId, gameName)
    }
    ContainedLoadingIndicator(
        Modifier.align(Alignment.Center).semantics {
            contentDescription = loadingDescription
        }
    )
}

@Composable
private fun GameDetailNotFound() =
    StatusMessage(Icons.SearchOffW500Rounded, stringResource(Res.string.game__not_found))

@Composable
private fun GameDetailError(onRetry: () -> Unit) =
    StatusMessage(Icons.AndroidWifi3BarAlertW500Rounded, stringResource(Res.string.game__load_error)) {
        Button(onClick = onRetry, shapes = ButtonDefaults.shapes()) {
            Icon(
                Icons.RefreshW500Rounded,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(Res.string.game__retry))
        }
    }
