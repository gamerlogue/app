package it.maicol07.gamerlogue.ui.views.comments

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.comments__edit_confirm_message
import gamerlogue.sharedui.generated.resources.comments__edit_confirm_title
import gamerlogue.sharedui.generated.resources.comments__editing
import gamerlogue.sharedui.generated.resources.comments__has_spoiler
import gamerlogue.sharedui.generated.resources.comments__hint
import gamerlogue.sharedui.generated.resources.comments__new
import gamerlogue.sharedui.generated.resources.comments__rejected
import gamerlogue.sharedui.generated.resources.comments__replying_to
import gamerlogue.sharedui.generated.resources.comments__save
import gamerlogue.sharedui.generated.resources.comments__send
import gamerlogue.sharedui.generated.resources.comments__spoiler
import gamerlogue.sharedui.generated.resources.comments__spoiler_hint
import gamerlogue.sharedui.generated.resources.common__more_options
import gamerlogue.sharedui.generated.resources.common_cancel
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AddW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.EditW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.MoreVertW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ReplyW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SendW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.VisibilityOffW500Rounded
import it.maicol07.gamerlogue.data.CommentSchema
import it.maicol07.gamerlogue.ui.components.RichTextFormatToolbar
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val ComposerMaxLines = 6

/** What the composer holds besides the text: the spoiler flag and whether the field is in use. */
private class ComposerDraft {
    var spoiler by mutableStateOf(false)
    var focused by mutableStateOf(false)
}

/**
 * The bar at the bottom of a screen with comments: a new comment, a reply or an edit, written with
 * rich text and sent as Markdown. The formatting toolbar slides in while the field is in use; the
 * field takes the focus whenever a new comment, a reply or an edit starts. An edit asks before it
 * replaces the comment.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CommentComposer(state: CommentsViewModel.UiState, viewModel: CommentsViewModel, modifier: Modifier = Modifier) {
    val text = rememberRichTextState()
    val draft = remember { ComposerDraft() }
    val focusRequester = remember { FocusRequester() }
    var confirmEdit by remember { mutableStateOf(false) }
    ComposerEffects(state, text, draft, focusRequester)
    val markdown = remember(text.annotatedString) { text.toMarkdown() }
    val tooLong = markdown.length > CommentSchema.MAX_TEXT_LENGTH
    val send: () -> Unit = {
        viewModel.send(markdown, draft.spoiler) {
            text.clear()
            draft.spoiler = false
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemGap)
                .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap / 2),
        ) {
            ComposerStatus(state, draft, tooLong) {
                viewModel.cancelComposer()
                text.clear()
                draft.spoiler = false
            }
            ComposerInput(
                text = text,
                draft = draft,
                focusRequester = focusRequester,
                sending = state.sending,
                canSend = !tooLong && text.annotatedString.text.isNotBlank(),
                onSend = { if (state.editing != null) confirmEdit = true else send() },
            )
            AnimatedVisibility(
                draft.focused || text.annotatedString.text.isNotEmpty(),
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
            ) { RichTextFormatToolbar(text) }
        }
    }
    if (confirmEdit) {
        ConfirmDialog(
            title = Res.string.comments__edit_confirm_title,
            message = Res.string.comments__edit_confirm_message,
            confirm = Res.string.comments__save,
            onConfirm = send,
            onDismiss = { confirmEdit = false },
        )
    }
}

/** Fills the field with the comment being edited, and focuses it whenever there is something to write. */
@Composable
private fun ComposerEffects(
    state: CommentsViewModel.UiState,
    text: RichTextState,
    draft: ComposerDraft,
    focusRequester: FocusRequester,
) {
    LaunchedEffect(state.editing) {
        val editing = state.editing
        if (editing != null) {
            text.setMarkdown(editing.text.orEmpty())
            draft.spoiler = editing.spoiler
        }
    }
    LaunchedEffect(state.composingNew, state.replyTo, state.editing) {
        if (state.composing) focusRequester.requestFocus()
    }
}

