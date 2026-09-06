package it.maicol07.gamerlogue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTreeLayout

/**
 * The app's back stack type. Named `App…` rather than shadowing [NavBackStack]: a typealias with the
 * same name as the class it aliases means the identifier denotes different things depending on which
 * imports a file happens to have.
 *
 * Keys are plain [NavKey]s: nav3ksp generates them (see `RootNavTree`) and generated keys can only
 * implement [NavKey], so there is no app-specific key supertype to narrow this to.
 */
typealias AppNavBackStack = NavBackStack<NavKey>

val TopLevelNavKeys: List<NavKey> =
    listOf(RootNavTree.Discover, RootNavTree.Library, RootNavTree.Calendar, RootNavTree.Profile)

/**
 * Indicates whether the given `NavKey` supports the navigation suite view. This property is
 * evaluated based on the specific type or instance of `NavKey`.
 *
 * Returns `false` for:
 * - `RootNavTree.LinkedServices` and `RootNavTree.Appearance`
 * - Instances of `RootNavTree.ServiceSync`, `RootNavTree.LibraryImportPreview`, and `RootNavTree.GameDetail`
 *
 * Returns `true` for all other cases.
 */
val NavKey.showsNavigationSuite: Boolean
    get() = when (this) {
        RootNavTree.LinkedServices,
        RootNavTree.Appearance -> false

        is RootNavTree.ServiceSync,
        is RootNavTree.LibraryImportPreview,
        is RootNavTree.GameDetail -> false

        else -> true
    }

/**
 * Remembers and returns an instance of [AppNavigationState] that is used to manage the app's
 * navigation state, including the back stack for each top-level navigation key and the
 * currently selected root index.
 *
 * @return An [AppNavigationState] instance maintaining the selected root index and back stacks
 * for top-level navigation keys.
 */
@Composable
fun rememberAppNavigationState(): AppNavigationState {
    val backStacks = TopLevelNavKeys.associateWith { RootNavTreeLayout.rememberTreeBackStack(it) }
    val selectedRootIndex = rememberSaveable { mutableIntStateOf(0) }

    return remember(selectedRootIndex, backStacks) {
        AppNavigationState(selectedRootIndex, backStacks)
    }
}

/**
 * Represents the state of the application's navigation, including the currently selected top-level
 * navigation key, and the back stack associated with each top-level navigation destination.
 *
 * @constructor Creates an instance of [AppNavigationState] with a mutable state for tracking the
 * selected root index, and a map of back stacks for managing navigation history across top-level
 * navigation keys.
 *
 * @property currentRoot The navigation key corresponding to the currently selected top-level
 * navigation destination.
 * @property backStack The back stack associated with the current root navigation key.
 *
 * @function selectRoot Updates the selected root navigation key. If the specified root is
 * already selected, it clears the back stack to retain only the initial destination. If a new
 * root is selected, it switches to the corresponding back stack.
 * @param root The navigation key of the root to be selected.
 * @throws IllegalArgumentException if the provided navigation key is not a valid top-level
 * destination.
 *
 * @function navigateBack Navigates backward by removing the last entry in the current back stack,
 * provided there is more than one entry remaining.
 */
class AppNavigationState(
    private val selectedRootIndex: MutableIntState,
    internal val backStacks: Map<NavKey, AppNavBackStack>,
) {
    val currentRoot: NavKey get() = TopLevelNavKeys[selectedRootIndex.intValue]
    val backStack: AppNavBackStack get() = backStacks.getValue(currentRoot)

    fun selectRoot(root: NavKey) {
        val index = TopLevelNavKeys.indexOf(root)
        require(index >= 0) { "Not a top-level destination: $root" }
        if (index == selectedRootIndex.intValue) {
            while (backStack.size > 1) backStack.removeLast()
        } else {
            selectedRootIndex.intValue = index
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) backStack.removeLast()
    }
}

val LocalNavigationState = staticCompositionLocalOf<AppNavigationState> {
    error("LocalNavigationState not provided")
}
