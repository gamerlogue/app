package it.maicol07.gamerlogue.ui.views.comments

import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.PageLoader
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.ApiErrorCodes
import it.maicol07.gamerlogue.data.Comment
import it.maicol07.gamerlogue.data.CommentReactions
import it.maicol07.gamerlogue.data.CommentSchema
import it.maicol07.gamerlogue.data.InteractionTarget
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.data.reactions
import it.maicol07.gamerlogue.data.withMine
import it.maicol07.gamerlogue.extensions.allPages
import it.maicol07.gamerlogue.extensions.fetchPage
import it.maicol07.gamerlogue.extensions.jsonApiErrorCode
import it.maicol07.spraypaintkt.Scope
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

/**
 * The comments of a review or a list: its direct comments page by page, the replies of each
 * expanded comment loaded whole, the reactions, and the composer (new comment, reply or edit).
 */
@KoinViewModel
class CommentsViewModel(
    @InjectedParam private val target: InteractionTarget,
    @InjectedParam private val targetId: String,
) : StateViewModel<CommentsViewModel.UiState>(UiState()) {
    data class UiState(
        /** Loaded replies by comment id; a comment missing here is collapsed. */
        val replies: Map<String, List<Comment>> = emptyMap(),
        /** Reactions changed here, by comment id, over the loaded ones. */
        val reactions: Map<String, CommentReactions> = emptyMap(),
        /** The comment being answered, or null for a comment on the review or list itself. */
        val replyTo: Comment? = null,
        val editing: Comment? = null,
        val sending: Boolean = false,
        /** The server refused the text (an empty or overlong field). */
        val rejected: Boolean = false,
        /** The comment whose reaction the server refused (not a single emoji). */
        val rejectedReaction: String? = null,
        /** A new comment on the review or list is being written: the screen shows the composer for it. */
        val composingNew: Boolean = false,
        /** Comments by id with a request running: deletion, reaction, loading their replies. */
        val deleting: Set<String> = emptySet(),
        val reacting: Set<String> = emptySet(),
        val loadingReplies: Set<String> = emptySet(),
    ) {
        /** The composer has something to do: a new comment, a reply or an edit. */
        val composing: Boolean get() = composingNew || replyTo != null || editing != null

        fun reactionsOf(comment: Comment): CommentReactions = reactions[comment.id] ?: comment.reactions
    }

    private val api by inject<ProfileApi>()
    private val authProvider by inject<AuthTokenProvider>()

    val currentUserId: String? get() = authProvider.session.value.userId

    val roots = PageLoader(viewModelScope) { page ->
        safeRequest(ApiErrorCodes.HIDDEN_CONTENT) { commentsOf(target.apiType, targetId).fetchPage(page) }.get()
    }

    fun toggleReplies(comment: Comment) {
        val id = comment.id ?: return
        if (id in state.replies) {
            update { copy(replies = replies - id) }
        } else {
            loadReplies(id)
        }
    }

    private fun loadReplies(commentId: String) = viewModelScope.launch {
        update { copy(loadingReplies = loadingReplies + commentId) }
        val replies = safeRequest { commentsOf(InteractionTarget.COMMENT.apiType, commentId).allPages() }.get()
        update {
            copy(
                loadingReplies = loadingReplies - commentId,
                replies = if (replies == null) this.replies else this.replies + (commentId to replies),
            )
        }
    }

    fun startNew() = update { copy(composingNew = true, replyTo = null, editing = null, rejected = false) }

    fun startReply(comment: Comment) =
        update { copy(replyTo = comment, editing = null, composingNew = false, rejected = false) }

    fun startEdit(comment: Comment) =
        update { copy(editing = comment, replyTo = null, composingNew = false, rejected = false) }

    fun cancelComposer() = update { copy(replyTo = null, editing = null, composingNew = false, rejected = false) }

    /** Sends the composer's text: an edit, a reply or a new comment, depending on the composer. */
    fun send(text: String, spoiler: Boolean, onSent: () -> Unit) = viewModelScope.launch {
        val trimmed = text.trim().take(CommentSchema.MAX_TEXT_LENGTH)
        if (trimmed.isEmpty()) return@launch
        update { copy(sending = true, rejected = false) }
        val editing = state.editing
        val comment = draft(editing, state.replyTo, target, targetId)
        comment.text = trimmed
        comment.spoiler = spoiler
        val result = safeRequest(::isRejection) { comment.save() }
        update { copy(sending = false, rejected = result.getError()?.jsonApiErrorCode?.let(::isRejection) == true) }
        if (result.isErr) return@launch
        cancelComposer()
        onSent()
        reloadSiblings(editing ?: comment)
    }

    /** Deletes [comment] with every reply below it, then reloads the list it was in. */
    fun delete(comment: Comment) = viewModelScope.launch {
        val id = comment.id ?: return@launch
        update { copy(deleting = deleting + id) }
        val deleted = safeRequest { comment.destroy() }.isOk
        update { copy(deleting = deleting - id, replies = if (deleted) replies - id else replies) }
        if (deleted) reloadSiblings(comment)
    }

    /** Reloads the comments next to [comment]: its parent's replies, or the direct comments. */
    private fun reloadSiblings(comment: Comment) {
        if (comment.commentableType == InteractionTarget.COMMENT.apiType) {
            loadReplies(comment.commentableId.toString())
        } else {
            roots.refresh()
        }
    }

    /** Sets the current user's reaction on [comment] to [type] (`like` or an emoji), or takes it back with null. */
    fun react(comment: Comment, type: String?) = viewModelScope.launch {
        val id = comment.id ?: return@launch
        update { copy(rejectedReaction = null, reacting = reacting + id) }
        val result = safeRequest(ApiErrorCodes::isValidation) { api.setReaction(InteractionTarget.COMMENT, id, type) }
        update {
            if (result.isOk) {
                copy(reacting = reacting - id, reactions = reactions + (id to reactionsOf(comment).withMine(type)))
            } else {
                copy(
                    reacting = reacting - id,
                    rejectedReaction = id.takeIf { result.getError()?.jsonApiErrorCode?.let(ApiErrorCodes::isValidation) == true }
                )
            }
        }
    }
}

/** The comment to save: the edited one by id, or a new one on [replyTo] or on [target] [targetId] itself. */
private fun draft(editing: Comment?, replyTo: Comment?, target: InteractionTarget, targetId: String) = Comment().apply {
    if (editing != null) {
        id = editing.id
        isPersisted = true
    } else {
        commentableType = replyTo?.let { InteractionTarget.COMMENT.apiType } ?: target.apiType
        commentableId = (replyTo?.id ?: targetId).toInt()
    }
}

/** The server refused the comment's content rather than failing. */
private fun isRejection(code: String) = ApiErrorCodes.isValidation(code) || code == ApiErrorCodes.REVIEW_REQUIRED

/** The comments directly on [id] of [type] (a review, a list or a comment), with their authors. */
private fun commentsOf(type: String, id: String): Scope<Comment> = Comment.scope {
    where("commentable_type", type)
    where("commentable_id", id)
    includes("user")
}
