package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import it.maicol07.gamerlogue.ui.components.layout.SegmentedListLayout
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Second-level list with a connected service's actions. The wishlist row is shown only for services with a
 * wishlist ([wishlistSync] non-null: its auto-sync flag). The library row syncs when the service can be
 * written to ([librarySync] non-null: the auto-sync flag), and is a plain import otherwise.
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
    modifier: Modifier = Modifier,
) = SegmentedListLayout(modifier.fillMaxWidth()) {
    val count = if (wishlistSync != null) 2 else 1
    if (wishlistSync != null) {
        SyncToggleItem(
            enabled = wishlistSync,
            onToggle = onToggleWishlist,
            onSyncNow = onWishlistSyncNow,
            icon = Icons.BookmarkW500Rounded,
            label = Res.string.settings__service_sync_wishlist,
            shapes = ListItemDefaults.segmentedShapes(0, count),
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
