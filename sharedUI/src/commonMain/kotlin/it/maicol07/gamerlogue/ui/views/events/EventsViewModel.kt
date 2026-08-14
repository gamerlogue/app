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
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Upcoming and previous gaming events from IGDB, shared by the Discover carousels and the full
 * events list.
 *
 * Upcoming events are a handful, so they are loaded in one request; previous ones number in the
 * thousands and are paginated by [loadMorePast].
 */
@KoinViewModel
class EventsViewModel : StateViewModel<EventsViewModel.UiState>(UiState()) {
    data class UiState(
        val upcoming: List<Event> = emptyList(),
        val past: List<Event> = emptyList(),
        val loading: Boolean = true,
        val loadingMorePast: Boolean = false,
        val pastEndReached: Boolean = false,
    )

    private companion object {
        const val PageSize = 100
        const val PrefetchThreshold = 6
    }

    private val igdb by inject<IgdbClient>()
    private var pastOffset = 0

    init {
        load()
    }

    @OptIn(ExperimentalTime::class)
    fun load() = viewModelScope.launch {
        update { copy(loading = true) }
        pastOffset = 0

        // An event is over once it ends, not once it starts, so a multi-day event running today
        // stays in "upcoming". IGDB leaves `end_time` unset on part of its events, hence the
        // fallback on `start_time`; the clause is raw because it mixes OR and AND.
        val upcoming = async {
            fetchEvents {
                where { raw(upcomingClause()) }
                sort(Event.field.start_time, SortOrder.ASC)
            }
        }
        val past = async { fetchPastEvents(offset = 0) }

        val upcomingEvents = upcoming.await()
        val pastEvents = past.await()
        pastOffset = pastEvents.size
        update {
            copy(
                upcoming = upcomingEvents,
                past = pastEvents,
                loading = false,
                pastEndReached = pastEvents.size < PageSize,
            )
        }
    }

    /** Loads the next page of previous events once the list is scrolled near its end. */
    fun onEndReached(lastVisibleIndex: Int) {
        if (state.loading || state.loadingMorePast || state.pastEndReached) return
        val total = state.upcoming.size + state.past.size
        if (lastVisibleIndex < total - PrefetchThreshold) return

        viewModelScope.launch {
            update { copy(loadingMorePast = true) }
            val page = fetchPastEvents(pastOffset)
            pastOffset += page.size
            update {
                copy(
                    past = past + page,
                    loadingMorePast = false,
                    pastEndReached = page.size < PageSize,
                )
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun fetchPastEvents(offset: Int): List<Event> = fetchEvents {
        where { raw(pastClause()) }
        sort(Event.field.start_time, SortOrder.DESC)
        offset(offset)
    }

    @OptIn(ExperimentalTime::class)
    private fun upcomingClause(): String {
        val now = Clock.System.now().epochSeconds
        return "(end_time >= $now | (end_time = null & start_time >= $now))"
    }

    @OptIn(ExperimentalTime::class)
    private fun pastClause(): String {
        val now = Clock.System.now().epochSeconds
        return "(end_time < $now | (end_time = null & start_time < $now))"
    }

    private suspend fun fetchEvents(query: ApicalypseQueryBuilder.() -> Unit): List<Event> {
        val result = safeRequest {
            igdb.getEvents {
                fields(
                    Event.field.name,
                    Event.field.start_time,
                    Event.field.end_time,
                    Event.field.event_logo.image_id,
                )
                query()
                limit(PageSize)
            }
        }
        return if (result.isOk) result.unwrap().events else emptyList()
    }
}
