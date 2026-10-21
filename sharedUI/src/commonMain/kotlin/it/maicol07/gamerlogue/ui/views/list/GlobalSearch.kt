package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.profile__search_users
import gamerlogue.sharedui.generated.resources.search__global_hint
import gamerlogue.sharedui.generated.resources.search__scope_games
import gamerlogue.sharedui.generated.resources.search__scope_users
import gamerlogue.sharedui.generated.resources.social__no_users
import gamerlogue.sharedui.generated.resources.social__search_hint
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.GroupW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SearchW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.detailNavKey
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.search.GameListSearchBar
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.AuthenticatedContent
import it.maicol07.gamerlogue.ui.views.social.CenteredEmptyState
import it.maicol07.gamerlogue.ui.views.social.UserPagedList
import it.maicol07.gamerlogue.ui.views.social.UserSearchViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** What the global search looks for. */
private enum class SearchScope(val label: StringResource, val icon: ImageVector) {
    GAMES(Res.string.search__scope_games, Icons.JoystickW500Rounded),
    USERS(Res.string.search__scope_users, Icons.GroupW500Rounded),
}

/**
 * The search opened from Discover: games, with their filters, or users, chosen above the results.
 * The text carries over when switching, so the same name can be looked up both ways.
 */
@Composable
internal fun GlobalSearch(viewModel: GameListViewModel) {
    val navigationState = LocalNavigationState.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterOptions.state.collectAsStateWithLifecycle()
    val userSearch = koinViewModel<UserSearchViewModel>()
    val userQuery by userSearch.query.collectAsStateWithLifecycle()
    var scope by rememberSaveable { mutableStateOf(SearchScope.GAMES) }
    val users = scope == SearchScope.USERS
    ScreenScaffold(
        topBar = {
            GameListSearchBar(
                placeholder = stringResource(
                    if (users) Res.string.profile__search_users else Res.string.search__global_hint
                ),
                query = if (users) userQuery else uiState.filterState.searchQuery,
                onQueryChange = if (users) userSearch::setQuery else viewModel::setSearchQuery,
                onSearch = if (users) userSearch::setQuery else viewModel::submitSearchQuery,
                onBack = navigationState::navigateBack,
                trailingActions = {
                    AnimatedVisibility(!users) {
                        FilterButton(uiState.filterState.hasActiveFilters) { viewModel.toggleFilterSheet(true) }
                    }
                }
            )
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            SearchScopeChooser(scope) { chosen ->
                // The other side catches up with the text typed so far.
                if (chosen == SearchScope.USERS) {
                    userSearch.setQuery(uiState.filterState.searchQuery)
                } else {
                    viewModel.setSearchQuery(userQuery)
                }
                scope = chosen
            }
            AnimatedContent(scope, transitionSpec = { fadeIn() togetherWith fadeOut() }) { shown ->
                when (shown) {
                    SearchScope.GAMES -> GameListBody(uiState, filterOptions, viewModel, null, null, emptySet()) {
                        navigationState.backStack.add(it.detailNavKey)
                    }
                    SearchScope.USERS -> AuthenticatedContent { UserResults(userSearch, userQuery) }
                }
            }
        }
    }
    GameListFilters(uiState, filterOptions, viewModel, FilterSheetScope.GAMES)
}

@Composable
private fun UserResults(userSearch: UserSearchViewModel, query: String) = if (query.isBlank()) {
    CenteredEmptyState(Icons.SearchW500Rounded, stringResource(Res.string.social__search_hint))
} else {
    UserPagedList(
        userSearch.users,
        Icons.SearchW500Rounded,
        stringResource(Res.string.social__no_users),
        loadOnStart = false
    )
}

/** Games or users, as connected toggle buttons whose shapes morph as they are pressed and checked. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchScopeChooser(selected: SearchScope, onSelect: (SearchScope) -> Unit) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemGap),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
) {
    SearchScope.entries.forEachIndexed { index, scope ->
        ToggleButton(
            checked = scope == selected,
            onCheckedChange = { onSelect(scope) },
            shapes = if (index == 0) {
                ButtonGroupDefaults.connectedLeadingButtonShapes()
            } else {
                ButtonGroupDefaults.connectedTrailingButtonShapes()
            },
            modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
        ) {
            Icon(scope.icon, contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize))
            Text(
                stringResource(scope.label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = ToggleButtonDefaults.IconSpacing)
            )
        }
    }
}
