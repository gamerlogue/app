package it.maicol07.gamerlogue.ui.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.search.GameSearchBar
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import it.maicol07.gamerlogue.ui.views.calendar.Calendar
import it.maicol07.gamerlogue.ui.views.discover.DiscoverScreen
import it.maicol07.gamerlogue.ui.views.game.GameDetailScreen
import it.maicol07.gamerlogue.ui.views.game.GameHandoff
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

/**
 * The app's [SharedTransitionScope], provided around the [NavDisplay] so any screen can opt a
 * composable into a shared-element transition (e.g. a game cover). Null outside the nav host.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * Like [entry], but wraps the destination's content in a [ScreenScaffold] whose title is taken from
 * [NavKeys.NavKeyWithMeta.title]. Use plain [entry] for destinations that draw their own bar (e.g.
 * the game detail screen with its collapsing overlay bar).
 */
private inline fun <reified K : NavKey> EntryProviderScope<NavKey>.screen(
    metadata: Map<String, Any> = emptyMap(),
    noinline actions: @Composable RowScope.(K) -> Unit = {},
    noinline topBar: (@Composable (K) -> Unit)? = null,
    noinline content: @Composable (K) -> Unit
) = entry<K>(metadata = metadata) { key ->
    ScreenScaffold(
        title = (key as? NavKeys.NavKeyWithMeta)?.title,
        actions = { actions(key) },
        topBar = topBar?.let { bar -> { bar(key) } }
    ) { content(key) }
}

/**
 * Hosts the Navigation 3 display: builds the list-detail adaptive strategy, registers an entry
 * per [NavKeys] destination, and wires navigation as callbacks so the screens stay navigation-free.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavDisplay(
    backStack: NavBackStack,
    modifier: Modifier = Modifier,
) {
    // Override the defaults so that there isn't a horizontal space between the panes.
    // See b/418201867
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    val navigateToGame: (Game) -> Unit = { game -> GameHandoff.put(game); backStack.add(NavKeys.GameDetail(game.id.toInt())) }

    SharedTransitionLayout {
        val sharedScope = this
        val entries = rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                val authProvider = koinInject<AuthTokenProvider>()

                screen<NavKeys.Discover>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            Text(stringResource(Res.string.nav__detail_placeholder))
                        }
                    ),
                    topBar = {
                        // Same NavEntry ViewModelStore as the content below, so both share one instance.
                        val viewModel = koinViewModel<GameListViewModel>()
                        val uiState by viewModel.uiState.collectAsState()
                        GameSearchBar(
                            placeholder = stringResource(Res.string.search__global_hint),
                            expanded = uiState.expanded,
                            onExpandedChange = viewModel::setExpanded,
                            onSearch = viewModel::setSearchQuery,
                            query = uiState.filterState.searchQuery,
                            trailingActions = {
                                // Only meaningful once the results pane is open, which is what it filters.
                                if (uiState.expanded) {
                                    // Anchored on the button, not the icon, so the dot sits on the
                                    // button's corner instead of overlapping the glyph.
                                    BadgedBox(
                                        badge = {
                                            if (uiState.filterState.hasActiveFilters) {
                                                Badge(
                                                    // Badge's own dot is 6.dp; a fixed size wins
                                                    // over its internal defaultMinSize. The offset
                                                    // pulls it in from the button's corner towards
                                                    // the icon (mirrored automatically in RTL).
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
                            }
                        ) {
                            GameListResults(viewModel = viewModel, onGameClick = navigateToGame)
                        }
                    }
                ) {
                    val viewModel = koinViewModel<GameListViewModel>()
                    DiscoverScreen(
                        onGameClick = navigateToGame,
                        onSeeAllClick = viewModel::showSection
                    )
                }
                screen<NavKeys.Library>(metadata = ListDetailSceneStrategy.listPane()) {
                    if (authProvider.accessToken == null) {
                        LoginView()
                    } else {
                        Library(onGameClick = navigateToGame)
                    }
                }
                screen<NavKeys.Calendar>(metadata = ListDetailSceneStrategy.detailPane()) {
                    if (authProvider.accessToken == null) LoginView() else Calendar()
                }
                screen<NavKeys.Profile>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                    actions = {
                        IconButton(onClick = { backStack.add(NavKeys.Settings) }) {
                            Icon(
                                Icons.SettingsW500Rounded,
                                contentDescription = stringResource(Res.string.nav__settings)
                            )
                        }
                    }
                ) { ProfileScreen() }
                screen<NavKeys.Settings>(metadata = ListDetailSceneStrategy.detailPane()) {
                    SettingsScreen(navigateTo = { backStack.add(it) })
                }
                screen<NavKeys.LinkedServices>(metadata = ListDetailSceneStrategy.detailPane()) {
                    LinkedServicesScreen(
                        navigateToSync = { service, action ->
                            backStack.add(NavKeys.ServiceSync(service, action))
                        }
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
                // Draws its own collapsing overlay bar, so it uses plain `entry`, not `screen`.
                entry<NavKeys.GameDetail>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) { navKey ->
                    GameDetailScreen(
                        gameId = navKey.gameId,
                        onGameClick = navigateToGame
                    )
                }
            }
        )

        CompositionLocalProvider(LocalSharedTransitionScope provides sharedScope) {
            NavDisplay(
                entries = entries,
                sceneStrategies = listOf(listDetailStrategy),
                sharedTransitionScope = sharedScope,
                transitionSpec = { fadeIn(tween(TransitionMillis)) togetherWith fadeOut(tween(TransitionMillis)) },
                popTransitionSpec = { fadeIn(tween(TransitionMillis)) togetherWith fadeOut(tween(TransitionMillis)) },
                predictivePopTransitionSpec = {
                    fadeIn(tween(TransitionMillis)) togetherWith fadeOut(tween(TransitionMillis))
                },
                modifier = modifier.fillMaxSize(),
                onBack = { backStack.removeLastOrNull() }
            )
        }
    }
}

private const val TransitionMillis = 250

/** Dot marking active filters; slightly larger than Material's default 6.dp badge. */
private val FilterBadgeSize = 9.dp

/** How far the dot is pulled in from the icon button's corner. */
private val FilterBadgeInset = 4.dp
