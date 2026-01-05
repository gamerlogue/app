package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation3.runtime.NavKey
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import org.koin.core.parameter.parametersOf

enum class GameListType { Popular, Upcoming }

@Composable
fun GameListScreen(
    type: GameListType,
    navigateTo: (NavKey) -> Unit = {}
) {
    val viewModel: GameListViewModel = koinViewModel(parameters = { parametersOf(type) })

    val listState = rememberLazyListState()
    LaunchedEffect(listState.firstVisibleItemIndex, viewModel.games.size) {
        val lastVisible = listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size
        viewModel.onEndReached(lastVisible)
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FiltersRow(type, viewModel)
        }
        item {
            GameGrid(viewModel.games)
        }
        if (viewModel.isLoading.value) {
            item { Text("Caricamento...", style = MaterialTheme.typography.bodySmall) }
        }
        if (viewModel.endReached.value && viewModel.games.isEmpty()) {
            item { Text("Nessun risultato", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun FiltersRow(type: GameListType, vm: GameListViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = { vm.setSort(GameListViewModel.GameSort.RatingDesc) },
            label = { Text("Rating") },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = if (vm.sort.value == GameListViewModel.GameSort.RatingDesc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        )
        AssistChip(
            onClick = { vm.setSort(GameListViewModel.GameSort.ReleaseDateAsc) },
            label = { Text("Data") },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = if (vm.sort.value == GameListViewModel.GameSort.ReleaseDateAsc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        )
        AssistChip(
            onClick = { vm.setSort(GameListViewModel.GameSort.NameAsc) },
            label = { Text("Nome") },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = if (vm.sort.value == GameListViewModel.GameSort.NameAsc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        )

        if (type == GameListType.Upcoming) {
            val selected = vm.timeframeDays.value
            AssistChip(
                onClick = { vm.setTimeframe(null) },
                label = { Text("Tutti") },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = if (selected == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )
            listOf(7, 30, 90).forEach { days ->
                AssistChip(
                    onClick = { vm.setTimeframe(days) },
                    label = { Text("<= ${days}g") },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = if (selected == days) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun GameGrid(games: List<Game>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(games) { game ->
            game.CoverImage(Modifier)
        }
    }
}
