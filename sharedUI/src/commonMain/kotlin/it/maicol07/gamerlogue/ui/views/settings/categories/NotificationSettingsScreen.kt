package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__notifications
import gamerlogue.sharedui.generated.resources.settings__notifications_events
import gamerlogue.sharedui.generated.resources.settings__notifications_events_empty
import gamerlogue.sharedui.generated.resources.settings__notifications_following
import gamerlogue.sharedui.generated.resources.settings__notifications_following_summary
import gamerlogue.sharedui.generated.resources.settings__notifications_system
import gamerlogue.sharedui.generated.resources.settings__notifications_system_summary
import gamerlogue.sharedui.generated.resources.settings__notifications_unfollow
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CelebrationW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.GroupW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.NotificationsW500RoundedFill
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SettingsW500Rounded
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.LocalNotifier
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes
import it.maicol07.gamerlogue.extensions.igdb.eventStartLabel
import it.maicol07.gamerlogue.services.FollowedEvent
import it.maicol07.gamerlogue.services.Reminders
import it.maicol07.gamerlogue.services.gamesNavKey
import it.maicol07.gamerlogue.ui.components.SectionIcon
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.layout.SegmentedListLayout
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.settings.components.SettingsGroupHeader
import it.maicol07.gamerlogue.ui.views.social.ConnectionsKind
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Where the user finds every notification they're subscribed to. Which kinds to get stays in the system settings
 * (Android channels), only linked from here; the follows that drive notifications are listed: the people (their
 * own screen) and the events, which live only on this device.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun NotificationSettingsView() {
    // Injected in the body: nav3ksp turns @Branch parameters into nav key properties.
    val reminders = koinInject<Reminders>()
    val openSystemSettings = koinInject<LocalNotifier>().openSystemSettings
    val userId = koinInject<AuthTokenProvider>().session.collectAsStateWithLifecycle().value.userId
    val events by reminders.followedEvents.collectAsStateWithLifecycle()
    val backStack = LocalNavigationState.current.backStack
    val links = notificationLinks(openSystemSettings, userId) { backStack.add(it) }

    ScreenScaffold(title = Res.string.settings__notifications) {
        Column(
            Modifier
                .padding(horizontal = Dimens.ScreenPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = Dimens.ScreenPadding)
        ) {
            if (links.isNotEmpty()) {
                SegmentedListLayout {
                    links.forEachIndexed { index, link -> LinkItem(link, index, links.size) }
                }
            }
            SettingsGroupHeader(stringResource(Res.string.settings__notifications_events))
            FollowedEvents(
                events = events.values.sortedBy { it.startsAt },
                onUnfollow = { reminders.unfollow(it.id) },
                onOpen = { backStack.add(it.gamesNavKey) },
            )
        }
    }
}

private class LinkRow(
    val title: StringResource,
    val summary: StringResource,
    val icon: @Composable () -> Unit,
    /** Leaves the app (the system settings): an "open in new" glyph instead of the chevron. */
    val external: Boolean,
    val onClick: () -> Unit,
)

/** The system page of the notification kinds, where there's one, and the people followed, when signed in. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun notificationLinks(
    openSystemSettings: (() -> Unit)?,
    userId: String?,
    onNavigate: (NavKey) -> Unit,
): List<LinkRow> {
    val colors = MaterialTheme.colorScheme
    return listOfNotNull(
        openSystemSettings?.let { open ->
            LinkRow(
                title = Res.string.settings__notifications_system,
                summary = Res.string.settings__notifications_system_summary,
                icon = {
                    SectionIcon(
                        Icons.SettingsW500Rounded,
                        MaterialShapes.Cookie6Sided,
                        colors.primaryContainer,
                        colors.onPrimaryContainer,
                    )
                },
                external = true,
                onClick = open,
            )
        },
        userId?.let { id ->
            LinkRow(
                title = Res.string.settings__notifications_following,
                summary = Res.string.settings__notifications_following_summary,
                icon = {
                    SectionIcon(
                        Icons.GroupW500Rounded,
                        MaterialShapes.Clover4Leaf,
                        colors.tertiaryContainer,
                        colors.onTertiaryContainer,
                    )
                },
                external = false,
                onClick = { onNavigate(RootNavTree.UserConnections(id, ConnectionsKind.FOLLOWING)) },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkItem(link: LinkRow, index: Int, count: Int) = SettingsMenuLink(
    title = { Text(stringResource(link.title)) },
    subtitle = { Text(stringResource(link.summary)) },
    icon = link.icon,
    action = { Icon(if (link.external) Icons.OpenInNewW500Rounded else Icons.KeyboardArrowRightW500Rounded, null) },
    onClick = link.onClick,
    shapes = ListItemDefaults.expressiveSegmentedShapes(index, count),
    colors = ListItemDefaults.expressiveSegmentedColors(),
)

/** The events followed on this device, soonest first; a hint on how to follow one when there are none. */
@Composable
private fun FollowedEvents(
    events: List<FollowedEvent>,
    onUnfollow: (FollowedEvent) -> Unit,
    onOpen: (FollowedEvent) -> Unit,
) {
    if (events.isEmpty()) {
        Text(
            stringResource(Res.string.settings__notifications_events_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    SegmentedListLayout {
        events.forEachIndexed { index, event ->
            FollowedEventItem(event, index, events.size, onUnfollow = { onUnfollow(event) }, onOpen = { onOpen(event) })
        }
    }
}

/** A followed event: opens its games; the bell stops following it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FollowedEventItem(
    event: FollowedEvent,
    index: Int,
    count: Int,
    onUnfollow: () -> Unit,
    onOpen: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SettingsMenuLink(
        title = { Text(event.name) },
        subtitle = { Text(eventStartLabel(event.startsAt)) },
        icon = {
            SectionIcon(
                Icons.CelebrationW500Rounded,
                MaterialShapes.Flower,
                colors.secondaryContainer,
                colors.onSecondaryContainer,
            )
        },
        action = {
            IconButton(onClick = onUnfollow, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.NotificationsW500RoundedFill, stringResource(Res.string.settings__notifications_unfollow))
            }
        },
        onClick = onOpen,
        shapes = ListItemDefaults.expressiveSegmentedShapes(index, count),
        colors = ListItemDefaults.expressiveSegmentedColors(),
    )
}
