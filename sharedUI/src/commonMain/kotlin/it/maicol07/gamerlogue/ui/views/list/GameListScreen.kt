package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.gamelist__filter_title
import gamerlogue.sharedui.generated.resources.gamelist__pick_confirm
import gamerlogue.sharedui.generated.resources.search__global_hint
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.TuneW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.detailNavKey
import it.maicol07.gamerlogue.ui.components.event.EventHeader
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.search.GameListSearchBar
import it.maicol07.gamerlogue.ui.navigation.ListPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import it.maicol07.gamerlogue.ui.views.discover.cardMetadata
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.gamerlogue.ui.views.library.cardMetadata
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val ChipRowPadding = 8.dp

@Branch(RootTree::class, metadata = ListPaneMetadata::class)
@Composable
fun GameListView(
    section: DiscoverSection?,
    eventId: Int?,
    eventName: String?,
    preset: GameListPreset?,
    libraryStatus: GameLibraryStatus?,
    /** With [libraryStatus], whose library: null for the signed-in user's. */
    libraryUserId: String?,
    /** Set when another screen opened the list to pick games instead of opening them. */
    pick: GamePick?,
) {
    val navigationState = LocalNavigationState.current
    val picks = koinInject<GamePicks>()
    var selected by rememberSaveable { mutableStateOf(listOf<Int>()) }

    fun finishPick(request: String, ids: List<Int>) {
        picks.deliver(request, ids)
        navigationState.navigateBack()
    }
    val viewModel = koinViewModel<GameListViewModel> {
        parametersOf(section, eventId, preset, libraryStatus, libraryUserId)
    }
    // Nothing to scope the list to: the search opened from Discover, over users too.
    if (listOf(section, eventId, preset, libraryStatus, pick).all { it == null }) {
        GlobalSearch(viewModel)
        return
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterOptions.state.collectAsStateWithLifecycle()
    ScreenScaffold(
        topBar = {
            GameListSearchBar(
                placeholder = eventName
                    ?: preset?.name
                    ?: stringResource(libraryStatus?.displayName ?: Res.string.search__global_hint),
                query = uiState.filterState.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                onSearch = viewModel::submitSearchQuery,
                onBack = navigationState::navigateBack,
                autoFocus = pick != null,
                trailingActions = {
                    FilterButton(
                        hasActiveFilters = uiState.filterState.hasActiveFilters,
                        onClick = { viewModel.toggleFilterSheet(true) }
                    )
                }
            )
        }
    ) {
        GameListBody(uiState, filterOptions, viewModel, section, preset, selected.toSet()) { game ->
            if (pick == null) {
                navigationState.backStack.add(game.detailNavKey)
            } else {
                pickGame(game.id.toInt(), pick, selected, onSelect = { selected = it }) { finishPick(pick.request, it) }
            }
        }
        pick?.let { PickConfirmButton(selected.size) { finishPick(it.request, selected) } }
    }

    GameListFilters(
        uiState,
        filterOptions,
        viewModel,
        if (libraryStatus != null) FilterSheetScope.LIBRARY else FilterSheetScope.GAMES
    )
}

/** The filter sheet, while it is open. */
@Composable
internal fun GameListFilters(
    uiState: GameListViewModel.UiState,
    filterOptions: FilterOptions.State,
    viewModel: GameListViewModel,
    scope: FilterSheetScope,
) {
    if (uiState.showFilterSheet) {
        GameListFilterSheet(
            filterState = uiState.filterState,
            scope = scope,
            options = filterOptions,
            onFilterSearch = viewModel.filterOptions::search,
            onFilterChange = viewModel::updateFilter,
            onReset = viewModel::resetFilter,
            onDismiss = { viewModel.toggleFilterSheet(false) }
        ) {
            GridColumnsSection(uiState.columnCount, viewModel::setColumnCount)
        }
    }
}

/** Toggles [gameId] among [selected] when picking several games, or hands it back right away. */
private fun pickGame(
    gameId: Int,
    pick: GamePick,
    selected: List<Int>,
    onSelect: (List<Int>) -> Unit,
    onPicked: (List<Int>) -> Unit,
) = when {
    !pick.multiple -> onPicked(listOf(gameId))
    gameId in selected -> onSelect(selected - gameId)
    else -> onSelect(selected + gameId)
}

/** The active filters above the cover grid; the games in [selected] show as picked. */
@Composable
internal fun GameListBody(
    uiState: GameListViewModel.UiState,
    filterOptions: FilterOptions.State,
    viewModel: GameListViewModel,
    section: DiscoverSection?,
    preset: GameListPreset?,
    selected: Set<Int>,
    onGameClick: (Game) -> Unit,
) = Column(Modifier.fillMaxSize()) {
    AnimatedVisibility(visible = uiState.filterState.hasActiveFilters) {
        ActiveFilterChips(
            filterState = uiState.filterState,
            knownOptions = filterOptions.knownOptions,
            presetPlatform = preset?.takeIf { it.type == GameListPresetType.PLATFORM },
            onFilterChange = viewModel::updateFilter,
            onReset = viewModel::resetFilter,
            modifier = Modifier.padding(top = ChipRowPadding)
        )
    }
    GameListResults(
        uiState = uiState,
        cardMetadata = { game ->
            uiState.libraryEntries[game.id]?.cardMetadata() ?: listOfNotNull(section?.cardMetadata(game))
        },
        onGameClick = onGameClick,
        onEndReached = viewModel::onEndReached,
        modifier = Modifier.weight(1f),
        selected = selected,
        header = uiState.event?.let { event -> { EventHeader(event) } }
    )
}

/** Confirms the games picked so far, over the grid's corner; it grows in with the first one. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PickConfirmButton(count: Int, onClick: () -> Unit) =
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(Dimens.ScreenPadding), Alignment.BottomEnd) {
        AnimatedVisibility(
            visible = count > 0,
            enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
            exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
        ) {
            ExtendedFloatingActionButton(
                onClick = onClick,
                icon = { Icon(Icons.CheckW500Rounded, contentDescription = null) },
                text = {
                    AnimatedContent(count) { shown ->
                        Text(pluralStringResource(Res.plurals.gamelist__pick_confirm, shown, shown))
                    }
                },
            )
        }
    }

/**
 * Opens the filter sheet. Checked means "filters are active": the button morphs from round to
 * rounded-square and fills with the primary container, instead of carrying a badge dot. Tapping
 * always opens the sheet; it never clears the filters.
 */
@Composable
internal fun FilterButton(hasActiveFilters: Boolean, onClick: () -> Unit) = IconToggleButton(
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
