package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.home__empty_section
import it.maicol07.gamerlogue.ui.components.game.CoverHeight
import it.maicol07.gamerlogue.ui.components.game.CoverWidth
import it.maicol07.gamerlogue.ui.components.game.GameCoverCard
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.discover.cardMetadata
import org.jetbrains.compose.resources.stringResource

private val CoverAspectRatio = CoverWidth / CoverHeight

/**
 * The paginated cover grid shown inside the expanded search bar, plus its filter sheet.
 *
 * The [viewModel] is the one the search bar drives, so the query typed above and the filters
 * applied here narrow the same list.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameListResults(
    viewModel: GameListViewModel,
    onGameClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState.firstVisibleItemIndex, uiState.games.size) {
        val lastVisible = gridState.firstVisibleItemIndex + gridState.layoutInfo.visibleItemsInfo.size
        viewModel.onEndReached(lastVisible)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!uiState.loading && uiState.games.isEmpty()) {
            EmptyState()
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(uiState.columnCount),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
            ) {
                items(uiState.games, key = { it.id }) { game ->
                    GameCoverCard(
                        game = game,
                        metadata = uiState.section?.cardMetadata(game),
                        showTitle = true,
                        modifier = Modifier.clip(MaterialTheme.shapes.large),
                        sizeModifier = Modifier.fillMaxWidth().aspectRatio(CoverAspectRatio),
                        onClick = onGameClick
                    )
                }
                if (uiState.loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                }
            }
        }

        if (uiState.showFilterSheet) {
            GameListFilterSheet(
                filterState = uiState.filterState,
                columnCount = uiState.columnCount,
                filterSearches = uiState.filterSearches,
                defaultOptions = uiState.defaultOptions,
                onFilterSearch = viewModel::searchFilterOptions,
                onColumnCountChange = viewModel::setColumnCount,
                onFilterChange = viewModel::updateFilter,
                onReset = viewModel::resetFilter,
                onDismiss = { viewModel.toggleFilterSheet(false) }
            )
        }
    }
}

@Composable
private fun EmptyState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(
        text = stringResource(Res.string.home__empty_section),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(Dimens.ScreenPadding)
    )
}
