package it.maicol07.gamerlogue.ui.views.comments

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.comments__delete
import gamerlogue.sharedui.generated.resources.comments__delete_confirm_message
import gamerlogue.sharedui.generated.resources.comments__delete_confirm_title
import gamerlogue.sharedui.generated.resources.comments__edit
import gamerlogue.sharedui.generated.resources.comments__edited
import gamerlogue.sharedui.generated.resources.comments__empty
import gamerlogue.sharedui.generated.resources.comments__hide_replies
import gamerlogue.sharedui.generated.resources.comments__in_reply_to
import gamerlogue.sharedui.generated.resources.comments__reply
import gamerlogue.sharedui.generated.resources.comments__show_replies
import gamerlogue.sharedui.generated.resources.comments__title
import gamerlogue.sharedui.generated.resources.profile__report
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DeleteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.EditW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FlagW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ReplyW500Rounded
import it.maicol07.gamerlogue.core.PagedState
import it.maicol07.gamerlogue.data.Comment
import it.maicol07.gamerlogue.data.InteractionTarget
import it.maicol07.gamerlogue.extensions.displayTimestamp
import it.maicol07.gamerlogue.ui.components.layout.loadingItem
import it.maicol07.gamerlogue.ui.components.social.ReportDialog
import it.maicol07.gamerlogue.ui.components.social.SpoilerContent
import it.maicol07.gamerlogue.ui.components.user.UserAvatar
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val ReplyIndent = 32.dp
private val CommentActionIconSize = 20.dp

/** Replies indent up to this depth; deeper ones stay aligned and name who they answer instead. */
private const val MaxIndentDepth = 3

/** What every comment of a section shares. [contentOwnerId] owns the review or list: they can delete any comment. */
private class CommentsContext(
    val state: CommentsViewModel.UiState,
    val viewModel: CommentsViewModel,
    val contentOwnerId: String?,
    val onUserClick: (String) -> Unit,
)

/** The comment threads, as items of the host screen's list. */
fun LazyListScope.commentsSection(
    roots: PagedState<Comment>,
    state: CommentsViewModel.UiState,
    viewModel: CommentsViewModel,
    contentOwnerId: String?,
    onUserClick: (String) -> Unit,
) {
    val context = CommentsContext(state, viewModel, contentOwnerId, onUserClick)
    item(key = "comments-title") {
        Text(
            stringResource(Res.string.comments__title),
            style = MaterialTheme.typography.titleMediumEmphasized,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SectionGap)
        )
    }
    items(
        roots.items,
        key = { "comment-${it.id}" }
    ) { root -> CommentThread(root, depth = 0, parentAuthor = null, context, Modifier.animateItem()) }
    loadingItem(roots.loading)
    if (roots.isEmpty) {
        item(key = "comments-empty") {
            Text(
                stringResource(Res.string.comments__empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
            )
        }
    }
}

/**
 * A comment and, when expanded, its replies, each a thread of its own. The replies unfold and fold
 * back; while folding, the last ones loaded stay drawn until they are gone.
 */
@Composable
private fun CommentThread(
    comment: Comment,
    depth: Int,
    parentAuthor: String?,
    context: CommentsContext,
    modifier: Modifier = Modifier,
): Unit = Column(modifier) {
    CommentItem(comment, depth, parentAuthor, context)
    val replies = context.state.replies[comment.id]
    var shownReplies by remember { mutableStateOf(replies.orEmpty()) }
    if (replies != null) shownReplies = replies
    AnimatedVisibility(
        replies != null,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
    ) {
        Column(Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())) {
            shownReplies.forEach { reply ->
                key(reply.id) { CommentThread(reply, depth + 1, comment.user?.displayName, context) }
            }
        }
    }
}

/** One comment; the one being edited or answered in the composer is tinted, fading in and out. */
@Composable
private fun CommentItem(comment: Comment, depth: Int, parentAuthor: String?, context: CommentsContext) {
    var reportOpen by remember { mutableStateOf(false) }
    val inComposer = comment.id != null && comment.id in listOf(context.state.editing?.id, context.state.replyTo?.id)
    val tint by animateColorAsState(
        if (inComposer) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.ItemGap + ReplyIndent * minOf(depth, MaxIndentDepth),
                end = Dimens.ItemGap,
                top = Dimens.ItemGap / 2
            )
            .background(tint, MaterialTheme.shapes.large)
            .padding(Dimens.ItemGap),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
    ) {
        val userId = comment.userId
        UserAvatar(
            comment.user,
            if (depth > 0) 28.dp else 36.dp,
            Modifier.clip(CircleShape).let { m -> userId?.let { id -> m.clickable { context.onUserClick(id) } } ?: m }
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CommentMeta(comment)
            CommentBody(comment, parentAuthor.takeIf { depth > MaxIndentDepth })
            CommentReactionsRow(comment, context.state, context.viewModel)
            CommentActions(comment, context, onReport = { reportOpen = true })
            if (comment.repliesCount > 0) {
                RepliesToggle(
                    comment,
                    expanded = comment.id in context.state.replies,
                    loading = comment.id in context.state.loadingReplies,
                    onToggle = { context.viewModel.toggleReplies(comment) },
                )
            }
        }
    }
    if (reportOpen) comment.id?.let { ReportDialog(InteractionTarget.COMMENT, it, onDismiss = { reportOpen = false }) }
}

