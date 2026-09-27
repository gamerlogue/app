package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

private val FilterBadgeSize = 9.dp
private val FilterBadgeInset = 4.dp

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
        GameListResults(
            uiState = uiState,
            section = section,
            onGameClick = { navigationState.backStack.add(it.detailNavKey) },
            onEndReached = viewModel::onEndReached,
            header = uiState.event?.let { event -> { EventHeader(event) } }
        )
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

@Composable
private fun FilterButton(hasActiveFilters: Boolean, onClick: () -> Unit) {
    BadgedBox(
        badge = {
            if (hasActiveFilters) {
                Badge(
                    modifier = Modifier
                        .offset(x = -FilterBadgeInset, y = FilterBadgeInset)
                        .size(FilterBadgeSize),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) {
        IconButton(onClick = onClick) {
            Icon(
                Icons.TuneW500Rounded,
                contentDescription = stringResource(Res.string.gamelist__filter_title)
            )
        }
    }
}
