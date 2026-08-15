package it.maicol07.gamerlogue.ui.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
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
import at.released.igdbclient.model.Event
import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.NavBackStack
import it.maicol07.gamerlogue.NavKeys
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.views.game.GameHandoff

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
internal inline fun <reified K : NavKey> EntryProviderScope<NavKey>.screen(
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
 * Hosts the Navigation 3 display: builds the list-detail adaptive strategy, registers the entries of
 * each feature (see NavEntries.kt), and wires navigation as callbacks so the screens stay
 * navigation-free.
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
    val navigateToGame: (Game) -> Unit = { game ->
        GameHandoff.put(game)
        backStack.add(NavKeys.GameDetail(game.id.toInt()))
    }
    val navigateToEventGames: (Event) -> Unit = { event ->
        backStack.add(NavKeys.GameList(eventId = event.id.toInt(), eventName = event.name))
    }

    SharedTransitionLayout {
        val sharedScope = this
        val entries = rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                browseEntries(backStack, navigateToGame, navigateToEventGames)
                accountEntries(backStack, navigateToGame)
                settingsEntries(backStack)
                gameEntries(navigateToGame)
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
