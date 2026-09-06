package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

private val FilterBadgeSize = 9.dp
private val FilterBadgeInset = 4.dp

@Branch(RootTree::class, metadata = ListPaneMetadata::class)
@Composable
fun GameListView(section: DiscoverSection?, eventId: Int?, eventName: String?) {
    val navigationState = LocalNavigationState.current
    val viewModel = koinViewModel<GameListViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(section, eventId) { viewModel.start(section, eventId) }
    ScreenScaffold(
        title = section?.sectionTitle,
        topBar = { GameListTopBar(viewModel, eventName, section == null && eventId == null) }
    ) {
        GameListResults(
            viewModel = viewModel,
            onGameClick = { navigationState.backStack.add(it.detailNavKey) },
            header = uiState.event?.let { event -> { EventHeader(event) } }
        )
    }
}

@Composable
private fun GameListTopBar(viewModel: GameListViewModel, eventName: String?, autoFocus: Boolean) {
    val navigationState = LocalNavigationState.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GameListSearchBar(
        placeholder = eventName ?: stringResource(Res.string.search__global_hint),
        query = uiState.filterState.searchQuery,
        onQueryChange = viewModel::setSearchQuery,
        onSearch = viewModel::submitSearchQuery,
        onBack = navigationState::navigateBack,
        autoFocus = autoFocus,
        trailingActions = {
            BadgedBox(
                badge = {
                    if (uiState.filterState.hasActiveFilters) {
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
                IconButton(onClick = { viewModel.toggleFilterSheet(true) }) {
                    Icon(
                        Icons.TuneW500Rounded,
                        contentDescription = stringResource(Res.string.gamelist__filter_title)
                    )
                }
            }
        }
    )
}
