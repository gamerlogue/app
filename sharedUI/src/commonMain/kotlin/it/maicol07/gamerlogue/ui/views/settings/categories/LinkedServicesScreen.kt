package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__linked_services_disclaimer
import gamerlogue.sharedui.generated.resources.settings__service_connect
import gamerlogue.sharedui.generated.resources.settings__service_disconnect
import gamerlogue.sharedui.generated.resources.settings__service_epic
import gamerlogue.sharedui.generated.resources.settings__service_gog
import gamerlogue.sharedui.generated.resources.settings__service_import_library
import gamerlogue.sharedui.generated.resources.settings__service_nintendo
import gamerlogue.sharedui.generated.resources.settings__service_playstation
import gamerlogue.sharedui.generated.resources.settings__service_refresh_profile
import gamerlogue.sharedui.generated.resources.settings__service_steam
import gamerlogue.sharedui.generated.resources.settings__service_sync_now
import gamerlogue.sharedui.generated.resources.settings__service_sync_wishlist
import gamerlogue.sharedui.generated.resources.settings__service_web_unsupported
import gamerlogue.sharedui.generated.resources.settings__service_xbox
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RefreshW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SyncW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.EpicgamesSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.GogdotcomSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.PlaystationSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.SteamSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.icons.XboxSvgl
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveShape
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.isServiceSyncSupported
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.layout.SegmentedListLayout
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons as MaterialSymbols
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.Icons as SvglIcons

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinkedServicesScreen(
    navigateToSync: (ExternalService, ServiceSyncAction) -> Unit,
    viewModel: LinkedServicesViewModel = koinViewModel(),
) {
    if (!isServiceSyncSupported()) {
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.settings__linked_services_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(16.dp),
            )
        }

        ExternalService.entries.forEach { service ->
            val state = uiState.services[service] ?: LinkedServicesViewModel.ServiceState()
            val uriHandler = LocalUriHandler.current
            ServiceSegmentedGroup(
                service = service,
                state = state,
                onOpenProfile = { state.profile?.profileUrl?.let { uriHandler.openURL(it) } },
                onRefreshProfile = { navigateToSync(service, ServiceSyncAction.REFRESH_PROFILE) },
                onConnect = { navigateToSync(service, ServiceSyncAction.CONNECT) },
                onDisconnect = { viewModel.disconnect(service) },
                onToggleWishlist = { enabled ->
                    viewModel.toggleWishlistSync(service, enabled)
                    if (enabled) navigateToSync(service, ServiceSyncAction.SYNC_WISHLIST)
                },
                onWishlistSyncNow = { navigateToSync(service, ServiceSyncAction.PREVIEW_WISHLIST) },
                onImport = { navigateToSync(service, ServiceSyncAction.IMPORT_LIBRARY) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ServiceSegmentedGroup(
    service: ExternalService,
    state: LinkedServicesViewModel.ServiceState,
    onOpenProfile: () -> Unit,
    onRefreshProfile: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleWishlist: (Boolean) -> Unit,
    onWishlistSyncNow: () -> Unit,
    onImport: () -> Unit,
) {
    val connected = state.connected
    val profile = state.profile

    SegmentedListLayout(Modifier.fillMaxWidth()) {
        // Header: service icon (or the linked account's avatar) + name + connect/disconnect. When the
        // profile has a public page, the row opens it.
        val headerShape = ListItemDefaults.expressiveShape(first = true, last = !connected)
        val openable = profile?.profileUrl != null
        ListItem(
            modifier = Modifier
                .clip(headerShape)
                .then(if (openable) Modifier.clickable(onClick = onOpenProfile) else Modifier),
            colors = ListItemDefaults.expressiveSegmentedColors(),
            leadingContent = {
                // Platform logo stays visible regardless of the linked account.
                Image(
                    service.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            },
            trailingContent = {
                if (connected) {
                    TextButton(onClick = onDisconnect) {
                        Text(stringResource(Res.string.settings__service_disconnect))
                    }
                } else {
                    FilledTonalButton(onClick = onConnect) {
                        Text(stringResource(Res.string.settings__service_connect))
                    }
                }
            },
            headlineContent = {
                Text(stringResource(service.labelRes()), style = MaterialTheme.typography.titleMedium)
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
        )

        if (connected) {
            // Wishlist sync: auto-toggle + manual preview
            ListItem(
                modifier = Modifier.clip(ListItemDefaults.expressiveShape(first = false, last = false)),
                colors = ListItemDefaults.expressiveSegmentedColors(),
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onWishlistSyncNow) {
                            Icon(
                                MaterialSymbols.SyncW500Rounded,
                                contentDescription = stringResource(Res.string.settings__service_sync_now),
                            )
                        }
                        Switch(checked = state.wishlistSync, onCheckedChange = onToggleWishlist)
                    }
                },
                headlineContent = { Text(stringResource(Res.string.settings__service_sync_wishlist)) },
            )

            // Import library
            ListItem(
                modifier = Modifier.clip(ListItemDefaults.expressiveShape(first = false, last = true)),
                colors = ListItemDefaults.expressiveSegmentedColors(),
                trailingContent = {
                    OutlinedButton(onClick = onImport) {
                        Text(stringResource(Res.string.settings__service_import_library))
                    }
                },
                headlineContent = { Text(stringResource(Res.string.settings__service_import_library)) },
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
}

internal fun ExternalService.labelRes(): StringResource = when (this) {
    ExternalService.STEAM -> Res.string.settings__service_steam
    ExternalService.PLAYSTATION -> Res.string.settings__service_playstation
    ExternalService.XBOX -> Res.string.settings__service_xbox
    ExternalService.GOG -> Res.string.settings__service_gog
    ExternalService.EPIC -> Res.string.settings__service_epic
    ExternalService.NINTENDO -> Res.string.settings__service_nintendo
}
