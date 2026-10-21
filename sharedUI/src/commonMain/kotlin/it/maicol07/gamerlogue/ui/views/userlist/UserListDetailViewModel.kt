package it.maicol07.gamerlogue.ui.views.userlist

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.model.Game
import com.github.michaelbull.result.get
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.ApiErrorCodes
import it.maicol07.gamerlogue.data.IgdbLookup
import it.maicol07.gamerlogue.data.InteractionTarget
import it.maicol07.gamerlogue.data.LikeState
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.data.UserList
import it.maicol07.gamerlogue.data.UserListItem
import it.maicol07.gamerlogue.extensions.allPages
import it.maicol07.gamerlogue.extensions.detachedCopy
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

/** A list with its games; its owner can edit, reorder, annotate and delete it. */
@KoinViewModel
class UserListDetailViewModel(
    @InjectedParam private val listId: String,
) : StateViewModel<UserListDetailViewModel.UiState>(UiState()) {
    data class UiState(
        val list: UserList? = null,
        /** In list order. */
        val items: List<UserListItem> = emptyList(),
        val games: Map<Int, Game> = emptyMap(),
        val loading: Boolean = true,
        val isOwner: Boolean = false,
        val like: LikeState = LikeState(liked = false, count = 0),
        val reordering: Boolean = false,
        /** Requests the screen shows an indicator for. */
        val liking: Boolean = false,
        val addingGames: Boolean = false,
        val savingList: Boolean = false,
        val deletingList: Boolean = false,
        /** Items by id whose removal or note is being saved. */
        val savingItems: Set<String> = emptySet(),
    ) {
        /** A change to the whole list is on its way. */
        val busy: Boolean get() = reordering || addingGames || savingList || deletingList
    }

    private val lookup by inject<IgdbLookup>()
    private val api by inject<ProfileApi>()
    private val authProvider by inject<AuthTokenProvider>()

    fun load() = viewModelScope.launch {
        val list = safeRequest(ApiErrorCodes.HIDDEN_CONTENT) { UserList.includes("user").find(listId).data }.get()
        val items = list?.let { safeRequest { UserListItem.where("list", listId).allPages() }.get() }.orEmpty()
            .sortedBy { it.position }
        val games = safeRequest { lookup.games(items.map { it.gameId }) }.get().orEmpty()
        update {
            copy(
                list = list,
                items = items,
                games = games,
                loading = false,
                isOwner = list?.userId != null && list.userId == authProvider.session.value.userId,
                like = LikeState(list?.likedByMe == true, list?.likesCount ?: 0),
            )
        }
    }

    fun edit(form: UserListForm) = viewModelScope.launch {
        val list = state.list ?: return@launch
        val edited = UserList().apply {
            id = list.id
            isPersisted = true
            form.applyTo(this)
        }
        update { copy(savingList = true) }
        val saved = safeRequest { edited.save() }.isOk
        // The response carries the whole list, but not the included owner: keep it from the loaded one.
        val merged = if (saved) list.detachedCopy(::UserList).apply { attributes.putAll(edited.attributes) } else list
        update { copy(savingList = false, list = merged) }
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        val list = state.list ?: return@launch
        update { copy(deletingList = true) }
        val deleted = safeRequest { list.destroy() }.isOk
        update { copy(deletingList = false) }
        if (deleted) onDeleted()
    }

    fun toggleLike() = viewModelScope.launch {
        update { copy(liking = true) }
        val like = safeRequest { api.toggleLike(InteractionTarget.LIST, listId, state.like) }.get()
        update { copy(liking = false, like = like ?: this.like) }
    }

    /** Moves the item at [index] by [offset] places and saves the order: the accessibility path of a drag. */
    fun move(index: Int, offset: Int) {
        val target = index + offset
        if (target !in state.items.indices || state.reordering) return
        update { copy(items = items.toMutableList().apply { add(target, removeAt(index)) }) }
        saveOrder()
    }

    /** Moves item [fromId] where [toId] is while it is dragged, on screen only: [saveOrder] follows the drop. */
    fun reorder(fromId: String, toId: String) = update {
        val from = items.indexOfFirst { it.id == fromId }
        val to = items.indexOfFirst { it.id == toId }
        if (from < 0 || to < 0) this else copy(items = items.toMutableList().apply { add(to, removeAt(from)) })
    }

    /** Saves the order on screen; the backend takes the whole order at once. */
    fun saveOrder() = viewModelScope.launch {
        update { copy(reordering = true) }
        val result = safeRequest { api.reorderList(listId, state.items.mapNotNull { it.id }) }
        // A failed reorder leaves the server order untouched: reload it rather than show a wrong one.
        update { copy(reordering = false) }
        if (result.isErr) load()
    }

    /** Appends the games not in the list yet, in the order given: without a position the server puts each last. */
    fun addGames(gameIds: List<Int>) = viewModelScope.launch {
        val listId = this@UserListDetailViewModel.listId.toInt()
        val present = state.items.map { it.gameId }.toSet()
        update { copy(addingGames = true) }
        gameIds.filterNot(present::contains).forEach { gameId ->
            safeRequest {
                UserListItem().apply {
                    this.listId = listId
                    this.gameId = gameId
                }.save()
            }
        }
        load().join()
        update { copy(addingGames = false) }
    }

    fun remove(item: UserListItem) = viewModelScope.launch {
        val id = item.id ?: return@launch
        update { copy(savingItems = savingItems + id) }
        val removed = safeRequest { item.destroy() }.isOk
        update { copy(savingItems = savingItems - id, items = if (removed) items - item else items) }
    }

    fun editNote(item: UserListItem, note: String) = viewModelScope.launch {
        val edited = UserListItem().apply {
            id = item.id
            isPersisted = true
            this.note = note.trim().ifBlank { null }
        }
        val id = item.id ?: return@launch
        update { copy(savingItems = savingItems + id) }
        val saved = safeRequest { edited.save() }.isOk
        update {
            copy(
                savingItems = savingItems - id,
                items = if (saved) items.map { if (it.id == id) edited else it } else items,
            )
        }
    }
}
