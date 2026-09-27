package it.maicol07.gamerlogue.ui.views.events

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.apicalypse.ApicalypseQueryBuilder
import at.released.igdbclient.apicalypse.SortOrder
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.getEvents
import at.released.igdbclient.model.Event
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.extensions.sort
import it.maicol07.gamerlogue.extensions.where
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject
import kotlin.time.Clock

/**
 * Upcoming and previous gaming events from IGDB, shared by the Discover carousels and the full
 * events list.
 *
 * Upcoming events are a handful, so they are loaded in one request; previous ones number in the
 * thousands and are paginated by [onEndReached], [pageSize] at a time.
 */
@KoinViewModel
class EventsViewModel(@InjectedParam private val pageSize: Int) : StateViewModel<EventsViewModel.UiState>(UiState()) {
    data class UiState(
        val upcoming: List<Event> = emptyList(),
        val past: List<Event> = emptyList(),
        val loading: Boolean = true,
        val error: Boolean = false,
        val loadingMorePast: Boolean = false,
        val pastPageError: Boolean = false,
        val pastEndReached: Boolean = false,
    )

    companion object {
        /** Page size of the full events list. */
        const val LIST_PAGE_SIZE = 100

        /** Page size of the Discover preview, which never paginates. */
        const val PREVIEW_PAGE_SIZE = 20

        private const val PrefetchThreshold = 6
    }

    private val igdb by inject<IgdbClient>()

    /**
     * Epoch seconds splitting upcoming from past, frozen per [load]: the past pages are fetched by
     * offset, so a moving boundary would shift them and repeat events across pages.
     */
    private var cutoff = 0L

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        update { copy(loading = true, error = false, pastPageError = false) }
        cutoff = Clock.System.now().epochSeconds

        // An event is over once it ends, not once it starts, so a multi-day event running today
        // stays in "upcoming". IGDB leaves `end_time` unset on part of its events, hence the
        // fallback on `start_time`; the clause is raw because it mixes OR and AND.
        val upcoming = async {
            fetchEvents {
                where { raw("(end_time >= $cutoff | (end_time = null & start_time >= $cutoff))") }
                sort(Event.field.start_time, SortOrder.ASC)
            }
        }
        val past = async { fetchPastEvents(offset = 0) }

        val upcomingEvents = upcoming.await()
        val pastEvents = past.await()
        update {
            if (upcomingEvents == null || pastEvents == null) {
                copy(loading = false, error = true)
            } else {
                copy(
                    upcoming = upcomingEvents,
                    past = pastEvents,
                    loading = false,
                    pastEndReached = pastEvents.size < pageSize,
                )
            }
        }
    }

    /** Loads the next page of previous events once the list is scrolled near its end. */
    fun onEndReached(lastVisibleIndex: Int) {
        if (state.loading || state.loadingMorePast || state.pastPageError || state.pastEndReached) return
        val total = state.upcoming.size + state.past.size
        if (lastVisibleIndex < total - PrefetchThreshold) return
        loadMorePast()
    }

    /** Fetches the next page of previous events; also the retry after a failed page. */
    fun loadMorePast() = viewModelScope.launch {
        update { copy(loadingMorePast = true, pastPageError = false) }
        val page = fetchPastEvents(offset = state.past.size)
        update {
            if (page == null) {
                copy(loadingMorePast = false, pastPageError = true)
            } else {
                copy(past = past + page, loadingMorePast = false, pastEndReached = page.size < pageSize)
            }
        }
    }

    private suspend fun fetchPastEvents(offset: Int): List<Event>? = fetchEvents {
        where { raw("(end_time < $cutoff | (end_time = null & start_time < $cutoff))") }
        sort(Event.field.start_time, SortOrder.DESC)
        offset(offset)
    }

    /** Null when the request failed; [safeRequest] has already reported it. */
    private suspend fun fetchEvents(query: ApicalypseQueryBuilder.() -> Unit): List<Event>? {
        val result = safeRequest {
            igdb.getEvents {
                fields(
                    Event.field.name,
                    Event.field.start_time,
                    Event.field.end_time,
                    Event.field.event_logo.image_id,
                )
                query()
                limit(pageSize)
            }
        }
        return if (result.isOk) result.unwrap().events else null
    }
}
