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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__linked_services_disclaimer
import gamerlogue.sharedui.generated.resources.settings__service_connect
import gamerlogue.sharedui.generated.resources.settings__service_disconnect
import gamerlogue.sharedui.generated.resources.settings__service_epic
import gamerlogue.sharedui.generated.resources.settings__service_gog
import gamerlogue.sharedui.generated.resources.settings__service_import_library
import gamerlogue.sharedui.generated.resources.settings__service_playstation
import gamerlogue.sharedui.generated.resources.settings__service_refresh_profile
import gamerlogue.sharedui.generated.resources.settings__service_steam
import gamerlogue.sharedui.generated.resources.settings__service_sync_done
import gamerlogue.sharedui.generated.resources.settings__service_sync_error
import gamerlogue.sharedui.generated.resources.settings__service_sync_now
import gamerlogue.sharedui.generated.resources.settings__service_sync_wishlist
import gamerlogue.sharedui.generated.resources.settings__service_web_unsupported
import gamerlogue.sharedui.generated.resources.settings__service_xbox
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
import it.maicol07.gamerlogue.ui.components.ServiceWebView
import it.maicol07.gamerlogue.ui.components.layout.SegmentedListLayout
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons as MaterialSymbols
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.Icons as SvglIcons

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinkedServicesScreen(
    navigateToImportPreview: (ExternalService, ImportMode) -> Unit,
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
    val action = uiState.action

    if (action != null) {
        val connector = viewModel.connector(action.service)
        ServiceWebView(
            initialUrl = connector.loginUrl,
            onClose = { viewModel.clearAction() },
        ) { session ->
            when (action) {
                is LinkedServicesViewModel.Action.Connect -> viewModel.runConnect(action.service, session)
                is LinkedServicesViewModel.Action.RefreshProfile -> viewModel.runRefreshProfile(action.service, session)
                is LinkedServicesViewModel.Action.SyncWishlist -> viewModel.runWishlistSync(action.service, session)
                is LinkedServicesViewModel.Action.ImportLibrary -> {
                    val refs = viewModel.runReadOwned(action.service, session)
                    ImportHandoff.put(action.service, refs)
                    navigateToImportPreview(action.service, ImportMode.OWNED)
                }

                is LinkedServicesViewModel.Action.PreviewWishlist -> {
                    val refs = viewModel.runWishlistPreview(action.service, session)
                    ImportHandoff.put(action.service, refs)
                    navigateToImportPreview(action.service, ImportMode.WISHLIST)
                }
            }
        }
        return
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

        uiState.message?.let { message ->
            val text = when (message) {
                "error" -> stringResource(Res.string.settings__service_sync_error)
                else -> stringResource(Res.string.settings__service_sync_done)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::consumeMessage,
            ) {
                Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        ExternalService.entries.forEach { service ->
            val state = uiState.services[service] ?: LinkedServicesViewModel.ServiceState()
            val uriHandler = LocalUriHandler.current
            ServiceSegmentedGroup(
                service = service,
                state = state,
                onOpenProfile = { state.profile?.profileUrl?.let { uriHandler.openURL(it) } },
                onRefreshProfile = { viewModel.refreshProfile(service) },
                onConnect = { viewModel.connect(service) },
                onDisconnect = { viewModel.disconnect(service) },
                onToggleWishlist = { viewModel.toggleWishlistSync(service, it) },
                onWishlistSyncNow = { viewModel.previewWishlist(service) },
                onImport = { viewModel.importLibrary(service) },
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
                            enabled = !state.busy,
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
                        IconButton(onClick = onWishlistSyncNow, enabled = !state.busy) {
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
                    if (state.busy) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                    } else {
                        OutlinedButton(onClick = onImport) {
                            Text(stringResource(Res.string.settings__service_import_library))
                        }
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
}

private fun ExternalService.labelRes(): StringResource = when (this) {
    ExternalService.STEAM -> Res.string.settings__service_steam
    ExternalService.PLAYSTATION -> Res.string.settings__service_playstation
    ExternalService.XBOX -> Res.string.settings__service_xbox
    ExternalService.GOG -> Res.string.settings__service_gog
    ExternalService.EPIC -> Res.string.settings__service_epic
}
