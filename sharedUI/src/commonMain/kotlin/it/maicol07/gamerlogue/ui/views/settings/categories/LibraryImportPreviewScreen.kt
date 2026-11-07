package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__id_sync_matches
import gamerlogue.sharedui.generated.resources.settings__import_already_present
import gamerlogue.sharedui.generated.resources.settings__import_change_match
import gamerlogue.sharedui.generated.resources.settings__import_confirm
import gamerlogue.sharedui.generated.resources.settings__import_confirm_match
import gamerlogue.sharedui.generated.resources.settings__import_deselect_all
import gamerlogue.sharedui.generated.resources.settings__import_done
import gamerlogue.sharedui.generated.resources.settings__import_finish
import gamerlogue.sharedui.generated.resources.settings__import_group_to_import
import gamerlogue.sharedui.generated.resources.settings__import_library_title
import gamerlogue.sharedui.generated.resources.settings__import_no_match
import gamerlogue.sharedui.generated.resources.settings__import_searching
import gamerlogue.sharedui.generated.resources.settings__import_select_all
import gamerlogue.sharedui.generated.resources.settings__import_selected
import gamerlogue.sharedui.generated.resources.settings__import_source
import gamerlogue.sharedui.generated.resources.settings__library_sync_continue
import gamerlogue.sharedui.generated.resources.settings__open_store
import gamerlogue.sharedui.generated.resources.settings__service_resync_ids
import gamerlogue.sharedui.generated.resources.settings__sync_title_sync_library
import gamerlogue.sharedui.generated.resources.settings__wishlist_preview_title
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.EditW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.StatusMessage
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.views.list.GamePick
import it.maicol07.gamerlogue.ui.views.list.GamePickEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun LibraryImportPreviewView(service: ExternalService, mode: ImportMode) {
    val navigationState = LocalNavigationState.current
    val title = when (mode) {
        ImportMode.OWNED, ImportMode.TRACKED -> Res.string.settings__import_library_title
        ImportMode.WISHLIST -> Res.string.settings__wishlist_preview_title
        ImportMode.SYNC -> Res.string.settings__sync_title_sync_library
        ImportMode.ID_SYNC -> Res.string.settings__service_resync_ids
    }
    val viewModel: LibraryImportViewModel = koinViewModel(parameters = { parametersOf(service, mode) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onDone: () -> Unit = { navigationState.backStack.removeLastOrNull() }
    val pickRequest = "sync-match-${service.name}-${mode.name}"
    GamePickEffect(pickRequest) { ids -> ids.singleOrNull()?.let(viewModel::chooseMatchId) }
    val onEdit: (Int) -> Unit = { index ->
        viewModel.startEdit(index)
        val query = uiState.rows[index].ref.name.ifBlank { null }
        val pick = GamePick(pickRequest, multiple = false, query = query)
        navigationState.backStack.add(RootNavTree.GameList(null, null, null, null, null, null, pick))
    }
    val plan = uiState.plan
    ScreenScaffold(title = title) {
        when {
            // Outgoing writes are reviewed in the WebView after the incoming selection, even when empty.
            uiState.pushPending -> LaunchedEffect(Unit) {
                navigationState.backStack.removeLastOrNull()
                val action = when (mode) {
                    ImportMode.WISHLIST -> ServiceSyncAction.PUSH_WISHLIST
                    ImportMode.ID_SYNC -> ServiceSyncAction.APPLY_RESYNC_IDS
                    else -> ServiceSyncAction.APPLY_LIBRARY_SYNC
                }
                navigationState.backStack.add(RootNavTree.ServiceSync(service, action))
            }

            plan != null && uiState.importedCount == null -> SyncPlanContent(
                service = service,
                rows = plan,
                applying = uiState.importing,
                onChoose = viewModel::choose,
                onToggle = viewModel::togglePlanRow,
                onApply = viewModel::applySync,
                onDone = onDone,
            )

            else -> LibraryImportPreviewContent(
                service = service,
                matchedTitle = if (mode == ImportMode.ID_SYNC) {
                    Res.string.settings__id_sync_matches
                } else Res.string.settings__import_group_to_import,
                confirmLabel = if (viewModel.hasNextStep) {
                    Res.string.settings__library_sync_continue
                } else {
                    Res.string.settings__import_confirm
                },
                onDone = onDone,
                viewModel = viewModel,
                onEdit = onEdit,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryImportPreviewContent(
    service: ExternalService,
    matchedTitle: StringResource,
    confirmLabel: StringResource,
    onDone: () -> Unit,
    viewModel: LibraryImportViewModel,
    onEdit: (Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    if (uiState.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ContainedLoadingIndicator() }
        return
    }

    uiState.importedCount?.let { count ->
        StatusMessage(
            Icons.CheckCircleW500Rounded,
            pluralStringResource(Res.plurals.settings__import_done, count, count),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialShapes.Sunny,
        ) {
            Button(onClick = onDone, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.settings__import_finish))
            }
        }
        return
    }

    val indexedRows = uiState.rows.withIndex().toList()
    // Mutually-exclusive groups: importable (confident, new), to review (unmatched/low-confidence), present.
    val toImport = indexedRows.filter { !it.value.alreadyPresent && it.value.confident }
    val toReview = indexedRows.filter { !it.value.alreadyPresent && !it.value.confident }
    val alreadyPresent = indexedRows.filter { it.value.alreadyPresent }

    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }
    val selectedCount = uiState.rows.count { it.included }
    val allSelected = toImport.isNotEmpty() && selectedCount == toImport.size

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                pluralStringResource(Res.plurals.settings__import_selected, selectedCount, selectedCount, uiState.rows.size),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { viewModel.setAllIncluded(!allSelected) },
                enabled = toImport.isNotEmpty(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(
                    stringResource(
                        if (allSelected) Res.string.settings__import_deselect_all
                        else Res.string.settings__import_select_all
                    )
                )
            }
        }

        if (uiState.matching) {
            val progress by animateFloatAsState(
                if (uiState.total > 0) uiState.processed.toFloat() / uiState.total else 0f,
                MaterialTheme.motionScheme.defaultEffectsSpec(),
            )
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            importRows(
                ImportGroups(toImport, toReview, alreadyPresent, matchedTitle),
                service,
                expandedSections,
                onSectionExpanded = { key, expanded -> expandedSections[key] = expanded },
                onToggle = viewModel::toggleIncluded,
                onEdit = onEdit,
                onOpenStore = uriHandler::openURL,
            )
        }

        Button(
            onClick = viewModel::confirm,
            enabled = !uiState.matching && !uiState.importing && (selectedCount > 0 || viewModel.hasNextStep),
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            if (uiState.importing) {
                CircularProgressIndicator(Modifier.padding(end = 8.dp).size(20.dp))
            }
            Text(stringResource(confirmLabel))
        }
    }

}

/**
 * One import row. Selectable rows toggle inclusion on tap (expressive `selected` styling, no checkbox);
 * review/already-present rows open the match editor on tap. The trailing edit icon always opens the
 * editor so the match can be changed.
 */
@Composable
internal fun ImportRow(
    row: LibraryImportViewModel.Row,
    indexInGroup: Int,
    groupCount: Int,
    selectable: Boolean,
    service: ExternalService,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onOpenStore: (String) -> Unit,
) {
    val shapes = ListItemDefaults.segmentedShapes(index = indexInGroup, count = groupCount)
    val colors = ListItemDefaults.expressiveSegmentedColors()
    val leading: @Composable () -> Unit = { if (row.searching) SearchingCover() else MatchCover(row.game) }
    val trailing: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (row.sourceUrl != null) {
                IconButton(onClick = { onOpenStore(row.sourceUrl) }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.OpenInNewW500Rounded, contentDescription = stringResource(Res.string.settings__open_store))
                }
            }
            IconButton(onClick = onEdit, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.EditW500Rounded, contentDescription = stringResource(Res.string.settings__import_change_match))
            }
        }
    }
    val supporting: @Composable () -> Unit = {
        Column {
            // Always show what the store calls this game, so the IGDB match can be verified.
            Text(
                stringResource(
                    Res.string.settings__import_source,
                    stringResource(service.labelRes()),
                    row.ref.name.ifEmpty { "#${row.ref.uid}" },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            matchStatus(row)?.let { (text, color) -> Text(text, color = color) }
        }
    }
    val headline = row.game?.name ?: row.ref.name.ifEmpty { row.ref.uid }
    if (selectable) {
        SegmentedListItem(
            selected = row.included,
            onClick = onToggle,
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supporting,
        ) { Text(headline) }
    } else {
        SegmentedListItem(
            onClick = onEdit,
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supporting,
        ) { Text(headline) }
    }
}

@Composable
internal fun MatchCover(game: Game?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    val coverId = game?.cover?.image_id
    if (coverId != null) {
        RemoteImage(
            url = igdbImageUrl(coverId, IgdbImageSize.COVER_SMALL),
            contentDescription = game.name,
            modifier = modifier.size(width = 40.dp, height = 53.dp).clip(shape),
            loadingModifier = modifier.size(width = 40.dp, height = 53.dp).clip(shape),
        )
    } else {
        Box(
            modifier.size(width = 40.dp, height = 53.dp).clip(shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PlaceholderIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The state line under a row's store name, with its color; null for a confirmed match to import. */
@Composable
private fun matchStatus(row: LibraryImportViewModel.Row): Pair<String, Color>? = when {
    row.searching -> stringResource(Res.string.settings__import_searching) to
        MaterialTheme.colorScheme.onSurfaceVariant

    row.alreadyPresent -> stringResource(Res.string.settings__import_already_present) to
        MaterialTheme.colorScheme.onSurfaceVariant

    row.game == null -> stringResource(Res.string.settings__import_no_match) to
        MaterialTheme.colorScheme.error

    !row.confident -> stringResource(Res.string.settings__import_confirm_match) to
        MaterialTheme.colorScheme.tertiary

    else -> null
}

/** [MatchCover]'s slot while the automatic match is still being looked up. */
@Composable
private fun SearchingCover() = Box(Modifier.size(width = 40.dp, height = 53.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(Modifier.size(24.dp))
}

private val PlaceholderIcon: ImageVector = Icons.JoystickW500Rounded
