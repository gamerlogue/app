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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.app_name
import gamerlogue.sharedui.generated.resources.common_close
import gamerlogue.sharedui.generated.resources.settings__library_sync_apply
import gamerlogue.sharedui.generated.resources.settings__library_sync_group_conflicts
import gamerlogue.sharedui.generated.resources.settings__library_sync_group_from
import gamerlogue.sharedui.generated.resources.settings__library_sync_group_to
import gamerlogue.sharedui.generated.resources.settings__library_sync_missing
import gamerlogue.sharedui.generated.resources.settings__library_sync_nothing
import gamerlogue.sharedui.generated.resources.settings__library_sync_status_change
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckCircleW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.SyncSide
import it.maicol07.gamerlogue.ui.components.StatusMessage
import it.maicol07.gamerlogue.ui.views.settings.components.SettingsGroupHeader
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * [ImportMode.SYNC] step 2: the sync plan in three groups — tracker → Gamerlogue, Gamerlogue → tracker and
 * conflicts, where the user picks which status to keep. Indices passed to [onChoose] are plan indices.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SyncPlanContent(
    service: ExternalService,
    rows: List<LibraryImportViewModel.PlanRow>,
    applying: Boolean,
    onChoose: (Int, SyncSide) -> Unit,
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
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            val (conflicts, settled) = indexed.partition { it.value.conflict }
            val (pulled, pushed) = settled.partition { it.value.choice == SyncSide.REMOTE }
            planGroup(Res.string.settings__library_sync_group_from, tracker, pulled, onChoose)
            planGroup(Res.string.settings__library_sync_group_to, tracker, pushed, onChoose)
            planGroup(Res.string.settings__library_sync_group_conflicts, tracker, conflicts, onChoose)
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

private fun LazyListScope.planGroup(
    titleRes: StringResource,
    tracker: String,
    rows: List<IndexedValue<LibraryImportViewModel.PlanRow>>,
    onChoose: (Int, SyncSide) -> Unit,
) {
    if (rows.isEmpty()) return
    item { SettingsGroupHeader(stringResource(titleRes, tracker, rows.size)) }
    itemsIndexed(rows) { indexInGroup, (index, row) ->
        PlanItem(row, tracker, indexInGroup, rows.size) { onChoose(index, it) }
    }
}

/** A plan row: the game, its status on each side and, for a conflict, the side to keep. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlanItem(
    row: LibraryImportViewModel.PlanRow,
    tracker: String,
    indexInGroup: Int,
    groupCount: Int,
    onChoose: (SyncSide) -> Unit,
) {
    val missing = stringResource(Res.string.settings__library_sync_missing)
    val app = stringResource(Res.string.app_name)
    val local = row.change.local?.let { stringResource(it.displayName) } ?: missing
    val remote = row.change.remote?.let { stringResource(it.displayName) } ?: missing
    SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(index = indexInGroup, count = groupCount),
        colors = ListItemDefaults.expressiveSegmentedColors(),
        leadingContent = { MatchCover(row.game) },
        supportingContent = {
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
        },
    ) { Text(row.game?.name.orEmpty()) }
}