/** The text; past the indentation limit, prefixed by who it answers, since the layout no longer shows it. */
@Composable
private fun CommentBody(comment: Comment, answered: String?) {
    if (answered != null) {
        Text(
            stringResource(Res.string.comments__in_reply_to, answered),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    val text = comment.text.orEmpty()
    SpoilerContent(text, comment.spoiler, Modifier) { CommentMarkdown(text) }
}

/** The comment's Markdown, as written with the composer's formatting. */
@OptIn(ExperimentalRichTextApi::class)
@Composable
private fun CommentMarkdown(markdown: String) {
    val state = remember(markdown) { RichTextState().setMarkdown(markdown) }
    RichText(state, style = MaterialTheme.typography.bodyMedium)
}

/** Reply, then edit and delete when allowed, or report someone else's comment: few enough for icons. */
@Composable
private fun CommentActions(comment: Comment, context: CommentsContext, onReport: () -> Unit) = Row(
    verticalAlignment = Alignment.CenterVertically,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val currentUserId = context.viewModel.currentUserId
    val isMine = comment.userId != null && comment.userId == currentUserId
    val canDelete = isMine || (context.contentOwnerId != null && context.contentOwnerId == currentUserId)
    CommentAction(Icons.ReplyW500Rounded, Res.string.comments__reply, loading = false) {
        context.viewModel.startReply(comment)
    }
    if (isMine) {
        CommentAction(Icons.EditW500Rounded, Res.string.comments__edit, loading = false) {
            context.viewModel.startEdit(comment)
        }
    }
    if (canDelete) {
        CommentAction(Icons.DeleteW500Rounded, Res.string.comments__delete, comment.id in context.state.deleting) {
            confirmDelete = true
        }
    }
    if (!isMine) CommentAction(Icons.FlagW500Rounded, Res.string.profile__report, loading = false, onClick = onReport)
    if (confirmDelete) {
        ConfirmDialog(
            title = Res.string.comments__delete_confirm_title,
            message = Res.string.comments__delete_confirm_message,
            confirm = Res.string.comments__delete,
            onConfirm = { context.viewModel.delete(comment) },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** An action icon; while its request runs, a loading indicator takes the icon's place. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CommentAction(icon: ImageVector, label: StringResource, loading: Boolean, onClick: () -> Unit) =
    IconButton(onClick = onClick, enabled = !loading, shapes = IconButtonDefaults.shapes()) {
        AnimatedContent(loading, transitionSpec = { fadeIn() togetherWith fadeOut() }) { busy ->
            if (busy) {
                LoadingIndicator(Modifier.size(CommentActionIconSize))
            } else {
                Icon(icon, contentDescription = stringResource(label), modifier = Modifier.size(CommentActionIconSize))
            }
        }
    }

/** Shows or hides the replies, with an indicator while they load. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RepliesToggle(comment: Comment, expanded: Boolean, loading: Boolean, onToggle: () -> Unit) =
    TextButton(onClick = onToggle, enabled = !loading, shapes = ButtonDefaults.shapes()) {
        Text(
            if (expanded) {
                stringResource(Res.string.comments__hide_replies)
            } else {
                pluralStringResource(Res.plurals.comments__show_replies, comment.repliesCount, comment.repliesCount)
            }
        )
        AnimatedVisibility(loading) {
            LoadingIndicator(Modifier.padding(start = Dimens.ItemGap).size(CommentActionIconSize))
        }
    }

@Composable
private fun CommentMeta(comment: Comment) = Row(
    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
    verticalAlignment = Alignment.CenterVertically,
) {
    comment.user?.let { Text(it.displayName, style = MaterialTheme.typography.labelLarge) }
    displayTimestamp(comment.createdAt)?.let {
        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (comment.edited) {
        Text(
            stringResource(Res.string.comments__edited),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
