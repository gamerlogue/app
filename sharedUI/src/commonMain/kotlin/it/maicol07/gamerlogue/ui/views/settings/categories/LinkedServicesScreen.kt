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
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__linked_services_disclaimer
import gamerlogue.sharedui.generated.resources.settings__linked_services
import gamerlogue.sharedui.generated.resources.settings__service_connect
import gamerlogue.sharedui.generated.resources.settings__service_disconnect
import gamerlogue.sharedui.generated.resources.settings__service_epic
import gamerlogue.sharedui.generated.resources.settings__service_gog
import gamerlogue.sharedui.generated.resources.settings__service_import_library
import gamerlogue.sharedui.generated.resources.settings__service_nintendo
import gamerlogue.sharedui.generated.resources.settings__service_pc_only
import gamerlogue.sharedui.generated.resources.settings__service_playstation
import gamerlogue.sharedui.generated.resources.settings__service_refresh_profile
import gamerlogue.sharedui.generated.resources.settings__service_steam
import gamerlogue.sharedui.generated.resources.settings__service_sync_now
import gamerlogue.sharedui.generated.resources.settings__service_sync_wishlist
import gamerlogue.sharedui.generated.resources.settings__service_ubisoft
import gamerlogue.sharedui.generated.resources.settings__service_web_unsupported
import gamerlogue.sharedui.generated.resources.settings__service_xbox
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.BookmarkW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DownloadW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.InfoW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RefreshW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SyncW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.EpicgamesSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.GogdotcomSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.PlaystationSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.SteamSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.UbisoftSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.icons.XboxSvgl
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.isServiceSyncSupported
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.SectionIcon
import it.maicol07.gamerlogue.ui.components.layout.SegmentedListLayout
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons as MaterialSymbols
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.Icons as SvglIcons

private val NestedListIndent = 16.dp

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

        // Services form the top-level segmented list. A connected service's actions are a nested list right
        // below it, so the top-level list closes on that service and reopens, spaced, with the next one.
        val stateOf = { service: ExternalService -> uiState.services[service] ?: LinkedServicesViewModel.ServiceState() }
        val uriHandler = LocalUriHandler.current
        SegmentedListLayout(Modifier.fillMaxWidth()) {
            ExternalService.entries.chunkedAfter { stateOf(it).connected }.forEach { run ->
                run.forEachIndexed { index, service ->
                    val state = stateOf(service)
                    ServiceItem(
                        service = service,
                        state = state,
                        shapes = ListItemDefaults.expressiveSegmentedShapes(index, run.size),
                        onOpenProfile = { state.profile?.profileUrl?.let { uriHandler.openURL(it) } },
                        onRefreshProfile = { navigateToSync(service, ServiceSyncAction.REFRESH_PROFILE) },
                        onConnect = { navigateToSync(service, ServiceSyncAction.CONNECT) },
                        onDisconnect = { viewModel.disconnect(service) },
                    )
                }
                val service = run.last()
                val state = stateOf(service)
                if (state.connected) {
                    ServiceActions(
                        wishlistSync = state.wishlistSync,
                        onToggleWishlist = { enabled ->
                            viewModel.toggleWishlistSync(service, enabled)
                            if (enabled) navigateToSync(service, ServiceSyncAction.SYNC_WISHLIST)
                        },
                        onWishlistSyncNow = { navigateToSync(service, ServiceSyncAction.PREVIEW_WISHLIST) },
                        onImport = { navigateToSync(service, ServiceSyncAction.IMPORT_LIBRARY) },
                        modifier = Modifier.padding(start = NestedListIndent, bottom = Dimens.ItemGap),
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

/** Top-level row: service logo + name + connect/disconnect, with the linked account below when connected. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ServiceItem(
    service: ExternalService,
    state: LinkedServicesViewModel.ServiceState,
    shapes: ListItemShapes,
    onOpenProfile: () -> Unit,
    onRefreshProfile: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
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
            )
        },
        trailingContent = {
            if (connected) {
                TextButton(onClick = onDisconnect, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(Res.string.settings__service_disconnect))
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

/** Second-level list with a connected service's actions. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ServiceActions(
    wishlistSync: Boolean,
    onToggleWishlist: (Boolean) -> Unit,
    onWishlistSyncNow: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) = SegmentedListLayout(modifier.fillMaxWidth()) {
    // Wishlist sync: the whole row toggles auto-sync; the trailing button runs a manual preview.
    SegmentedListItem(
        checked = wishlistSync,
        onCheckedChange = onToggleWishlist,
        shapes = ListItemDefaults.segmentedShapes(0, 2),
        colors = ListItemDefaults.expressiveSegmentedColors(),
        leadingContent = { Icon(MaterialSymbols.BookmarkW500Rounded, contentDescription = null) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onWishlistSyncNow) {
                    Icon(
                        MaterialSymbols.SyncW500Rounded,
                        contentDescription = stringResource(Res.string.settings__service_sync_now),
                    )
                }
                Switch(checked = wishlistSync, onCheckedChange = null)
            }
        },
    ) { Text(stringResource(Res.string.settings__service_sync_wishlist)) }

    // Import library
    SegmentedListItem(
        onClick = onImport,
        shapes = ListItemDefaults.segmentedShapes(1, 2),
        colors = ListItemDefaults.expressiveSegmentedColors(),
        leadingContent = { Icon(MaterialSymbols.DownloadW500Rounded, contentDescription = null) },
        trailingContent = { Icon(MaterialSymbols.KeyboardArrowRightW500Rounded, contentDescription = null) },
    ) { Text(stringResource(Res.string.settings__service_import_library)) }
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
}

internal fun ExternalService.labelRes(): StringResource = when (this) {
    ExternalService.STEAM -> Res.string.settings__service_steam
    ExternalService.PLAYSTATION -> Res.string.settings__service_playstation
    ExternalService.XBOX -> Res.string.settings__service_xbox
    ExternalService.GOG -> Res.string.settings__service_gog
    ExternalService.EPIC -> Res.string.settings__service_epic
    ExternalService.NINTENDO -> Res.string.settings__service_nintendo
    ExternalService.UBISOFT -> Res.string.settings__service_ubisoft
}

// Ubisoft's console releases are already covered by their own platform connectors, so this
// connector only ever reflects the PC (Ubisoft Connect launcher) copy of a game.
private fun ExternalService.platformNoteRes(): StringResource? = when (this) {
    ExternalService.UBISOFT -> Res.string.settings__service_pc_only
    else -> null
}
