package it.maicol07.gamerlogue.ui.navigation

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import at.released.igdbclient.model.Event
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.gamelist__filter_title
import gamerlogue.sharedui.generated.resources.nav__detail_placeholder
import gamerlogue.sharedui.generated.resources.nav__settings
import gamerlogue.sharedui.generated.resources.search__global_hint
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SettingsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.TuneW500Rounded
import it.maicol07.gamerlogue.NavBackStack
import it.maicol07.gamerlogue.NavKeys
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.ui.components.event.EventHeader
import it.maicol07.gamerlogue.ui.components.search.GameListSearchBar
import it.maicol07.gamerlogue.ui.components.search.GameSearchButton
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import it.maicol07.gamerlogue.ui.views.calendar.Calendar
import it.maicol07.gamerlogue.ui.views.discover.DiscoverScreen
import it.maicol07.gamerlogue.ui.views.events.EventListScreen
import it.maicol07.gamerlogue.ui.views.game.GameDetailScreen
import it.maicol07.gamerlogue.ui.views.library.Library
import it.maicol07.gamerlogue.ui.views.list.GameListResults
import it.maicol07.gamerlogue.ui.views.list.GameListViewModel
import it.maicol07.gamerlogue.ui.views.list.hasActiveFilters
import it.maicol07.gamerlogue.ui.views.profile.ProfileScreen
import it.maicol07.gamerlogue.ui.views.settings.SettingsScreen
import it.maicol07.gamerlogue.ui.views.settings.categories.AppearanceScreen
import it.maicol07.gamerlogue.ui.views.settings.categories.LibraryImportPreviewScreen
import it.maicol07.gamerlogue.ui.views.settings.categories.LinkedServicesScreen
import it.maicol07.gamerlogue.ui.views.settings.categories.ServiceSyncScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Dot marking active filters; slightly larger than Material's default 6.dp badge. */
private val FilterBadgeSize = 9.dp

/** How far the dot is pulled in from the icon button's corner. */
private val FilterBadgeInset = 4.dp

