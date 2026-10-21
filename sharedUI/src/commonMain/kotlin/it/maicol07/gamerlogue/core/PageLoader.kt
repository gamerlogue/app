package it.maicol07.gamerlogue.core

import it.maicol07.gamerlogue.extensions.ResourcePage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Infinite-scroll state of one backend collection. */
data class PagedState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = false,
    val endReached: Boolean = false,
    /** The last page failed; the failure was already reported by the request. */
    val failed: Boolean = false,
) {
    /** Loaded and nothing there. */
    val isEmpty: Boolean get() = items.isEmpty() && endReached && !failed
}

/**
 * Loads a page-based backend collection one page at a time, for the lists that grow as they
 * scroll (reviews, activities, comments, notifications, users). [fetch] gets the 1-based page and
 * returns null when the request failed, which ends the list instead of retrying it in a loop.
 */
class PageLoader<T>(
    private val scope: CoroutineScope,
    private val fetch: suspend (page: Int) -> ResourcePage<T>?,
) {
    val state: StateFlow<PagedState<T>>
        field = MutableStateFlow(PagedState())

    private var lastPage = 0
    private var job: Job? = null

    /** Starts again from the first page; what is loaded stays on screen until that page replaces it. */
    fun refresh() {
        job?.cancel()
        state.update { it.copy(loading = true, endReached = false, failed = false) }
        job = scope.launch {
            val page = fetch(1)
            lastPage = if (page == null) 0 else 1
            state.value = if (page == null) {
                PagedState(endReached = true, failed = true)
            } else {
                PagedState(page.items, endReached = page.isLast(page.items.size))
            }
        }
    }

    /** Whether anything was loaded or found missing, i.e. whether a [refresh] has something to redo. */
    val isStarted: Boolean
        get() = state.value.let { it.items.isNotEmpty() || it.endReached || it.loading }

    /** Loads the first page, unless something is already loaded, loading or known to be empty. */
    fun ensureLoaded() {
        val current = state.value
        if (current.items.isEmpty() && !current.endReached) loadMore()
    }

    /** Loads the next page, unless one is loading or the end was reached. */
    fun loadMore() {
        val current = state.value
        if (current.loading || current.endReached) return
        state.update { it.copy(loading = true) }
        job = scope.launch {
            val page = fetch(lastPage + 1)
            if (page == null) {
                state.update { it.copy(loading = false, endReached = true, failed = true) }
                return@launch
            }
            lastPage++
            state.update {
                val items = it.items + page.items
                it.copy(items = items, loading = false, endReached = page.isLast(items.size))
            }
        }
    }

    /** Rewrites the loaded items in place, e.g. after a like or a deletion, without a reload. */
    fun updateItems(transform: (List<T>) -> List<T>) = state.update { it.copy(items = transform(it.items)) }
}
