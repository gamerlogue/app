package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common__more_options
import gamerlogue.sharedui.generated.resources.settings__linked_services
import gamerlogue.sharedui.generated.resources.settings__linked_services_disclaimer
import gamerlogue.sharedui.generated.resources.settings__service_backloggd
import gamerlogue.sharedui.generated.resources.settings__service_backloggery
import gamerlogue.sharedui.generated.resources.settings__service_category_console
import gamerlogue.sharedui.generated.resources.settings__service_category_pc
import gamerlogue.sharedui.generated.resources.settings__service_category_tracker
import gamerlogue.sharedui.generated.resources.settings__service_connect
import gamerlogue.sharedui.generated.resources.settings__service_disconnect
import gamerlogue.sharedui.generated.resources.settings__service_epic
import gamerlogue.sharedui.generated.resources.settings__service_gog
import gamerlogue.sharedui.generated.resources.settings__service_grouvee
import gamerlogue.sharedui.generated.resources.settings__service_howlongtobeat
import gamerlogue.sharedui.generated.resources.settings__service_infinite_backlog
import gamerlogue.sharedui.generated.resources.settings__service_nintendo
import gamerlogue.sharedui.generated.resources.settings__service_pc_only
import gamerlogue.sharedui.generated.resources.settings__service_playstation
import gamerlogue.sharedui.generated.resources.settings__service_refresh_profile
import gamerlogue.sharedui.generated.resources.settings__service_resync_ids
import gamerlogue.sharedui.generated.resources.settings__service_stash
import gamerlogue.sharedui.generated.resources.settings__service_steam
import gamerlogue.sharedui.generated.resources.settings__service_ubisoft
import gamerlogue.sharedui.generated.resources.settings__service_web_unsupported
import gamerlogue.sharedui.generated.resources.settings__service_xbox
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.EditNoteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FormatListBulletedW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.InfoW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.MoreVertW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RefreshW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StyleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.TimerW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ToysAndGamesW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.TrophyW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.EpicgamesSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.GogdotcomSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.PlaystationSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.SteamSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.UbisoftSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.icons.XboxSvgl
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceCategory
import it.maicol07.gamerlogue.services.isServiceSyncSupported
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.SectionIcon
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.views.settings.components.SettingsGroupHeader
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons as MaterialSymbols
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.Icons as SvglIcons

