package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__import_group_already_present
import gamerlogue.sharedui.generated.resources.settings__import_group_to_review
import gamerlogue.sharedui.generated.resources.settings__import_selected
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.ui.components.layout.CollapsibleSectionHeader
import it.maicol07.gamerlogue.ui.components.layout.collapsibleSection
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

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
                CollapsibleSectionHeader(
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
                CollapsibleSectionHeader(
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

/** Only confident matches of games not already in the library can be selected. */
private val LibraryImportViewModel.Row.isSelectable: Boolean get() = confident && !alreadyPresent

