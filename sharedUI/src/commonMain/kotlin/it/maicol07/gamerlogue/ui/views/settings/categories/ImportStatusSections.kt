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
import androidx.compose.ui.graphics.vector.ImageVector
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
 * one section per status (Backlog, Playing, …), each listing its rows in the same state order. Both kinds of
 * section collapse; [expanded] holds the ones toggled, by key: state groups start expanded, statuses collapsed.
 */
internal fun LazyListScope.importRows(
    groups: ImportGroups,
    service: ExternalService,
    expanded: Map<String, Boolean>,
    onSectionExpanded: (key: String, expanded: Boolean) -> Unit,
    onToggle: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onConfirm: (Int) -> Unit,
    onOpenStore: (String) -> Unit,
) {
    val all = buildList {
        addAll(groups.toImport)
        addAll(groups.toReview)
        addAll(groups.alreadyPresent)
    }
    val row: @Composable (IndexedValue<LibraryImportViewModel.Row>, Int, Int) -> Unit =
        { (index, row), position, count ->
            ImportRow(
                row = row,
                indexInGroup = position,
                groupCount = count,
                selectable = row.isSelectable,
                service = service,
                onToggle = { onToggle(index) },
                onEdit = { onEdit(index) },
                onConfirm = { onConfirm(index) },
                onOpenStore = onOpenStore,
            )
        }
    if (all.none { it.value.ref.status != null }) {
        groupSections(groups, expanded, onSectionExpanded, row)
    } else {
        statusSections(all, expanded, onSectionExpanded, row)
    }
}

/** The three state groups, each its own section. */
private fun LazyListScope.groupSections(
    groups: ImportGroups,
    expanded: Map<String, Boolean>,
    onSectionExpanded: (key: String, expanded: Boolean) -> Unit,
    row: @Composable (IndexedValue<LibraryImportViewModel.Row>, position: Int, count: Int) -> Unit,
) {
    listOf(
        groups.matchedTitle to groups.toImport,
        Res.string.settings__import_group_to_review to groups.toReview,
        Res.string.settings__import_group_already_present to groups.alreadyPresent,
    ).forEach { (title, rows) ->
        val key = "group-${title.key}"
        val open = expanded[key] ?: true
        collapsibleSection(
            key = key,
            rows = rows,
            expanded = open,
            header = { shapes, modifier ->
                SectionHeader(
                    title = stringResource(title, rows.size),
                    icon = null,
                    supporting = null,
                    expanded = open,
                    shapes = shapes,
                    onToggle = { onSectionExpanded(key, !open) },
                    modifier = modifier,
                )
            },
            rowKey = { "row-${it.index}" },
            row = row,
        )
    }
}

/** One section per status, listing its rows in the state order of [all]. */
private fun LazyListScope.statusSections(
    all: List<IndexedValue<LibraryImportViewModel.Row>>,
    expanded: Map<String, Boolean>,
    onSectionExpanded: (key: String, expanded: Boolean) -> Unit,
    row: @Composable (IndexedValue<LibraryImportViewModel.Row>, position: Int, count: Int) -> Unit,
) {
    // Same fallback as the import: a tracker game with no status of ours lands in the backlog.
    val byStatus = all.groupBy { it.value.ref.status ?: GameLibraryStatus.BACKLOG }
    GameLibraryStatus.entries.forEach { status ->
        val rows = byStatus[status] ?: return@forEach
        val open = expanded[status.name] == true
        val selected = rows.count { it.value.included }
        val plural = Res.plurals.settings__import_selected
        collapsibleSection(
            key = "status-${status.name}",
            rows = rows,
            expanded = open,
            header = { shapes, modifier ->
                SectionHeader(
                    title = stringResource(status.displayName),
                    icon = status.icon,
                    supporting = pluralStringResource(plural, selected, selected, rows.size),
                    expanded = open,
                    shapes = shapes,
                    onToggle = { onSectionExpanded(status.name, !open) },
                    modifier = modifier,
                )
            },
            rowKey = { "row-${it.index}" },
            row = row,
        )
    }
}

/**
 * One collapsible section: a header segment that expands/collapses it, with its rows segmented below it; no-op
 * when there are no rows. [header] draws the header with the shapes and item modifier it is given; [row] draws a
 * row at its segment position (the header is position 0) out of the section's segment count.
 */
internal fun <T> LazyListScope.collapsibleSection(
    key: String,
    rows: List<T>,
    rowKey: (T) -> Any,
    expanded: Boolean,
    header: @Composable (shapes: ListItemShapes, modifier: Modifier) -> Unit,
    row: @Composable (item: T, position: Int, count: Int) -> Unit,
) {
    if (rows.isEmpty()) return
    val count = if (expanded) rows.size + 1 else 1
    item(key = key) {
        header(ListItemDefaults.segmentedShapes(index = 0, count = count), Modifier.animateItem().padding(top = 8.dp))
    }
    if (!expanded) return
    itemsIndexed(rows, key = { _, item -> rowKey(item) }) { i, item ->
        Box(
            Modifier.animateItem(
                fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                fadeOutSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            ),
        ) { row(item, i + 1, count) }
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    icon: ImageVector?,
    supporting: String?,
    expanded: Boolean,
    shapes: ListItemShapes,
    onToggle: () -> Unit,
    modifier: Modifier,
) {
    SegmentedListItem(
        onClick = onToggle,
        shapes = shapes,
        colors = ListItemDefaults.expressiveSegmentedColors(),
        modifier = modifier,
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = { ExpandArrow(expanded, Modifier) },
    ) { Text(title, style = MaterialTheme.typography.titleMedium) }
}

/** A section's arrow: down when collapsed, turning up as it expands. */
@Composable
internal fun ExpandArrow(expanded: Boolean, modifier: Modifier) {
    val arrowTurn by animateFloatAsState(
        if (expanded) HALF_TURN_DEGREES else 0f,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
    )
    Icon(
        Icons.KeyboardArrowDownW500Rounded,
        contentDescription = null,
        modifier = modifier.graphicsLayer { rotationZ = arrowTurn },
    )
}

/** Only confident matches of games not already in the library can be selected. */
private val LibraryImportViewModel.Row.isSelectable: Boolean get() = confident && !alreadyPresent

