package it.maicol07.gamerlogue

import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import it.maicol07.gamerlogue.ui.navigation.AppNavigationState
import it.maicol07.gamerlogue.ui.navigation.TopLevelNavKeys
import it.maicol07.gamerlogue.ui.navigation.showsNavigationSuite
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationStateTest {
    @Test
    fun navigationSuiteVisibilityFollowsDestination() {
        assertTrue(RootNavTree.Discover.showsNavigationSuite)
        assertFalse(RootNavTree.LinkedServices.showsNavigationSuite)
        assertFalse(RootNavTree.GameDetail(1, null, null).showsNavigationSuite)
    }

    @Test
    fun eachTopLevelDestinationKeepsItsBackStack() {
        val backStacks = TopLevelNavKeys.associateWith { NavBackStack<NavKey>(it) }
        val discover = backStacks.getValue(RootNavTree.Discover)
        val library = backStacks.getValue(RootNavTree.Library)
        val state = AppNavigationState(
            mutableIntStateOf(0),
            backStacks,
        )

        discover.add(RootNavTree.Settings)
        state.selectRoot(RootNavTree.Library)
        library.add(RootNavTree.Settings)
        state.selectRoot(RootNavTree.Discover)

        assertEquals(RootNavTree.Settings, state.backStack.last())
        state.selectRoot(RootNavTree.Discover)
        assertEquals(listOf(RootNavTree.Discover), state.backStack.toList())
    }
}
