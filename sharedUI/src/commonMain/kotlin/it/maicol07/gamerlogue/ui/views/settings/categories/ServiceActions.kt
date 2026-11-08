package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__service_import_library
import gamerlogue.sharedui.generated.resources.settings__service_sync_library
import gamerlogue.sharedui.generated.resources.settings__service_sync_now
import gamerlogue.sharedui.generated.resources.settings__service_sync_now_tooltip
import gamerlogue.sharedui.generated.resources.settings__service_sync_wishlist
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.BookmarkW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DownloadW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlayCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SyncW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.ui.components.layout.SegmentedListLayout
import it.maicol07.gamerlogue.ui.components.layout.rememberCollapsibleHeaderShapes
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val NestedListIndent = 16.dp

/**
 * Services as one top-level segmented list. A connected service's actions are a nested list right below it,
 * so the list closes on that service and reopens, spaced, with the next one.
 */
@Composable
internal fun ServiceRuns(
    services: List<ExternalService>,
    connected: (ExternalService) -> Boolean,
    item: @Composable (ExternalService, ListItemShapes) -> Unit,
    actions: @Composable (ExternalService, Modifier) -> Unit,
) = SegmentedListLayout(Modifier.fillMaxWidth()) {
    services.chunkedAfter(connected).forEach { run ->
        run.forEachIndexed { index, service ->
            item(service, ListItemDefaults.expressiveSegmentedShapes(index, run.size))
        }
        val service = run.last()
        if (connected(service)) actions(service, Modifier.padding(start = NestedListIndent, bottom = Dimens.ItemGap))
    }
}

/**
 * Each service as its own segmented group: [item] is the header segment and, once the service is connected,
 * toggles its [actions] right below it. [item] gets a null expanded state while the service is disconnected.
 */
@Composable
internal fun CollapsibleServices(
    services: List<ExternalService>,
    connected: (ExternalService) -> Boolean,
    item: @Composable (ExternalService, ListItemShapes, expanded: Boolean?, onToggle: () -> Unit) -> Unit,
    actions: @Composable (ExternalService) -> Unit,
) = Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
    services.forEach { service ->
        key(service) {
            var expanded by rememberSaveable { mutableStateOf(false) }
            val open = expanded && connected(service)
            SegmentedListLayout(Modifier.fillMaxWidth()) {
                val shapes = rememberCollapsibleHeaderShapes(open)
                item(service, shapes, expanded.takeIf { connected(service) }) { expanded = !expanded }
                AnimatedVisibility(open) { actions(service) }
            }
        }
    }
}

/**
 * Second-level list with a connected service's actions. The wishlist row is shown only for services with a
 * wishlist ([wishlistSync] non-null: its auto-sync flag). The library row syncs when the service can be
 * written to ([librarySync] non-null: the auto-sync flag), and is a plain import otherwise. The rows are shaped
 * as segments after [segmentOffset] leading ones (e.g. a header segment above them in the same group).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ServiceActions(
    wishlistSync: Boolean?,
    onToggleWishlist: (Boolean) -> Unit,
    onWishlistSyncNow: () -> Unit,
    librarySync: Boolean?,
    onToggleLibrarySync: (Boolean) -> Unit,
    onLibrarySyncNow: () -> Unit,
    onImport: () -> Unit,
    segmentOffset: Int,
    modifier: Modifier = Modifier,
) = SegmentedListLayout(modifier.fillMaxWidth()) {
    val count = segmentOffset + if (wishlistSync != null) 2 else 1
    if (wishlistSync != null) {
        SyncToggleItem(
            enabled = wishlistSync,
            onToggle = onToggleWishlist,
            onSyncNow = onWishlistSyncNow,
            icon = Icons.BookmarkW500Rounded,
            label = Res.string.settings__service_sync_wishlist,
            shapes = ListItemDefaults.segmentedShapes(segmentOffset, count),
        )
    }
    if (librarySync != null) {
        SyncToggleItem(
            enabled = librarySync,
            onToggle = onToggleLibrarySync,
            onSyncNow = onLibrarySyncNow,
            icon = Icons.SyncW500Rounded,
            label = Res.string.settings__service_sync_library,
            shapes = ListItemDefaults.segmentedShapes(count - 1, count),
        )
    } else {
        SegmentedListItem(
            onClick = onImport,
            shapes = ListItemDefaults.segmentedShapes(count - 1, count),
            colors = ListItemDefaults.expressiveSegmentedColors(),
            leadingContent = { Icon(Icons.DownloadW500Rounded, contentDescription = null) },
            trailingContent = { Icon(Icons.KeyboardArrowRightW500Rounded, contentDescription = null) },
        ) { Text(stringResource(Res.string.settings__service_import_library)) }
    }
}

/** A sync row: the whole row toggles auto-sync; the trailing play button runs a manual, reviewed sync. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SyncToggleItem(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    icon: ImageVector,
    label: StringResource,
    shapes: ListItemShapes,
) = SegmentedListItem(
    checked = enabled,
    onCheckedChange = onToggle,
    shapes = shapes,
    colors = ListItemDefaults.expressiveSegmentedColors(),
    leadingContent = { Icon(icon, contentDescription = null) },
    trailingContent = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TooltipBox(
                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                { PlainTooltip { Text(stringResource(Res.string.settings__service_sync_now_tooltip)) } },
                rememberTooltipState(),
            ) {
                IconButton(onClick = onSyncNow, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        Icons.PlayCircleW500Rounded,
                        contentDescription = stringResource(Res.string.settings__service_sync_now),
                    )
                }
            }
            Switch(checked = enabled, onCheckedChange = null)
        }
    },
) { Text(stringResource(label)) }
