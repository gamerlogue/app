package it.maicol07.gamerlogue.ui.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.nav__detail_placeholder
import it.maicol07.gamerlogue.NavBackStack
import it.maicol07.gamerlogue.NavKeys
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import it.maicol07.gamerlogue.ui.views.calendar.Calendar
import it.maicol07.gamerlogue.ui.views.discover.DiscoverScreen
import it.maicol07.gamerlogue.ui.views.game.GameDetailScreen
import it.maicol07.gamerlogue.ui.views.library.Library
import it.maicol07.gamerlogue.ui.views.list.GameListScreen
import it.maicol07.gamerlogue.ui.views.profile.ProfileScreen
import it.maicol07.gamerlogue.ui.views.settings.SettingsScreen
import it.maicol07.gamerlogue.ui.views.settings.categories.AppearanceScreen
import it.maicol07.gamerlogue.ui.views.settings.categories.LinkedServicesScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

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
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    SharedTransitionLayout {
        val entries = rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                val authProvider = koinInject<AuthTokenProvider>()

                entry<NavKeys.Discover>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            Text(stringResource(Res.string.nav__detail_placeholder))
                        }
                    )
                ) {
                    DiscoverScreen(
                        onGameClick = { game ->
                            backStack.add(NavKeys.GameDetail(game.id.toInt()))
                        },
                        onSeeAllClick = { section ->
                            backStack.add(NavKeys.GameList(section))
                        }
                    )
                }
                entry<NavKeys.Library>(
                    metadata = ListDetailSceneStrategy.listPane()
                ) {
                    if (authProvider.accessToken == null) {
                        LoginView()
                    } else {
                        Library(
                            onGameClick = { game ->
                                backStack.add(NavKeys.GameDetail(game.id.toInt()))
                            }
                        )
                    }
                }
                entry<NavKeys.Calendar>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    if (authProvider.accessToken == null) {
                        LoginView()
                    } else {
                        Calendar()
                    }
                }
                entry<NavKeys.Profile>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) { ProfileScreen { backStack.add(it) } }
                entry<NavKeys.Settings>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    SettingsScreen(navigateTo = { backStack.add(it) })
                }
                entry<NavKeys.LinkedServices>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    LinkedServicesScreen()
                }
                entry<NavKeys.Appearance>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    AppearanceScreen()
                }
                entry<NavKeys.GameDetail>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) { navKey ->
                    GameDetailScreen(gameId = navKey.gameId)
                }
                entry<NavKeys.GameList>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) { navKey ->
                    GameListScreen(
                        section = navKey.section,
                        onGameClick = { game ->
                            backStack.add(NavKeys.GameDetail(game.id.toInt()))
                        }
                    )
                }
            }
        )

        NavDisplay(
            entries = entries,
            sceneStrategies = listOf(listDetailStrategy),
            sharedTransitionScope = this,
            modifier = modifier.fillMaxSize(),
            onBack = { backStack.removeLastOrNull() }
        )
    }
}
