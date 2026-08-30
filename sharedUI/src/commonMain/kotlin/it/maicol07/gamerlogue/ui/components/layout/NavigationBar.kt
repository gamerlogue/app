package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
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
import it.maicol07.gamerlogue.AppNavigationState
import it.maicol07.gamerlogue.NavKeys
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun NavigationSuiteScope.appNavigationItems(navigationState: AppNavigationState) {
    for (item in NavBarItem.entries) {
        val selected = navigationState.currentRoot == item.navKey
        item(
            selected = selected,
            onClick = { navigationState.selectRoot(item.navKey) },
            icon = { Icon(if (selected) item.iconSelected else item.icon, null) },
            label = { Text(stringResource(item.title)) },
        )
    }
}

private enum class NavBarItem(
    val navKey: AppNavKey,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val title: StringResource,
) {
    Discover(NavKeys.Discover, Icons.ExploreW500Rounded, Icons.ExploreW500RoundedFill, Res.string.nav__discover),
    Library(NavKeys.Library, Icons.NewsstandW500Rounded, Icons.NewsstandW500RoundedFill, Res.string.nav__library),
    Calendar(
        NavKeys.Calendar,
        Icons.CalendarMonthW500Rounded,
        Icons.CalendarMonthW500RoundedFill,
        Res.string.nav__calendar,
    ),
    Profile(NavKeys.Profile, Icons.PersonW500Rounded, Icons.PersonW500RoundedFill, Res.string.nav__profile),
}
