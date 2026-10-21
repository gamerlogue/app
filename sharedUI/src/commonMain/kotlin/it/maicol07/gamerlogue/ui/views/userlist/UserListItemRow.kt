package it.maicol07.gamerlogue.ui.views.userlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.activity__unknown_game
import gamerlogue.sharedui.generated.resources.common_cancel
import gamerlogue.sharedui.generated.resources.lists__note
import gamerlogue.sharedui.generated.resources.lists__remove_game
import gamerlogue.sharedui.generated.resources.lists__save
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DeleteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DragIndicatorW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.EditNoteW500Rounded
import it.maicol07.gamerlogue.data.UserListItem
import it.maicol07.gamerlogue.data.UserListItemSchema
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val ItemCoverWidth = 48.dp
private val ItemCoverHeight = 64.dp
private val DragHandleSize = 48.dp
private val DraggedElevation = 6.dp

/** What the list's owner gets on each row: the drag handle and the note and remove buttons. */
internal class OwnerControls(
    /** Drags the row; for accessibility services, the [moves] (moving up, down) do it instead. */
    val handle: Modifier,
    val dragging: Boolean,
    val moves: List<Pair<StringResource, () -> Unit>>,
    /** Its removal or note is being saved: an indicator takes the buttons' place. */
    val saving: Boolean,
    val onRemove: () -> Unit,
    val onEditNote: (String) -> Unit,
)

/**
 * One game of the list, a segment of the list's group; [owner] adds the controls of the list's owner.
 * The dragged row lifts.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ListItemRow(
    item: UserListItem,
    rank: Int?,
    game: Game?,
    shapes: ListItemShapes,
    owner: OwnerControls?,
    onClick: () -> Unit,
) {
    var noteOpen by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(if (owner?.dragging == true) DraggedElevation else 0.dp)
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = ListItemDefaults.expressiveSegmentedColors(),
        modifier = Modifier
            .padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = ListItemDefaults.SegmentedGap)
            .shadow(elevation, MaterialTheme.shapes.large, clip = false),
        leadingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
            ) {
                owner?.let { DragHandle(it) }
                if (rank != null) Text("$rank", style = MaterialTheme.typography.titleLargeEmphasized)
                game?.CoverImage(
                    Modifier.clip(MaterialTheme.shapes.small),
                    sizeModifier = Modifier.width(ItemCoverWidth).height(ItemCoverHeight)
                )
            }
        },
        supportingContent = item.note?.let { note -> { Text(note) } },
        trailingContent = owner?.let { controls -> { OwnerButtons(controls) { noteOpen = true } } },
    ) {
        Text(
            game?.name ?: stringResource(Res.string.activity__unknown_game),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (noteOpen && owner != null) NoteDialog(item.note, onSave = owner.onEditNote, onDismiss = { noteOpen = false })
}

/** Note and remove, or an indicator in their place while either is saved. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OwnerButtons(owner: OwnerControls, onNote: () -> Unit) =
    AnimatedContent(owner.saving, transitionSpec = { fadeIn() togetherWith fadeOut() }) { saving ->
        if (saving) {
            LoadingIndicator(Modifier.size(DragHandleSize))
        } else {
            Row {
                IconButton(onClick = onNote, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.EditNoteW500Rounded, contentDescription = stringResource(Res.string.lists__note))
                }
                IconButton(onClick = owner.onRemove, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.DeleteW500Rounded, contentDescription = stringResource(Res.string.lists__remove_game))
                }
            }
        }
    }

/** The handle the row is dragged by; screen readers move the row with the up and down actions instead. */
@Composable
private fun DragHandle(owner: OwnerControls) {
    val moves = owner.moves.map { (label, move) ->
        CustomAccessibilityAction(stringResource(label)) {
            move()
            true
        }
    }
    Box(
        modifier = owner.handle
            .size(DragHandleSize)
            .semantics { customActions = moves },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.DragIndicatorW500Rounded,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NoteDialog(initial: String?, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val note = rememberTextFieldState(initial.orEmpty())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.lists__note)) },
        text = { TextField(state = note, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            TextButton(
                enabled = note.text.length <= UserListItemSchema.MAX_NOTE_LENGTH,
                onClick = {
                    onSave(note.text.toString())
                    onDismiss()
                },
                shapes = ButtonDefaults.shapes(),
            ) { Text(stringResource(Res.string.lists__save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )
}
