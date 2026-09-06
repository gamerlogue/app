package it.maicol07.gamerlogue.ui.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTreeBuilder

/**
 * The app's [SharedTransitionScope], provided around the [NavDisplay] so any screen can opt a
 * composable into a shared-element transition (e.g. a game cover). Null outside the nav host.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * Hosts the Navigation 3 display: builds the list-detail adaptive strategy and feeds it the entries
 * nav3ksp generated from branches declared beside their feature screens.
 *
 * Deliberately not nav3ksp's own `NavDisplay` proxy: that one takes neither entry decorators (the
 * per-entry ViewModelStore the game list relies on) nor the shared-transition scope, transition
 * specs and `onBack` used here.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavDisplay(
    navigationState: AppNavigationState,
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
        val provider = entryProvider {
            with(RootNavTreeBuilder) { buildTree() }
        }
        // Keep every stack's decorators alive when its tab is not displayed.
        val entriesByStack = navigationState.backStacks.mapValues { (root, stack) ->
            key(root) {
                rememberDecoratedNavEntries(
                    backStack = stack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = provider
                )
            }
        }
        val entries = entriesByStack.getValue(navigationState.currentRoot)

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
                onBack = navigationState::navigateBack
            )
        }
    }
}

private const val TransitionMillis = 250
