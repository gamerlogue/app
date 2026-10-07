package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.gamelist__empty_hint
import gamerlogue.sharedui.generated.resources.home__empty_section
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SearchOffW500Rounded
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.components.game.CoverAspectRatio
import it.maicol07.gamerlogue.ui.components.game.GameCoverCard
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import it.maicol07.gamerlogue.ui.views.discover.cardMetadata
import org.jetbrains.compose.resources.stringResource

private val EmptyStatePadding = 48.dp

/**
 * The paginated cover grid of the game list destination.
 *
 * [onEndReached] receives the last visible item index whenever it changes, to prefetch the next
 * page. Pass [header] to prepend a full-width block that scrolls with the grid (e.g., the details
 * of the event the list is scoped to).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameListResults(
    uiState: GameListViewModel.UiState,
    section: DiscoverSection?,
    onGameClick: (Game) -> Unit,
    onEndReached: (lastVisibleIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val gridState = rememberLazyGridState()
    // Read in a snapshotFlow rather than as effect keys, so scrolling does not recompose the grid.
    val currentOnEndReached by rememberUpdatedState(onEndReached)
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { currentOnEndReached(it) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(uiState.columnCount),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
        ) {
            // The header spans the grid and scrolls with it, so it stays visible even when the scope
            // has no games at all (an event whose line-up IGDB has not filled in yet).
            header?.let { content ->
                item(key = "header", span = { GridItemSpan(maxLineSpan) }) { content() }
            }
            items(uiState.games, key = { it.id }) { game ->
                GameCoverCard(
                    game = game,
                    metadata = listOfNotNull(section?.cardMetadata(game)),
                    showTitle = true,
                    modifier = Modifier.animateItem().clip(MaterialTheme.shapes.large),
                    sizeModifier = Modifier.fillMaxWidth().aspectRatio(CoverAspectRatio),
                    onClick = onGameClick
                )
            }
            if (uiState.loading) {
                item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        // The first page gets the contained, more prominent indicator; later pages
                        // load below content that is already there.
                        if (uiState.games.isEmpty()) ContainedLoadingIndicator() else LoadingIndicator()
                    }
                }
            } else if (uiState.games.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { NoResults() }
            }
        }
        AppVerticalScrollbar(gridState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
private fun NoResults() = EmptyState(
    icon = Icons.SearchOffW500Rounded,
    title = stringResource(Res.string.home__empty_section),
    hint = stringResource(Res.string.gamelist__empty_hint),
    modifier = Modifier.fillMaxWidth().padding(vertical = EmptyStatePadding, horizontal = Dimens.ScreenPadding)
)
