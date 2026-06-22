package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameListScreen(
    section: DiscoverSection,
    onGameClick: (Game) -> Unit = {},
    viewModel: GameListViewModel = koinViewModel(parameters = { parametersOf(section) })
) {
    val uiState by viewModel.uiState.collectAsState()

    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState.firstVisibleItemIndex, uiState.games.size) {
        val lastVisible = gridState.firstVisibleItemIndex + gridState.layoutInfo.visibleItemsInfo.size
        viewModel.onEndReached(lastVisible)
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.games) { game ->
            game.CoverImage(Modifier.clickable { onGameClick(game) })
        }
        if (uiState.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        }
    }
}
