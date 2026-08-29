package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.nav__calendar
import gamerlogue.sharedui.generated.resources.nav__discover
import gamerlogue.sharedui.generated.resources.nav__library
import gamerlogue.sharedui.generated.resources.nav__profile
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CalendarMonthW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CalendarMonthW500RoundedFill
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ExploreW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ExploreW500RoundedFill
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.NewsstandW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.NewsstandW500RoundedFill
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonW500RoundedFill
import it.maicol07.gamerlogue.AppNavKey
import it.maicol07.gamerlogue.LocalNavBackStack
import it.maicol07.gamerlogue.AppNavBackStack
import it.maicol07.gamerlogue.NavKeys
import it.maicol07.gamerlogue.NavigationBarContrastEnforced
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The bottom bar reads the current destination itself instead of receiving it: it is the only global
 * chrome that depends on it, so the snapshot read stays scoped here and a navigation does not
 * invalidate the whole app shell.
 */
@Composable
fun AppNavigationBar(
    backStack: AppNavBackStack = LocalNavBackStack.current
) {
    val currentNavKey = backStack.lastOrNull()
    val showBottomBar = currentNavKey?.showBottomBar ?: true
    // The system scrim behind a transparent navigation bar follows the bar's own visibility.
    NavigationBarContrastEnforced(!showBottomBar)

    AnimatedVisibility(
        showBottomBar,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically(tween(200, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(200))
    ) {
        NavigationBar {
            for (item in NavBarItems.entries) {
                val selected = remember(currentNavKey) { currentNavKey == item.navKey }
                NavigationBarItem(
                    icon = {
                        Icon(if (selected) item.iconSelected else item.icon, null)
                    },
                    label = { item.title?.let { Text(stringResource(item.title)) } ?: "" },
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            backStack.clear()
                            backStack.add(item.navKey)
                        } else {
                            // When we click again on a bottom bar item and it was already selected
                            // we want to pop the back stack until the initial destination of this bottom bar item

                            // Find the last occurrence of the selected navKey in the back stack and remove all entries after it
                            val lastIndex = backStack.indexOfLast { it == item.navKey }
                            if (lastIndex != -1 && lastIndex < backStack.size - 1) {
                                backStack.subList(lastIndex + 1, backStack.size).clear()
                            }
                        }
                    }
                )
            }
        }
    }
}

enum class NavBarItems(
    val navKey: AppNavKey,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val title: StringResource? = null
) {
    Discover(
        NavKeys.Discover,
        Icons.ExploreW500Rounded,
        Icons.ExploreW500RoundedFill,
        Res.string.nav__discover
    ),
    Library(
        NavKeys.Library,
        Icons.NewsstandW500Rounded,
        Icons.NewsstandW500RoundedFill,
        Res.string.nav__library
    ),
    Calendar(
        NavKeys.Calendar,
        Icons.CalendarMonthW500Rounded,
        Icons.CalendarMonthW500RoundedFill,
        Res.string.nav__calendar
    ),
    Profile(
        NavKeys.Profile,
        Icons.PersonW500Rounded,
        Icons.PersonW500RoundedFill,
        Res.string.nav__profile
    ),
//    Settings(NavKeys.Settings, { Icon(Icons.Outlined.Settings, null) }, { Icon(Icons.Rounded.Settings, null) })
}