@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun LinkedServicesView(
    viewModel: LinkedServicesViewModel = koinViewModel(),
) {
    val navigationState = LocalNavigationState.current
    ScreenScaffold(title = Res.string.settings__linked_services) {
        LinkedServicesContent(
            viewModel = viewModel,
            navigateToSync = { service, action ->
                navigationState.backStack.add(RootNavTree.ServiceSync(service, action))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkedServicesContent(
    viewModel: LinkedServicesViewModel,
    navigateToSync: (ExternalService, ServiceSyncAction) -> Unit,
) {
    if (!isServiceSyncSupported) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.settings__service_web_unsupported),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // A sync flow runs on its own screen (its own ViewModel instance) and persists to settings; re-read
    // when we return so the list reflects any connect/disconnect/sync that happened while away.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshAll()
        onPauseOrDispose {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(MaterialSymbols.InfoW500Rounded, contentDescription = null)
                Text(
                    text = stringResource(Res.string.settings__linked_services_disclaimer),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        val stateOf = { service: ExternalService -> uiState.services[service] ?: LinkedServicesViewModel.ServiceState() }
        val uriHandler = LocalUriHandler.current
        // A service with no sign-in asks for the user's public handle before its connect flow.
        var askHandleFor by remember { mutableStateOf<ExternalService?>(null) }
        askHandleFor?.let { service ->
            HandleDialog(
                onConfirm = { handle ->
                    askHandleFor = null
                    viewModel.setHandle(service, handle)
                    navigateToSync(service, ServiceSyncAction.CONNECT)
                },
                onDismiss = { askHandleFor = null },
            )
        }
        val serviceItem: @Composable (ExternalService, ListItemShapes, Boolean?, () -> Unit) -> Unit =
            { service, shapes, expanded, onToggleExpanded ->
                val state = stateOf(service)
                ServiceItem(
                    service = service,
                    state = state,
                    shapes = shapes,
                    expanded = expanded,
                    onToggleExpanded = onToggleExpanded,
                    onOpenProfile = { state.profile?.profileUrl?.let { uriHandler.openURL(it) } },
                    onRefreshProfile = { navigateToSync(service, ServiceSyncAction.REFRESH_PROFILE) },
                    onConnect = {
                        if (viewModel.connector(service).needsHandle) {
                            askHandleFor = service
                        } else {
                            navigateToSync(service, ServiceSyncAction.CONNECT)
                        }
                    },
                    onDisconnect = { viewModel.disconnect(service) },
                    onResyncIds = if (state.connected && service == ExternalService.BACKLOGGERY) {
                        { navigateToSync(service, ServiceSyncAction.RESYNC_IDS) }
                    } else null,
                )
            }
        val serviceActions: @Composable (ExternalService, Int, Modifier) -> Unit = { service, segmentOffset, modifier ->
            val state = stateOf(service)
            ServiceActions(
                wishlistSync = state.wishlistSync.takeIf { viewModel.connector(service).wishlist != null },
                onToggleWishlist = { enabled ->
                    viewModel.toggleWishlistSync(service, enabled)
                    if (enabled) navigateToSync(service, ServiceSyncAction.SYNC_WISHLIST)
                },
                onWishlistSyncNow = { navigateToSync(service, ServiceSyncAction.PREVIEW_WISHLIST) },
                librarySync = state.librarySync.takeIf { viewModel.connector(service).statusWrite != null },
                onToggleLibrarySync = { enabled ->
                    viewModel.toggleLibrarySync(service, enabled)
                    // The first sync is always reviewed: nothing is written unattended before it.
                    if (enabled) {
                        navigateToSync(
                            service,
                            if (viewModel.hasSyncedLibrary(service)) {
                                ServiceSyncAction.SYNC_LIBRARY
                            } else {
                                ServiceSyncAction.PREVIEW_LIBRARY_SYNC
                            },
                        )
                    }
                },
                onLibrarySyncNow = { navigateToSync(service, ServiceSyncAction.PREVIEW_LIBRARY_SYNC) },
                onImport = { navigateToSync(service, ServiceSyncAction.IMPORT_LIBRARY) },
                segmentOffset = segmentOffset,
                modifier = modifier,
            )
        }
        ExternalService.entries.groupBy { it.category }.forEach { (category, services) ->
            Column {
                SettingsGroupHeader(stringResource(category.labelRes()))
                val connected = { service: ExternalService -> stateOf(service).connected }
                if (category == ServiceCategory.TRACKER) {
                    // Like the sync preview's sections: each tracker is its own group, and a connected one
                    // collapses its actions, drawn as the segments right below it.
                    CollapsibleServices(
                        services = services,
                        connected = connected,
                        item = serviceItem,
                        actions = { serviceActions(it, 1, Modifier) },
                    )
                } else {
                    ServiceRuns(
                        services = services,
                        connected = connected,
                        item = { service, shapes -> serviceItem(service, shapes, null) {} },
                        actions = { service, modifier -> serviceActions(service, 0, modifier) },
                    )
                }
            }
        }
    }
}

/** Splits into consecutive chunks, each ending at an element matching [predicate] or at the end of the list. */
internal fun <T> List<T>.chunkedAfter(predicate: (T) -> Boolean): List<List<T>> =
    fold(mutableListOf(mutableListOf<T>())) { chunks, item ->
        chunks.last().add(item)
        if (predicate(item)) chunks.add(mutableListOf())
        chunks
    }.filter { it.isNotEmpty() }

/**
 * Top-level row: service logo + name + connect/disconnect, with the linked account below when connected.
 * A non-null [expanded] adds an arrow toggling the service's collapsible actions.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ServiceItem(
    service: ExternalService,
    state: LinkedServicesViewModel.ServiceState,
    shapes: ListItemShapes,
    expanded: Boolean?,
    onToggleExpanded: () -> Unit,
    onOpenProfile: () -> Unit,
    onRefreshProfile: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onResyncIds: (() -> Unit)?,
) {
    val connected = state.connected
    val profile = state.profile

    // Tapping connects the service, or opens the linked account's public page once connected (no-op when
    // the store exposes none).
    SegmentedListItem(
        onClick = if (connected) onOpenProfile else onConnect,
        shapes = shapes,
        colors = ListItemDefaults.expressiveSegmentedColors(),
        leadingContent = {
            // Platform logo stays visible regardless of the linked account; the container turns
            // primary once the service is connected.
            SectionIcon(
                service.icon(),
                MaterialShapes.Cookie4Sided,
                if (connected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                if (connected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                service.logoUrl(),
            )
        },
        trailingContent = {
            if (connected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDisconnect, shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(Res.string.settings__service_disconnect))
                    }
                    onResyncIds?.let { ServiceMoreMenu(it) }
                    expanded?.let {
                        IconButton(onClick = onToggleExpanded, shapes = IconButtonDefaults.shapes()) {
                            ExpandArrow(it, Modifier)
                        }
                    }
                }
            } else {
                FilledTonalButton(onClick = onConnect, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(Res.string.settings__service_connect))
                }
            }
        },
        // Account row (small avatar + username + refresh) below the platform name when connected.
        supportingContent = if (connected) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    profile?.avatarUrl?.let { avatar ->
                        RemoteImage(
                            url = avatar,
                            contentDescription = profile.username,
                            modifier = Modifier.size(20.dp).clip(CircleShape),
                            loadingModifier = Modifier.size(20.dp).clip(CircleShape),
                        )
                    }
                    profile?.username?.let { username ->
                        Text(username, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(
                        onClick = onRefreshProfile,
                        modifier = Modifier.size(28.dp),
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            MaterialSymbols.RefreshW500Rounded,
                            contentDescription = stringResource(Res.string.settings__service_refresh_profile),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        } else {
            null
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(service.labelRes()), style = MaterialTheme.typography.titleMediumEmphasized)
            service.platformNoteRes()?.let { note ->
                Text(
                    stringResource(note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ServiceMoreMenu(onResyncIds: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, shapes = IconButtonDefaults.shapes()) {
            Icon(MaterialSymbols.MoreVertW500Rounded, contentDescription = stringResource(Res.string.common__more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.settings__service_resync_ids)) },
                onClick = { expanded = false; onResyncIds() },
            )
        }
    }
}

private fun ExternalService.icon(): ImageVector = when (this) {
    ExternalService.STEAM -> Icons.SteamSimpleIcons
    ExternalService.PLAYSTATION -> Icons.PlaystationSimpleIcons
    ExternalService.XBOX -> SvglIcons.XboxSvgl
    ExternalService.GOG -> Icons.GogdotcomSimpleIcons
    ExternalService.EPIC -> Icons.EpicgamesSimpleIcons
    // Nintendo's logo was pulled from simple-icons/svgl on legal request; use a neutral gaming glyph.
    ExternalService.NINTENDO -> MaterialSymbols.JoystickW500Rounded
    ExternalService.UBISOFT -> Icons.UbisoftSimpleIcons
    // No tracker has a brand icon on simple-icons/svgl; each gets a distinct neutral glyph instead.
    ExternalService.BACKLOGGERY -> MaterialSymbols.FormatListBulletedW500Rounded
    ExternalService.BACKLOGGD -> MaterialSymbols.StyleW500Rounded
    ExternalService.HOWLONGTOBEAT -> MaterialSymbols.TimerW500Rounded
    ExternalService.GROUVEE -> MaterialSymbols.TrophyW500Rounded
    ExternalService.INFINITE_BACKLOG -> MaterialSymbols.EditNoteW500Rounded
    ExternalService.STASH -> MaterialSymbols.ToysAndGamesW500Rounded
}

// No tracker has a brand icon on simple-icons/svgl, and bundling their artwork would redistribute it: their
// own icon is loaded at runtime instead, with the glyph above as the fallback. Grouvee's comes from its CDN
// (content-hashed: a redeploy breaks the URL and falls back to the glyph); Stash's site has none, so it uses
// the Play Store icon of its app (io.stashteam.games.tracker.stashapp). HowLongToBeat uses Google's cached
// favicon because its own image endpoint returns 403 to non-browser requests.
private fun ExternalService.logoUrl(): String? = when (this) {
    ExternalService.BACKLOGGD -> "https://backloggd.com/apple-touch-icon.png"
    ExternalService.BACKLOGGERY -> "https://backloggery.com/img/icons/favicon-32x32.png"
    ExternalService.HOWLONGTOBEAT -> "https://www.google.com/s2/favicons?domain=howlongtobeat.com&sz=64"
    ExternalService.INFINITE_BACKLOG -> "https://images.infinitebacklog.net/images/icons/apple-touch-icon-152x152.png"
    // Pulled from simple-icons/svgl on Nintendo's request, so it is loaded from nintendo.com, never bundled.
    ExternalService.NINTENDO -> "https://www.nintendo.com/eu/app/media/images/01_website_elements/icons/Nintendo_Website_icon_114x114.png"
    ExternalService.GROUVEE -> "https://files-grouvee-com.b-cdn.net/img/apple-touch-icon.ef9bfb53f6a7.png"
    ExternalService.STASH ->
        "https://play-lh.googleusercontent.com/_tk5SamI5kPyLodXp0hLMSMBK3AFeoImF0ZtIzdsS-xYVRHWnTnOw9wCk9sedN3QXi9jL898L3bU62d61P2_gg=s96"

    else -> null
}

internal fun ExternalService.labelRes(): StringResource = when (this) {
    ExternalService.STEAM -> Res.string.settings__service_steam
    ExternalService.PLAYSTATION -> Res.string.settings__service_playstation
    ExternalService.XBOX -> Res.string.settings__service_xbox
    ExternalService.GOG -> Res.string.settings__service_gog
    ExternalService.EPIC -> Res.string.settings__service_epic
    ExternalService.NINTENDO -> Res.string.settings__service_nintendo
    ExternalService.UBISOFT -> Res.string.settings__service_ubisoft
    ExternalService.BACKLOGGERY -> Res.string.settings__service_backloggery
    ExternalService.BACKLOGGD -> Res.string.settings__service_backloggd
    ExternalService.HOWLONGTOBEAT -> Res.string.settings__service_howlongtobeat
    ExternalService.GROUVEE -> Res.string.settings__service_grouvee
    ExternalService.INFINITE_BACKLOG -> Res.string.settings__service_infinite_backlog
    ExternalService.STASH -> Res.string.settings__service_stash
}

private fun ServiceCategory.labelRes(): StringResource = when (this) {
    ServiceCategory.CONSOLE -> Res.string.settings__service_category_console
    ServiceCategory.PC -> Res.string.settings__service_category_pc
    ServiceCategory.TRACKER -> Res.string.settings__service_category_tracker
}

// Ubisoft's console releases are already covered by their own platform connectors, so this
// connector only ever reflects the PC (Ubisoft Connect launcher) copy of a game.
private fun ExternalService.platformNoteRes(): StringResource? = when (this) {
    ExternalService.UBISOFT -> Res.string.settings__service_pc_only
    else -> null
}
