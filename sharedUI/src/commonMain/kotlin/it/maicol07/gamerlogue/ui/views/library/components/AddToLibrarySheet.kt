package it.maicol07.gamerlogue.ui.views.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.library__add_to_library
import gamerlogue.sharedui.generated.resources.library__cancel
import gamerlogue.sharedui.generated.resources.library__completion_status
import gamerlogue.sharedui.generated.resources.library__delete
import gamerlogue.sharedui.generated.resources.library__edit_entry
import gamerlogue.sharedui.generated.resources.library__save
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.LibraryEntrySchema
import it.maicol07.gamerlogue.ui.components.ButtonProgress
import it.maicol07.gamerlogue.ui.components.TooltipBox
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameAddEditLibrarySheet(
    game: Game,
    existingData: LibraryEntrySchema? = null,
    onDismiss: () -> Unit,
    viewModel: AddToLibrarySheetViewModel = koinViewModel(
        key = "${game.id}_${existingData.hashCode()}",
    ) { parametersOf(game, existingData) },
    onDelete: () -> Unit = {},
    onSave: (LibraryEntry) -> Unit
) {
    val status = viewModel.selectedStatus
    val isBacklog = status == GameLibraryStatus.BACKLOG
    val isPlayingOrPaused = status == GameLibraryStatus.PLAYING || status == GameLibraryStatus.PAUSED
    val isEditing = existingData != null

    ModalBottomSheet(onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            statusSection(game.name, viewModel, isEditing)
            item { OwnedSwitch(viewModel.owned, onOwnedChange = { viewModel.owned = it }) }
            item { EditionSection() }
            item { PlatformSection(game, viewModel) }
            if (!isBacklog) {
                item {
                    Column(modifier = Modifier.animateItem()) {
                        DatesSection(viewModel, showEndDate = !isPlayingOrPaused)
                    }
                }
                item {
                    Box(modifier = Modifier.animateItem()) {
                        PlayedTimeField(viewModel)
                    }
                }
            }

            if (!isBacklog && !isPlayingOrPaused) {
                item {
                    Column(modifier = Modifier.animateItem()) {
                        RatingSection(viewModel)
                    }
                }
                item {
                    Box(modifier = Modifier.animateItem()) {
                        ReviewSection(viewModel)
                    }
                }
            }

            if (viewModel.error != null) {
                item {
                    Text(
                        text = stringResource(viewModel.error!!),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().animateItem()
                    )
                }
            }

            item {
                ActionsRow(
                    saveLoading = viewModel.saveLoading,
                    deleteLoading = viewModel.deleteLoading,
                    isEditing = isEditing,
                    onDismiss = onDismiss,
                    onDelete = {
                        viewModel.deleteEntry {
                            onDelete()
                            onDismiss()
                        }
                    },
                    onSave = {
                        viewModel.saveEntry {
                            onSave(it)
                            onDismiss()
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun LazyListScope.statusSection(
    gameName: String,
    viewModel: AddToLibrarySheetViewModel,
    isEditing: Boolean
) {
    item {
        Text(
            text = stringResource(
                Res.string.run {
                    if (isEditing) library__edit_entry else library__add_to_library
                },
                gameName
            ),
            style = MaterialTheme.typography.headlineSmall
        )
    }
    item {
        LibraryStatusSelector(
            selectedStatus = viewModel.selectedStatus,
            onSectionStatus = { viewModel.selectedStatus = it }
        )
    }

    // Completion status (only for the COMPLETED section)
    if (viewModel.selectedStatus == GameLibraryStatus.COMPLETED) {
        item("completion_status") {
            Text(
                text = stringResource(Res.string.library__completion_status),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.animateItem()
            )
            CompletionStatusChips(
                selectedStatus = viewModel.completionStatus,
                onStatusSelected = { viewModel.completionStatus = it },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LibraryStatusSelector(
    selectedStatus: GameLibraryStatus?,
    onSectionStatus: (GameLibraryStatus) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (section in GameLibraryStatus.entries) {
            TooltipBox(
                tooltip = { PlainTooltip { Text(stringResource(section.displayName)) } }
            ) {
                FilledIconToggleButton(
                    checked = selectedStatus == section,
                    onCheckedChange = { onSectionStatus(section) },
                    shapes = IconButtonDefaults.toggleableShapes()
                ) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = stringResource(section.displayName)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionStatusChips(
    selectedStatus: LibraryEntrySchema.CompletionStatus?,
    onStatusSelected: (LibraryEntrySchema.CompletionStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        LibraryEntrySchema.CompletionStatus.entries.forEach { status ->
            FilterChip(
                leadingIcon = {
                    AnimatedVisibility(selectedStatus == status) {
                        Icon(
                            imageVector = Icons.CheckW500Rounded,
                            contentDescription = null
                        )
                    }
                },
                selected = selectedStatus == status,
                onClick = {
                    onStatusSelected(if (selectedStatus == status) null else status)
                },
                label = { Text(stringResource(status.displayName)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionsRow(
    saveLoading: Boolean = false,
    deleteLoading: Boolean = false,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            shapes = ButtonDefaults.shapes(),
            onClick = onDismiss
        ) {
            Text(stringResource(Res.string.library__cancel))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isEditing) {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    enabled = !saveLoading && !deleteLoading,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = onDelete
                ) {
                    ButtonProgress(deleteLoading) {
                        Text(stringResource(Res.string.library__delete))
                    }
                }
            }
            Button(
                shapes = ButtonDefaults.shapes(),
                enabled = !saveLoading && !deleteLoading,
                onClick = onSave
            ) {
                ButtonProgress(saveLoading) {
                    Text(stringResource(Res.string.library__save))
                }
            }
        }
    }
}
