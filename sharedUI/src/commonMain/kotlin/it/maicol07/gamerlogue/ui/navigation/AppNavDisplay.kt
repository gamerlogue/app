package it.maicol07.gamerlogue.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTreeBuilder

/**
 * The app's [SharedTransitionScope], provided around the [NavDisplay] so any screen can opt a
 * composable into a shared-element transition (e.g. a game cover). Null outside the nav host.
 */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

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
    val motion = MaterialTheme.motionScheme

    SharedTransitionLayout {
        val sharedScope = this
        val provider = remember {
            entryProvider { with(RootNavTreeBuilder) { buildTree() } }
        }
        // Keep every stack's decorators alive when its tab is not displayed.
        val entriesByStack = navigationState.backStacks.mapValues { (root, stack) ->
            key(root) {
                rememberDecoratedNavEntries(
                    backStack = stack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                        BlockExitingInputDecorator
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
                transitionSpec = {
                    // A tab switch swaps the whole stack: nothing on screen belongs to the new one.
                    // ponytail: matched by contentKey, so a destination open in both tabs slides
                    // instead of fading through.
                    val isTabSwitch = initialState.entries.none { old ->
                        entries.any { it.contentKey == old.contentKey }
                    }
                    if (isTabSwitch) fadeThrough(motion) else sharedAxisX(motion, direction = 1)
                },
                popTransitionSpec = { sharedAxisX(motion, direction = -1) },
                predictivePopTransitionSpec = { sharedAxisX(motion, direction = -1) },
                modifier = modifier.fillMaxSize(),
                onBack = navigationState::navigateBack
            )
        }
    }
}

/**
 * Keeps a leaving entry from taking taps meant for the incoming one. On pop the leaving screen stays on
 * top until the whole transition settles, long after it has faded out: input is swallowed while it is
 * still visible, then it is left unplaced (still composed) so hit testing skips it.
 */
private val BlockExitingInputDecorator = NavEntryDecorator<NavKey> { entry ->
    val transition = LocalNavAnimatedContentScope.current.transition
    val motion = MaterialTheme.motionScheme
    // Same spec as the exit fade in sharedAxisX/fadeThrough, so it reaches 0 when the screen vanishes.
    val exitAlpha = transition.animateFloat({ motion.fastEffectsSpec() }) {
        if (it == EnterExitState.PostExit) 0f else 1f
    }
    val exiting = transition.targetState == EnterExitState.PostExit
    Layout(
        content = {
            Box(if (exiting) Modifier.pointerInput(Unit) { consumeAllPointerEvents() } else Modifier) {
                entry.Content()
            }
        }
    ) { measurables, constraints ->
        val placeable = measurables.single().measure(constraints)
        layout(placeable.width, placeable.height) {
            if (exitAlpha.value > 0f) placeable.place(0, 0)
        }
    }
}

private suspend fun PointerInputScope.consumeAllPointerEvents(): Unit = awaitPointerEventScope {
    while (true) {
        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
    }
}

/**
 * Shared axis X, offset by a quarter of the width rather than M3's 30 dp: between look-alike screens
 * (settings lists) the stock offset reads as an in-place swap. [direction] is 1 forward, -1 back.
 */
private fun sharedAxisX(motion: MotionScheme, direction: Int): ContentTransform {
    val enter = slideInHorizontally(motion.defaultSpatialSpec()) { it * direction / SlideFraction } +
        fadeIn(motion.defaultEffectsSpec())
    val exit = slideOutHorizontally(motion.defaultSpatialSpec()) { -it * direction / SlideFraction } +
        fadeOut(motion.fastEffectsSpec())
    return enter togetherWith exit
}

/** M3 fade through, for switching between top-level destinations. */
private fun fadeThrough(motion: MotionScheme): ContentTransform =
    (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = FadeThroughScale))
        .togetherWith(fadeOut(motion.fastEffectsSpec()))

private const val SlideFraction = 4
private const val FadeThroughScale = 0.92f
