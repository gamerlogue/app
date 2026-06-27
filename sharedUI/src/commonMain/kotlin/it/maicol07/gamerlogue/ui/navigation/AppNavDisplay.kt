package it.maicol07.gamerlogue.ui.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.nav__detail_placeholder
import gamerlogue.sharedui.generated.resources.nav__settings
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SettingsW500Rounded
import it.maicol07.gamerlogue.NavBackStack
import it.maicol07.gamerlogue.NavKeys
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import it.maicol07.gamerlogue.ui.views.calendar.Calendar
import it.maicol07.gamerlogue.ui.views.discover.DiscoverScreen
import it.maicol07.gamerlogue.ui.views.game.GameDetailScreen
import it.maicol07.gamerlogue.ui.views.game.GameHandoff
import it.maicol07.gamerlogue.ui.views.library.Library
import it.maicol07.gamerlogue.ui.views.list.GameListScreen
import it.maicol07.gamerlogue.ui.views.profile.ProfileScreen
import it.maicol07.gamerlogue.ui.views.settings.SettingsScreen
import it.maicol07.gamerlogue.ui.views.settings.categories.AppearanceScreen
import it.maicol07.gamerlogue.ui.views.settings.categories.LibraryImportPreviewScreen
import it.maicol07.gamerlogue.ui.views.settings.categories.LinkedServicesScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

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
    noinline actions: @Composable RowScope.() -> Unit = {},
    noinline content: @Composable (K) -> Unit
) = entry<K>(metadata = metadata) { key ->
    ScreenScaffold((key as? NavKeys.NavKeyWithMeta)?.title, actions = actions) { content(key) }
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
                    )
                ) {
                    DiscoverScreen(
                        onGameClick = { game -> GameHandoff.put(game); backStack.add(NavKeys.GameDetail(game.id.toInt())) },
                        onSeeAllClick = { section -> backStack.add(NavKeys.GameList(section)) }
                    )
                }
                screen<NavKeys.Library>(metadata = ListDetailSceneStrategy.listPane()) {
                    if (authProvider.accessToken == null) {
                        LoginView()
                    } else {
                        Library(onGameClick = { game -> GameHandoff.put(game); backStack.add(NavKeys.GameDetail(game.id.toInt())) })
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
                        navigateToImportPreview = { service, mode ->
                            backStack.add(NavKeys.LibraryImportPreview(service, mode))
                        }
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
                screen<NavKeys.GameList>(metadata = ListDetailSceneStrategy.detailPane()) { navKey ->
                    GameListScreen(
                        section = navKey.section,
                        onGameClick = { game -> GameHandoff.put(game); backStack.add(NavKeys.GameDetail(game.id.toInt())) }
                    )
                }
                // Draws its own collapsing overlay bar, so it uses plain `entry`, not `screen`.
                entry<NavKeys.GameDetail>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) { navKey ->
                    GameDetailScreen(gameId = navKey.gameId)
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
