package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__import_already_present
import gamerlogue.sharedui.generated.resources.settings__import_cancel
import gamerlogue.sharedui.generated.resources.settings__import_confirm_match
import gamerlogue.sharedui.generated.resources.settings__import_confirm
import gamerlogue.sharedui.generated.resources.settings__import_deselect_all
import gamerlogue.sharedui.generated.resources.settings__import_done
import gamerlogue.sharedui.generated.resources.settings__import_finish
import gamerlogue.sharedui.generated.resources.settings__import_no_match
import gamerlogue.sharedui.generated.resources.settings__import_search
import gamerlogue.sharedui.generated.resources.settings__import_search_hint
import gamerlogue.sharedui.generated.resources.settings__import_select_all
import gamerlogue.sharedui.generated.resources.settings__import_selected
import gamerlogue.sharedui.generated.resources.settings__import_source
import gamerlogue.sharedui.generated.resources.settings__open_store
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.ui.components.RemoteImage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryImportPreviewScreen(
    service: ExternalService,
    mode: ImportMode,
    onDone: () -> Unit,
    viewModel: LibraryImportViewModel = koinViewModel(parameters = { parametersOf(service, mode) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    if (uiState.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    uiState.importedCount?.let { count ->
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.settings__import_done, count), style = MaterialTheme.typography.titleMedium)
            Button(onClick = onDone) { Text(stringResource(Res.string.settings__import_finish)) }
        }
        return
    }

    val selectedCount = uiState.rows.count { it.included }
    val selectableCount = uiState.rows.count { it.confident && !it.alreadyPresent }
    val allSelected = selectableCount > 0 && selectedCount == selectableCount

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.settings__import_selected, selectedCount, uiState.rows.size),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { viewModel.setAllIncluded(!allSelected) },
                enabled = selectableCount > 0,
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
            val progress = if (uiState.total > 0) uiState.processed.toFloat() / uiState.total else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(uiState.rows) { index, row ->
                SegmentedListItem(
                    colors = ListItemDefaults.expressiveSegmentedColors(),
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = uiState.rows.size),
                    onClick = { viewModel.startEdit(index) },
                    leadingContent = { MatchCover(row.game) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (row.sourceUrl != null) {
                                IconButton(onClick = { uriHandler.openURL(row.sourceUrl) }) {
                                    Icon(
                                        Icons.OpenInNewW500Rounded,
                                        contentDescription = stringResource(Res.string.settings__open_store),
                                    )
                                }
                            }
                            Checkbox(
                                checked = row.included,
                                enabled = row.confident && !row.alreadyPresent,
                                onCheckedChange = { viewModel.toggleIncluded(index) },
                            )
                        }
                    },
                    supportingContent = {
                        Column {
                            // Always show what the store calls this game, so the IGDB match can be verified.
                            Text(
                                stringResource(
                                    Res.string.settings__import_source,
                                    service.name,
                                    row.ref.name.ifEmpty { "#${row.ref.uid}" },
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val status = when {
                                row.alreadyPresent -> stringResource(Res.string.settings__import_already_present) to
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                row.game == null -> stringResource(Res.string.settings__import_no_match) to
                                    MaterialTheme.colorScheme.error
                                !row.confident -> stringResource(Res.string.settings__import_confirm_match) to
                                    MaterialTheme.colorScheme.tertiary
                                else -> null
                            }
                            status?.let { (text, color) -> Text(text, color = color) }
                        }
                    },
                ) {
                    Text(row.game?.name ?: row.ref.name.ifEmpty { row.ref.uid })
                }
            }
        }

        Button(
            onClick = viewModel::confirm,
            enabled = !uiState.matching && !uiState.importing && selectedCount > 0,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            if (uiState.importing) {
                CircularProgressIndicator(Modifier.padding(end = 8.dp).size(20.dp))
            }
            Text(stringResource(Res.string.settings__import_confirm))
        }
    }

    if (uiState.editingIndex != null) {
        MatchSearchDialog(
            searching = uiState.searching,
            results = uiState.searchResults,
            onSearch = viewModel::search,
            onPick = viewModel::chooseMatch,
            onDismiss = viewModel::cancelEdit,
        )
    }
}

@Composable
private fun MatchCover(game: Game?, modifier: Modifier = Modifier) {
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

private val PlaceholderIcon: ImageVector = Icons.JoystickW500Rounded

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchSearchDialog(
    searching: Boolean,
    results: List<Game>,
    onSearch: (String) -> Unit,
    onPick: (Game) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSearch(query) }, enabled = !searching) {
                Text(stringResource(Res.string.settings__import_search))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.settings__import_cancel)) } },
        title = { Text(stringResource(Res.string.settings__import_search_hint)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(Res.string.settings__import_search_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (searching) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        items(results) { game ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onPick(game) }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MatchCover(game, Modifier.padding(end = 12.dp))
                                Text(game.name)
                            }
                        }
                    }
                }
            }
        },
    )
}
