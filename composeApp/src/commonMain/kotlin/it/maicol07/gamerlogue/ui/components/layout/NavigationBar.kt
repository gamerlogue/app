package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import gamerlogue.composeapp.generated.resources.Res
import gamerlogue.composeapp.generated.resources.nav__calendar
import gamerlogue.composeapp.generated.resources.nav__discover
import gamerlogue.composeapp.generated.resources.nav__library
import gamerlogue.composeapp.generated.resources.nav__profile
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CalendarMonthW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CalendarMonthW500Roundedfill1
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ExploreW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ExploreW500Roundedfill1
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.NewsstandW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.NewsstandW500Roundedfill1
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonW500Roundedfill1
import it.maicol07.gamerlogue.NavBackStack
import it.maicol07.gamerlogue.NavKeys
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AppNavigationBar(
    currentNavKey: NavKey,
    backStack: NavBackStack = koinInject()
) {
    AnimatedVisibility(
        (currentNavKey as? NavKeys.NavKeyWithMeta)?.showBottomBar ?: true,
        enter = slideInVertically(),
        exit = slideOutVertically()
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
    val navKey: NavKey,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val title: StringResource? = null
) {
    Discover(
        NavKeys.Discover,
        Icons.ExploreW500Rounded,
        Icons.ExploreW500Roundedfill1,
        Res.string.nav__discover
    ),
    Library(
        NavKeys.Library,
        Icons.NewsstandW500Rounded,
        Icons.NewsstandW500Roundedfill1,
        Res.string.nav__library
    ),
    Calendar(
        NavKeys.Calendar,
        Icons.CalendarMonthW500Rounded,
        Icons.CalendarMonthW500Roundedfill1,
        Res.string.nav__calendar
    ),
    Profile(
        NavKeys.Profile,
        Icons.PersonW500Rounded,
        Icons.PersonW500Roundedfill1,
        Res.string.nav__profile
    ),
//    Settings(NavKeys.Settings, { Icon(Icons.Outlined.Settings, null) }, { Icon(Icons.Rounded.Settings, null) })
}
