package it.maicol07.gamerlogue

import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation3.runtime.NavBackStack
import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavigationStateTest {
    @Test
    fun eachTopLevelDestinationKeepsItsBackStack() {
        val discover = NavBackStack<AppNavKey>(NavKeys.Discover)
        val library = NavBackStack<AppNavKey>(NavKeys.Library)
        val state = AppNavigationState(
            mutableIntStateOf(0),
            listOf(
                discover,
                library,
                NavBackStack(NavKeys.Calendar),
                NavBackStack(NavKeys.Profile),
            ),
        )

        discover.add(NavKeys.Settings)
        state.selectRoot(NavKeys.Library)
        library.add(NavKeys.Settings)
        state.selectRoot(NavKeys.Discover)

        assertEquals(NavKeys.Settings, state.backStack.last())
        state.selectRoot(NavKeys.Discover)
        assertEquals(listOf(NavKeys.Discover), state.backStack.toList())
    }
}
