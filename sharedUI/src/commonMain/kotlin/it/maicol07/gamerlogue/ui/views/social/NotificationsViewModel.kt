package it.maicol07.gamerlogue.ui.views.social

import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.github.michaelbull.result.get
import it.maicol07.gamerlogue.core.BaseViewModel
import it.maicol07.gamerlogue.core.PageLoader
import it.maicol07.gamerlogue.core.UnreadNotifications
import it.maicol07.gamerlogue.data.AppNotification
import it.maicol07.gamerlogue.data.Comment
import it.maicol07.gamerlogue.data.InteractionTarget
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.data.UserList
import it.maicol07.gamerlogue.extensions.detachedCopy
import it.maicol07.gamerlogue.extensions.fetchPage
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.spraypaintkt.Resource
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

/** The signed-in user's notifications, newest first; opening one marks it read. */
@KoinViewModel
class NotificationsViewModel : BaseViewModel() {
    private val api by inject<ProfileApi>()
    private val unread by inject<UnreadNotifications>()

    val notifications =
        PageLoader(viewModelScope) { page -> safeRequest { AppNotification.scope().fetchPage(page) }.get() }

    fun markRead(notification: AppNotification) = viewModelScope.launch {
        val id = notification.id ?: return@launch
        if (notification.isRead) return@launch
        if (safeRequest { api.markNotificationRead(id) }.isOk) {
            notifications.updateItems { list -> list.map { if (it.id == id) it.markedRead() else it } }
            unread.refresh()
        }
    }

    /** Marks [notification] read and opens what it is about. */
    fun open(notification: AppNotification, onNavigate: (NavKey) -> Unit) = viewModelScope.launch {
        markRead(notification)
        val subjectId = notification.text("subject_id")
        val actorId = (notification.data["actor"] as? Map<*, *>)?.get("id")?.toString()
        val target = when (notification.text("subject_type")) {
            // The signed-in user's own review: no owner needed to open it.
            InteractionTarget.REVIEW.apiType -> subjectId?.let { RootNavTree.ReviewDetail(it, null) }
            InteractionTarget.LIST.apiType -> subjectId?.let(RootNavTree::UserListDetail)
            InteractionTarget.COMMENT.apiType -> subjectId?.let { safeRequest { threadTarget(it) }.get() }
            else -> actorId?.let(RootNavTree::UserProfile)
        }
        target?.let(onNavigate)
    }

    /**
     * The review or list a comment's thread is about: replies are comments on comments, so the
     * comment's ancestors are walked up to the content at the root.
     */
    private suspend fun threadTarget(commentId: String): NavKey? {
        var id: String? = commentId
        var target: NavKey? = null
        var depth = 0
        while (id != null && target == null && depth < MAX_THREAD_DEPTH) {
            val parent = Comment.includes("commentable").find(id).data.relationships["commentable"] as? Resource
            id = (parent as? Comment)?.id
            target = when (parent) {
                is LibraryEntry -> parent.id?.let { RootNavTree.ReviewDetail(it, parent.userId) }
                is UserList -> parent.id?.let(RootNavTree::UserListDetail)
                else -> null
            }
            depth++
        }
        return target
    }

    fun markAllRead() = viewModelScope.launch {
        if (safeRequest { api.markAllNotificationsRead() }.isOk) {
            notifications.updateItems { list -> list.map { if (it.isRead) it else it.markedRead() } }
            unread.refresh()
        }
    }

    /** A read copy: the state needs a new instance to recompose. */
    private fun AppNotification.markedRead() = detachedCopy(::AppNotification).apply { isRead = true }

    private companion object {
        /** A guard on the walk up a thread, far deeper than any real one. */
        const val MAX_THREAD_DEPTH = 50
    }
}
