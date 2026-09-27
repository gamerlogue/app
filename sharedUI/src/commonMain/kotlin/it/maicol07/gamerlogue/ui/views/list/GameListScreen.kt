package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.gamelist__filter_title
import gamerlogue.sharedui.generated.resources.search__global_hint
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.TuneW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.detailNavKey
import it.maicol07.gamerlogue.ui.components.event.EventHeader
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.search.GameListSearchBar
import it.maicol07.gamerlogue.ui.navigation.ListPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val ChipRowPadding = 8.dp

@Branch(RootTree::class, metadata = ListPaneMetadata::class)
@Composable
fun GameListView(section: DiscoverSection?, eventId: Int?, eventName: String?) {
    val navigationState = LocalNavigationState.current
    val viewModel = koinViewModel<GameListViewModel> { parametersOf(section, eventId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenScaffold(
        topBar = {
            GameListSearchBar(
                placeholder = eventName ?: stringResource(Res.string.search__global_hint),
                query = uiState.filterState.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                onSearch = viewModel::submitSearchQuery,
                onBack = navigationState::navigateBack,
                autoFocus = section == null && eventId == null,
                trailingActions = {
                    FilterButton(
                        hasActiveFilters = uiState.filterState.hasActiveFilters,
                        onClick = { viewModel.toggleFilterSheet(true) }
                    )
                }
            )
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = uiState.filterState.hasActiveFilters) {
                ActiveFilterChips(
                    filterState = uiState.filterState,
                    knownOptions = uiState.knownOptions,
                    onFilterChange = viewModel::updateFilter,
                    onReset = viewModel::resetFilter,
                    modifier = Modifier.padding(top = ChipRowPadding)
                )
            }
            GameListResults(
                uiState = uiState,
                section = section,
                onGameClick = { navigationState.backStack.add(it.detailNavKey) },
                onEndReached = viewModel::onEndReached,
                modifier = Modifier.weight(1f),
                header = uiState.event?.let { event -> { EventHeader(event) } }
            )
        }
    }

    if (uiState.showFilterSheet) {
        GameListFilterSheet(
            filterState = uiState.filterState,
            columnCount = uiState.columnCount,
            filterSearches = uiState.filterSearches,
            defaultOptions = uiState.defaultOptions,
            knownOptions = uiState.knownOptions,
            onFilterSearch = viewModel::searchFilterOptions,
            onColumnCountChange = viewModel::setColumnCount,
            onFilterChange = viewModel::updateFilter,
            onReset = viewModel::resetFilter,
            onDismiss = { viewModel.toggleFilterSheet(false) }
        )
    }
}

/**
 * Opens the filter sheet. Checked means "filters are active": the button morphs from round to
 * rounded-square and fills with the primary container, instead of carrying a badge dot. Tapping
 * always opens the sheet; it never clears the filters.
 */
@Composable
private fun FilterButton(hasActiveFilters: Boolean, onClick: () -> Unit) = IconToggleButton(
    checked = hasActiveFilters,
    onCheckedChange = { onClick() },
    shapes = IconButtonDefaults.toggleableShapes(),
    colors = IconButtonDefaults.iconToggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
) {
    Icon(
        Icons.TuneW500Rounded,
        contentDescription = stringResource(Res.string.gamelist__filter_title)
    )
}
