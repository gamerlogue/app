package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.app_name
import gamerlogue.sharedui.generated.resources.common_close
import gamerlogue.sharedui.generated.resources.settings__library_sync_apply
import gamerlogue.sharedui.generated.resources.settings__library_sync_group_conflicts
import gamerlogue.sharedui.generated.resources.settings__library_sync_group_from
import gamerlogue.sharedui.generated.resources.settings__library_sync_group_to
import gamerlogue.sharedui.generated.resources.settings__library_sync_hint
import gamerlogue.sharedui.generated.resources.settings__library_sync_missing
import gamerlogue.sharedui.generated.resources.settings__library_sync_nothing
import gamerlogue.sharedui.generated.resources.settings__library_sync_status_change
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckCircleW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.SyncSide
import it.maicol07.gamerlogue.ui.components.HintCard
import it.maicol07.gamerlogue.ui.components.StatusMessage
import it.maicol07.gamerlogue.ui.components.layout.CollapsibleSectionHeader
import it.maicol07.gamerlogue.ui.components.layout.collapsibleSection
import org.jetbrains.compose.resources.stringResource

/**
 * [ImportMode.SYNC] step 2: the sync plan in three groups — tracker → Gamerlogue, Gamerlogue → tracker and
 * conflicts, where the user picks which status to keep. Any other row can be tapped to leave it out of the sync.
 * The first two groups collapse; conflicts always stay open.
 * Indices passed to [onChoose] and [onToggle] are plan indices.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SyncPlanContent(
    service: ExternalService,
    rows: List<LibraryImportViewModel.PlanRow>,
    applying: Boolean,
    onChoose: (Int, SyncSide) -> Unit,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit,
    onDone: () -> Unit,
) {
    if (rows.isEmpty()) {
        StatusMessage(
            Icons.CheckCircleW500Rounded,
            stringResource(Res.string.settings__library_sync_nothing),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialShapes.Sunny,
        ) {
            Button(onClick = onDone, shapes = ButtonDefaults.shapes()) { Text(stringResource(Res.string.common_close)) }
        }
        return
    }
    val tracker = stringResource(service.labelRes())
    val indexed = rows.withIndex().toList()
    // The groups toggled, by title key: they start expanded.
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item(key = "hint") {
                HintCard(stringResource(Res.string.settings__library_sync_hint), Modifier.padding(bottom = 8.dp))
            }
            planGroups(indexed, tracker, expanded, onChoose, onToggle)
        }
        Button(
            onClick = onApply,
            enabled = !applying,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            if (applying) CircularProgressIndicator(Modifier.padding(end = 8.dp).size(20.dp))
            Text(stringResource(Res.string.settings__library_sync_apply))
        }
    }
}

/** The three plan groups: the two collapsing on their header's tap ([expanded] holds the toggled ones), then the
 *  conflicts, which never collapse since each one must be settled before applying. */
private fun LazyListScope.planGroups(
    indexed: List<IndexedValue<LibraryImportViewModel.PlanRow>>,
    tracker: String,
    expanded: MutableMap<String, Boolean>,
    onChoose: (Int, SyncSide) -> Unit,
    onToggle: (Int) -> Unit,
) {
    val (conflicts, settled) = indexed.partition { it.value.conflict }
    val (pulled, pushed) = settled.partition { it.value.choice == SyncSide.REMOTE }
    listOf(
        Res.string.settings__library_sync_group_from to pulled,
        Res.string.settings__library_sync_group_to to pushed,
    ).forEach { (title, group) ->
        val open = expanded[title.key] ?: true
        planGroup(title.key, group, open, tracker, onChoose, onToggle) { shapes, modifier ->
            CollapsibleSectionHeader(
                title = stringResource(title, tracker, group.size),
                icon = null,
                supporting = null,
                expanded = open,
                shapes = shapes,
                onToggle = { expanded[title.key] = !open },
                modifier = modifier,
            )
        }
    }
    val conflictsTitle = Res.string.settings__library_sync_group_conflicts
    planGroup(conflictsTitle.key, conflicts, true, tracker, onChoose, onToggle) { shapes, modifier ->
        val title = stringResource(conflictsTitle, tracker, conflicts.size)
        SegmentedListItem(shapes = shapes, colors = ListItemDefaults.expressiveSegmentedColors(), modifier = modifier) {
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** A plan group as a [collapsibleSection] under [header], keyed by its title's [key]. */
private fun LazyListScope.planGroup(
    key: String,
    rows: List<IndexedValue<LibraryImportViewModel.PlanRow>>,
    expanded: Boolean,
    tracker: String,
    onChoose: (Int, SyncSide) -> Unit,
    onToggle: (Int) -> Unit,
    header: @Composable (shapes: ListItemShapes, modifier: Modifier) -> Unit,
) = collapsibleSection(
    key = "group-$key",
    rows = rows,
    rowKey = { it.value.change.gameId },
    expanded = expanded,
    header = header,
) { (index, row), position, count ->
    PlanItem(row, tracker, position, count, onToggle = { onToggle(index) }) { onChoose(index, it) }
}

/** A plan row: the game, its status on each side and, for a conflict, the side to keep. Other rows toggle on tap. */
@Composable
private fun PlanItem(
    row: LibraryImportViewModel.PlanRow,
    tracker: String,
    indexInGroup: Int,
    groupCount: Int,
    onToggle: () -> Unit,
    onChoose: (SyncSide) -> Unit,
) {
    val missing = stringResource(Res.string.settings__library_sync_missing)
    val app = stringResource(Res.string.app_name)
    val local = row.change.local?.let { stringResource(it.displayName) } ?: missing
    val remote = row.change.remote?.let { stringResource(it.displayName) } ?: missing
    val shapes = ListItemDefaults.segmentedShapes(index = indexInGroup, count = groupCount)
    val colors = ListItemDefaults.expressiveSegmentedColors()
    val leading: @Composable () -> Unit = { MatchCover(row.game) }
    val supporting: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.settings__library_sync_status_change, local, tracker, remote),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (row.conflict) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(SyncSide.LOCAL to app, SyncSide.REMOTE to tracker).forEachIndexed { i, (side, name) ->
                        SegmentedButton(
                            selected = row.choice == side,
                            onClick = { onChoose(side) },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                        ) { Text(name) }
                    }
                }
            }
        }
    }
    if (row.conflict) {
        SegmentedListItem(
            onClick = {},
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            supportingContent = supporting,
        ) { Text(row.game?.name.orEmpty()) }
    } else {
        SegmentedListItem(
            selected = row.included,
            onClick = onToggle,
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            supportingContent = supporting,
        ) { Text(row.game?.name.orEmpty()) }
    }
}