/**
 * What the composer is doing (a new comment, an edit, a reply), whether it is a spoiler and whether
 * the text is refused, by the server or for being too long; each line grows in and out.
 */
@Composable
private fun ComposerStatus(
    state: CommentsViewModel.UiState,
    draft: ComposerDraft,
    tooLong: Boolean,
    onCancel: () -> Unit,
) {
    val replyTo = state.replyTo
    AnimatedVisibility(
        state.composing || draft.spoiler,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
            when {
                state.editing != null ->
                    ComposerChip(stringResource(Res.string.comments__editing), Icons.EditW500Rounded, onCancel)
                replyTo != null -> ComposerChip(
                    stringResource(Res.string.comments__replying_to, replyTo.user?.displayName.orEmpty()),
                    Icons.ReplyW500Rounded,
                    onCancel
                )
                state.composingNew ->
                    ComposerChip(stringResource(Res.string.comments__new), Icons.AddW500Rounded, onCancel)
            }
            if (draft.spoiler) {
                ComposerChip(stringResource(Res.string.comments__has_spoiler), Icons.VisibilityOffW500Rounded) {
                    draft.spoiler = false
                }
            }
        }
    }
    AnimatedVisibility(state.rejected || tooLong, enter = expandVertically(), exit = shrinkVertically()) {
        Text(
            stringResource(Res.string.comments__rejected),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ComposerInput(
    text: RichTextState,
    draft: ComposerDraft,
    focusRequester: FocusRequester,
    sending: Boolean,
    canSend: Boolean,
    onSend: () -> Unit,
) = Row(verticalAlignment = Alignment.CenterVertically) {
    RichTextEditor(
        state = text,
        placeholder = { Text(stringResource(Res.string.comments__hint)) },
        maxLines = ComposerMaxLines,
        modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester)
            .onFocusChanged { draft.focused = it.isFocused },
    )
    ComposerMenu(draft)
    FilledIconButton(onClick = onSend, enabled = !sending && canSend, shapes = IconButtonDefaults.shapes()) {
        AnimatedContent(sending, transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() }) {
            if (it) {
                LoadingIndicator(Modifier.size(24.dp), color = LocalContentColor.current)
            } else {
                Icon(Icons.SendW500Rounded, contentDescription = stringResource(Res.string.comments__send))
            }
        }
    }
}

/** The comment's options; for now marking it as a spoiler, with what that means. */
@Composable
private fun ComposerMenu(draft: ComposerDraft) = Box {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, shapes = IconButtonDefaults.shapes()) {
        Icon(Icons.MoreVertW500Rounded, contentDescription = stringResource(Res.string.common__more_options))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = {
                Column {
                    Text(stringResource(Res.string.comments__spoiler))
                    Text(
                        stringResource(Res.string.comments__spoiler_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingIcon = { Icon(Icons.VisibilityOffW500Rounded, contentDescription = null) },
            trailingIcon = { Switch(checked = draft.spoiler, onCheckedChange = null) },
            onClick = { draft.spoiler = !draft.spoiler },
        )
    }
}

/** A yes/no question before an action that changes or removes something for everyone. */
@Composable
internal fun ConfirmDialog(
    title: StringResource,
    message: StringResource,
    confirm: StringResource,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(title)) },
    text = { Text(stringResource(message)) },
    confirmButton = {
        TextButton(
            onClick = {
                onDismiss()
                onConfirm()
            },
            shapes = ButtonDefaults.shapes(),
        ) { Text(stringResource(confirm)) }
    },
    dismissButton = {
        TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(Res.string.common_cancel))
        }
    },
)

@Composable
private fun ComposerChip(label: String, icon: ImageVector, onCancel: () -> Unit) = InputChip(
    selected = true,
    onClick = onCancel,
    label = { Text(label) },
    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    trailingIcon = {
        Icon(
            Icons.CloseW500Rounded,
            contentDescription = stringResource(Res.string.common_cancel),
            modifier = Modifier.size(18.dp)
        )
    },
)
