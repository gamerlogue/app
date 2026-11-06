package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__import_group_already_present
import gamerlogue.sharedui.generated.resources.settings__import_group_to_review
import gamerlogue.sharedui.generated.resources.settings__import_selected
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowDownW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.gamerlogue.ui.views.settings.components.SettingsGroupHeader
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val HALF_TURN_DEGREES = 180f

/** The preview rows split by state: importable (confident, new), to review (unmatched/low-confidence) and
 *  already present. Indices are the original row indices. */
internal data class ImportGroups(
    val toImport: List<IndexedValue<LibraryImportViewModel.Row>>,
    val toReview: List<IndexedValue<LibraryImportViewModel.Row>>,
    val alreadyPresent: List<IndexedValue<LibraryImportViewModel.Row>>,
    val matchedTitle: StringResource,
)

/**
 * The preview list. Store games come as the three state groups; tracker games carry a status, so they come as
 * one collapsible section per status (Backlog, Playing, …), each listing its rows in the same state order.
 */
internal fun LazyListScope.importRows(
    groups: ImportGroups,
    service: ExternalService,
    expanded: Map<GameLibraryStatus, Boolean>,
    onToggleStatus: (GameLibraryStatus) -> Unit,
    onToggle: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onOpenStore: (String) -> Unit,
) {
    val all = buildList {
        addAll(groups.toImport)
        addAll(groups.toReview)
        addAll(groups.alreadyPresent)
    }
    if (all.none { it.value.ref.status != null }) {
        listOf(
            groups.matchedTitle to groups.toImport,
            Res.string.settings__import_group_to_review to groups.toReview,
            Res.string.settings__import_group_already_present to groups.alreadyPresent,
        ).forEach { (title, rows) -> importGroup(title, rows, service, onToggle, onEdit, onOpenStore) }
        return
    }
    // Same fallback as the import: a tracker game with no status of ours lands in the backlog.
    val byStatus = all.groupBy { it.value.ref.status ?: GameLibraryStatus.BACKLOG }
    GameLibraryStatus.entries.forEach { status ->
        val rows = byStatus[status] ?: return@forEach
        statusSection(
            status = status,
            rows = rows,
            expanded = expanded[status] == true,
            onToggleSection = { onToggleStatus(status) },
            service = service,
            onToggle = onToggle,
            onEdit = onEdit,
            onOpenStore = onOpenStore,
        )
    }
}

/** A titled segmented list for one state group; no-op when empty. */
private fun LazyListScope.importGroup(
    titleRes: StringResource,
    rows: List<IndexedValue<LibraryImportViewModel.Row>>,
    service: ExternalService,
    onToggle: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onOpenStore: (String) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "group-${titleRes.key}") { SettingsGroupHeader(stringResource(titleRes, rows.size)) }
    itemsIndexed(rows, key = { _, indexed -> "row-${indexed.index}" }) { indexInGroup, (index, row) ->
        Box(
            Modifier.animateItem(
                fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                fadeOutSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            ),
        ) {
            ImportRow(
                row = row,
                indexInGroup = indexInGroup,
                groupCount = rows.size,
                selectable = row.isSelectable,
                service = service,
                onToggle = { onToggle(index) },
                onEdit = { onEdit(index) },
                onOpenStore = onOpenStore,
            )
        }
    }
}

/** One status: a header that expands/collapses the section, with its rows segmented below it. */
private fun LazyListScope.statusSection(
    status: GameLibraryStatus,
    rows: List<IndexedValue<LibraryImportViewModel.Row>>,
    expanded: Boolean,
    onToggleSection: () -> Unit,
    service: ExternalService,
    onToggle: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onOpenStore: (String) -> Unit,
) {
    val count = if (expanded) rows.size + 1 else 1
    item(key = "status-${status.name}") {
        StatusHeader(
            status = status,
            selected = rows.count { it.value.included },
            total = rows.size,
            expanded = expanded,
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = count),
            onToggle = onToggleSection,
            modifier = Modifier.animateItem().padding(top = 8.dp),
        )
    }
    if (!expanded) return
    itemsIndexed(rows, key = { _, indexed -> "row-${indexed.index}" }) { i, (index, row) ->
        Box(
            Modifier.animateItem(
                fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                fadeOutSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            ),
        ) {
            ImportRow(
                row = row,
                indexInGroup = i + 1,
                groupCount = count,
                selectable = row.isSelectable,
                service = service,
                onToggle = { onToggle(index) },
                onEdit = { onEdit(index) },
                onOpenStore = onOpenStore,
            )
        }
    }
}

@Composable
private fun StatusHeader(
    status: GameLibraryStatus,
    selected: Int,
    total: Int,
    expanded: Boolean,
    shapes: ListItemShapes,
    onToggle: () -> Unit,
    modifier: Modifier,
) {
    val arrowTurn by animateFloatAsState(
        if (expanded) HALF_TURN_DEGREES else 0f,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
    )
    SegmentedListItem(
        onClick = onToggle,
        shapes = shapes,
        colors = ListItemDefaults.expressiveSegmentedColors(),
        modifier = modifier,
        leadingContent = { Icon(status.icon, contentDescription = null) },
        supportingContent = {
            Text(pluralStringResource(Res.plurals.settings__import_selected, selected, selected, total))
        },
        trailingContent = {
            Icon(
                Icons.KeyboardArrowDownW500Rounded,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = arrowTurn },
            )
        },
    ) { Text(stringResource(status.displayName), style = MaterialTheme.typography.titleMedium) }
}

/** Only confident matches of games not already in the library can be selected. */
private val LibraryImportViewModel.Row.isSelectable: Boolean get() = confident && !alreadyPresent

