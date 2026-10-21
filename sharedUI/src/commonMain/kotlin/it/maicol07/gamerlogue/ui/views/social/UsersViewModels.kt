package it.maicol07.gamerlogue.ui.views.social

import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.get
import it.maicol07.gamerlogue.core.BaseViewModel
import it.maicol07.gamerlogue.core.PageLoader
import it.maicol07.gamerlogue.data.ApiErrorCodes
import it.maicol07.gamerlogue.data.Block
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.extensions.ResourcePage
import it.maicol07.gamerlogue.extensions.fetchPage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.milliseconds

/** Which side of a user's follow graph to list; travels in the navigation key. */
@Serializable
enum class ConnectionsKind {
    FOLLOWERS,
    FOLLOWING,
}

/** A user's followers or followed users. */
@KoinViewModel
class ConnectionsViewModel(
    @InjectedParam private val userId: String,
    @InjectedParam private val kind: ConnectionsKind,
) : BaseViewModel() {
    private val api by inject<ProfileApi>()

    val users = PageLoader(viewModelScope) { page ->
        safeRequest(ApiErrorCodes.HIDDEN_CONTENT) {
            when (kind) {
                ConnectionsKind.FOLLOWERS -> api.followers(userId, page)
                ConnectionsKind.FOLLOWING -> api.following(userId, page)
            }
        }.get()
    }
}

/** The users the signed-in user blocked, each of which can be unblocked from here. */
@KoinViewModel
class BlockedUsersViewModel : BaseViewModel() {
    private val api by inject<ProfileApi>()

    /** The loaded blocks by blocked user id, to lift one without looking it up again. */
    private val blocks = mutableMapOf<String, Block>()

    val users = PageLoader(viewModelScope) { page ->
        safeRequest { api.blocks(page) }.get()?.let { result ->
            result.items.forEach { block -> blocks[block.blockedId] = block }
            ResourcePage(result.items.mapNotNull { it.blocked as? User }, result.total)
        }
    }

    fun unblock(user: User) = viewModelScope.launch {
        val block = blocks[user.id] ?: return@launch
        if (safeRequest { block.destroy() }.isOk) users.updateItems { list -> list.filterNot { it.id == user.id } }
    }
}

/** Searches users by nickname or name; blocked users never show up. */
@KoinViewModel
class UserSearchViewModel : BaseViewModel() {
    val query: StateFlow<String>
        field = MutableStateFlow("")

    val users = PageLoader(viewModelScope) { page ->
        val text = query.value.trim()
        if (text.isEmpty()) {
            ResourcePage(emptyList(), 0)
        } else {
            safeRequest { User.where("search", text).fetchPage(page) }.get()
        }
    }

    private var searchJob: Job? = null

    /** Searches [text] after a short pause, so typing a name costs one request. */
    fun setQuery(text: String) {
        query.value = text
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MILLIS.milliseconds)
            users.refresh()
        }
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 300L
    }
}