/** Browse destinations: the Discover carousels, the event list and the paginated game list. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.browseEntries(
    backStack: NavBackStack,
    navigateToGame: (Game) -> Unit,
    navigateToEventGames: (Event) -> Unit,
) {
    screen<NavKeys.Discover>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = { Text(stringResource(Res.string.nav__detail_placeholder)) }
        ),
        topBar = {
            GameSearchButton(
                placeholder = stringResource(Res.string.search__global_hint),
                onClick = { backStack.add(NavKeys.GameList()) }
            )
        }
    ) {
        DiscoverScreen(
            onGameClick = navigateToGame,
            onSeeAllClick = { backStack.add(NavKeys.GameList(it)) },
            onEventClick = navigateToEventGames,
            onSeeAllEventsClick = { backStack.add(NavKeys.EventList) }
        )
    }
    screen<NavKeys.EventList>(metadata = ListDetailSceneStrategy.listPane()) {
        EventListScreen(onEventClick = navigateToEventGames)
    }
    screen<NavKeys.GameList>(
        metadata = ListDetailSceneStrategy.listPane(),
        topBar = { navKey -> GameListTopBar(navKey, backStack) }
    ) { navKey ->
        val viewModel = koinViewModel<GameListViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        LaunchedEffect(navKey) { viewModel.start(navKey.section, navKey.eventId) }
        GameListResults(
            viewModel = viewModel,
            onGameClick = navigateToGame,
            header = uiState.event?.let { event -> { EventHeader(event) } }
        )
    }
}

@Composable
private fun GameListTopBar(navKey: NavKeys.GameList, backStack: NavBackStack) {
    // Same NavEntry ViewModelStore as the content below, so both share one instance.
    val viewModel = koinViewModel<GameListViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    GameListSearchBar(
        // In event scope the event's name identifies the list; there is no title bar.
        placeholder = navKey.eventName ?: stringResource(Res.string.search__global_hint),
        query = uiState.filterState.searchQuery,
        onSearch = viewModel::setSearchQuery,
        onBack = { backStack.removeLastOrNull() },
        // Arriving on a Discover section or an event is a browse intent, not a typing one.
        autoFocus = navKey.section == null && navKey.eventId == null,
        trailingActions = {
            // Anchored on the button, not the icon, so the dot sits on the button's corner
            // instead of overlapping the glyph.
            BadgedBox(
                badge = {
                    if (uiState.filterState.hasActiveFilters) {
                        Badge(
                            // Badge's own dot is 6.dp; a fixed size wins over its internal
                            // defaultMinSize. The offset pulls it in from the button's corner
                            // towards the icon (mirrored automatically in RTL).
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

/** Signed-in destinations: they fall back to the login view while there is no session. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.accountEntries(
    backStack: NavBackStack,
    navigateToGame: (Game) -> Unit,
) {
    screen<NavKeys.Library>(metadata = ListDetailSceneStrategy.listPane()) {
        AuthenticatedContent { Library(onGameClick = navigateToGame) }
    }
    screen<NavKeys.Calendar>(metadata = ListDetailSceneStrategy.detailPane()) {
        AuthenticatedContent { Calendar() }
    }
    screen<NavKeys.Profile>(
        metadata = ListDetailSceneStrategy.detailPane(),
        actions = {
            IconButton(onClick = { backStack.add(NavKeys.Settings) }) {
                Icon(Icons.SettingsW500Rounded, contentDescription = stringResource(Res.string.nav__settings))
            }
        }
    ) { ProfileScreen() }
}

@Composable
private fun AuthenticatedContent(content: @Composable () -> Unit) {
    val authProvider = koinInject<AuthTokenProvider>()
    val accessToken by authProvider.accessToken.collectAsState()
    if (accessToken == null) LoginView() else content()
}

/** Settings and everything reachable from it, including the linked-services sync flow. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.settingsEntries(backStack: NavBackStack) {
    screen<NavKeys.Settings>(metadata = ListDetailSceneStrategy.detailPane()) {
        SettingsScreen(navigateTo = { backStack.add(it) })
    }
    screen<NavKeys.LinkedServices>(metadata = ListDetailSceneStrategy.detailPane()) {
        LinkedServicesScreen(
            navigateToSync = { service, action -> backStack.add(NavKeys.ServiceSync(service, action)) }
        )
    }
    // Draws its own chrome (a persistent bottom sheet hosting the WebView), so plain `entry`.
    entry<NavKeys.ServiceSync>(metadata = ListDetailSceneStrategy.detailPane()) { navKey ->
        ServiceSyncScreen(
            service = navKey.service,
            action = navKey.action,
            onFinish = { backStack.removeLastOrNull() },
            navigateToImportPreview = { service, mode ->
                backStack.removeLastOrNull()
                backStack.add(NavKeys.LibraryImportPreview(service, mode))
            },
        )
    }
    screen<NavKeys.LibraryImportPreview>(metadata = ListDetailSceneStrategy.detailPane()) { navKey ->
        LibraryImportPreviewScreen(
            service = navKey.service,
            mode = navKey.mode,
            onDone = { backStack.removeLastOrNull() }
        )
    }
    screen<NavKeys.Appearance>(metadata = ListDetailSceneStrategy.detailPane()) {
        AppearanceScreen()
    }
}

/** The game detail, which draws its own collapsing overlay bar and so uses plain `entry`. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.gameEntries(navigateToGame: (Game) -> Unit) {
    entry<NavKeys.GameDetail>(metadata = ListDetailSceneStrategy.detailPane()) { navKey ->
        GameDetailScreen(
            gameId = navKey.gameId,
            coverImageId = navKey.coverImageId,
            gameName = navKey.gameName,
            onGameClick = navigateToGame,
        )
    }
}
