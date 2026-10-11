package it.maicol07.gamerlogue.ui.views.calendar

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.apicalypse.ApicalypseQueryBuilder
import at.released.igdbclient.apicalypse.SortOrder
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.getGames
import at.released.igdbclient.model.Game
import at.released.igdbclient.multiquery
import com.github.michaelbull.result.get
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder
import it.maicol07.gamerlogue.extensions.allPages
import it.maicol07.gamerlogue.extensions.currentUserEntries
import it.maicol07.gamerlogue.extensions.igdb.releaseDay
import it.maicol07.gamerlogue.extensions.multiqueryResults
import it.maicol07.gamerlogue.extensions.sort
import it.maicol07.gamerlogue.extensions.where
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.gamerlogue.ui.views.list.FilterOptions
import it.maicol07.gamerlogue.ui.views.list.GameListFilterState
import it.maicol07.gamerlogue.ui.views.list.applyFilters
import it.maicol07.gamerlogue.ui.views.list.baseGamesOnly
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject
import kotlin.time.Clock

/**
 * Upcoming releases from today on, in release order, filtered like a game list; with
 * [GameListFilterState.backlogOnly] only the games in the user's backlog. That choice is remembered
 * across openings, and the backlog is refetched whenever the library changes.
 */
@KoinViewModel
class CalendarViewModel(
    private val preferences: AppPreferences,
    libraryChanges: LibraryChanges,
) : StateViewModel<CalendarViewModel.UiState>(
    UiState(filterState = GameListFilterState(backlogOnly = preferences.calendarBacklogOnly))
) {
    data class UiState(
        val games: List<Game> = emptyList(),
        val loading: Boolean = false,
        val endReached: Boolean = false,
        val filterState: GameListFilterState = GameListFilterState(),
        val showFilterSheet: Boolean = false,
        /** The day the timeline should scroll to, set once the games up to it are loaded. */
        val scrollTarget: LocalDate? = null,
    )

    private companion object {
        const val PAGE_SIZE = 50

        /** IGDB's largest `limit`: catching up to a far day takes as few requests as possible. */
        const val MAX_LIMIT = 500

        /** IGDB's cap on the sub-queries of one multiquery. */
        const val MAX_SUBQUERIES = 10
    }

    val filterOptions = FilterOptions(viewModelScope, emptyMap())

    private val igdb: IgdbClient by inject()
    private var offset = 0
    private var loadJob: Job? = null

    init {
        load(reset = true)
        viewModelScope.launch { libraryChanges.changes.collect { refreshBacklog() } }
    }

    fun toggleFilterSheet(show: Boolean) {
        update { copy(showFilterSheet = show) }
        if (show) filterOptions.loadDefaults()
    }

    fun updateFilter(newFilterState: GameListFilterState) {
        update { copy(filterState = newFilterState) }
        preferences.calendarBacklogOnly = newFilterState.backlogOnly
        load(reset = true)
    }

    fun resetFilter() {
        updateFilter(GameListFilterState())
        filterOptions.clearSearches()
    }

    /** Loads the next page once the last day of the timeline is on screen. */
    fun onEndReached() {
        if (!state.loading && !state.endReached) load(reset = false)
    }

    /** Loads the games up to [day], since the timeline only scrolls over loaded days, then asks it to scroll there. */
    fun jumpTo(day: LocalDate) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            update { copy(loading = true) }
            while (!state.endReached && state.games.lastOrNull()?.releaseDay?.let { it < day } != false) {
                appendPage(MAX_LIMIT)
            }
            update { copy(loading = false, scrollTarget = day) }
        }
    }

    fun onScrolledToTarget() = update { copy(scrollTarget = null) }

    /**
     * Refetches the backlog in place: its page replaces the games rather than appending, so the
     * timeline keeps its scroll position. The full timeline does not depend on the library.
     */
    private fun refreshBacklog() {
        if (!state.filterState.backlogOnly) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            appendPage(PAGE_SIZE)
            // A load this one cancelled may have left the indicator on.
            update { copy(loading = false) }
        }
    }

    /** Loads a page, replacing any load still in flight so a stale page never lands on a new filter. */
    private fun load(reset: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (reset) {
                offset = 0
                update { copy(games = emptyList(), endReached = false) }
            }
            update { copy(loading = true) }
            appendPage(PAGE_SIZE)
            update { copy(loading = false) }
        }
    }

    private suspend fun appendPage(limit: Int) {
        val filter = state.filterState
        if (filter.backlogOnly) {
            val releases = fetchBacklogReleases(filter)
            update { copy(games = releases, endReached = true) }
            return
        }
        val page = safeRequest {
            igdb.getGames {
                timelineQuery()
                where {
                    upcoming(filter)
                    baseGamesOnly(filter)
                }
                offset(offset)
                limit(limit)
            }
        }.get()?.games.orEmpty()
        offset += limit
        // A failed request ends the timeline like an exhausted source, so it is never retried in a
        // loop and the advanced offset is never used.
        update { copy(games = games + page, endReached = page.size < limit) }
    }

    /**
     * Every upcoming game in the backlog, in release order, at once: only upcoming games match, so
     * they are few even in a large backlog.
     *
     * The ids are split in chunks of [MAX_LIMIT], so each sub-query returns all its matches in one
     * page, and the chunks go [MAX_SUBQUERIES] per multiquery. The library keeps editions, so they
     * are not dropped here.
     */
    private suspend fun fetchBacklogReleases(filter: GameListFilterState): List<Game> {
        val ids = safeRequest { LibraryEntry.currentUserEntries(GameLibraryStatus.BACKLOG).allPages() }
            .get()
            .orEmpty()
            .map { it.gameId }
            .distinct()
        return ids.chunked(MAX_LIMIT).chunked(MAX_SUBQUERIES).flatMap { batch ->
            val results = safeRequest {
                igdb.multiquery {
                    batch.forEachIndexed { index, chunk ->
                        query(IgdbEndpoint.GAME, "backlog$index") {
                            timelineQuery()
                            where {
                                upcoming(filter)
                                Game.field.id inAny chunk.map(Int::toString)
                            }
                            limit(MAX_LIMIT)
                        }
                    }
                }
            }.get().orEmpty()
            batch.indices.flatMap { results.multiqueryResults<Game>("backlog$it") }
        }.sortedBy { it.first_release_date?.getEpochSecond() }
    }
}

/** Fields and order of every timeline query; the `where` is the caller's, since a second one replaces the first. */
private fun ApicalypseQueryBuilder.timelineQuery() {
    fields(
        Game.field.name.igdbFullName,
        Game.field.cover.image_id.igdbFullName,
        Game.field.first_release_date.igdbFullName,
    )
    sort(Game.field.first_release_date, SortOrder.ASC)
}

/** Releases from today's start on, matching [filter]. */
private fun ApicalypseQueryBuilderWhereBuilder.upcoming(filter: GameListFilterState) {
    // IGDB stores release days as UTC midnights: today's releases start at today's UTC midnight.
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).atStartOfDayIn(TimeZone.UTC).epochSeconds
    Game.field.first_release_date greaterThanOrEqual today
    applyFilters(filter)
}

/** [this] grouped by release day, keeping the input order across and within days; undated games are dropped. */
internal fun List<Game>.groupByReleaseDay(): Map<LocalDate, List<Game>> =
    mapNotNull { game -> game.releaseDay?.let { it to game } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
