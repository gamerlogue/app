package it.maicol07.gamerlogue.ui.views.list

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.apicalypse.ApicalypseQueryBuilder
import at.released.igdbclient.apicalypse.SortOrder
import at.released.igdbclient.dsl.field.IgdbRequestField
import at.released.igdbclient.dsl.field.IgdbRequestFieldDsl
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.getCompanies
import at.released.igdbclient.getEvents
import at.released.igdbclient.getFranchises
import at.released.igdbclient.getGameEngines
import at.released.igdbclient.getGameTimeToBeat
import at.released.igdbclient.getGames
import at.released.igdbclient.getKeywords
import at.released.igdbclient.model.Company
import at.released.igdbclient.model.Event
import at.released.igdbclient.model.Franchise
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameEngine
import at.released.igdbclient.model.GameTimeToBeat
import at.released.igdbclient.model.Keyword
import at.released.igdbclient.model.PopularityPrimitive
import at.released.igdbclient.multiquery
import com.github.michaelbull.result.get
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder
import it.maicol07.gamerlogue.extensions.allPages
import it.maicol07.gamerlogue.extensions.alreadyReleased
import it.maicol07.gamerlogue.extensions.currentUserEntries
import it.maicol07.gamerlogue.extensions.igdb.sortedByIds
import it.maicol07.gamerlogue.extensions.multiqueryResults
import it.maicol07.gamerlogue.extensions.notYetReleased
import it.maicol07.gamerlogue.extensions.sort
import it.maicol07.gamerlogue.extensions.where
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/** Selectable range of grid columns, shared by the view model and the filter sheet's slider. */
const val MinColumns = 1
const val MaxColumns = 10

/** Upper bound of the "time to beat" slider, in hours; at the maximum the filter is off. */
const val MaxHoursToBeat = 100f

/** Upper bound of both rating sliders; at the maximum the filter is off. */
const val MaxRating = 100f

/** Bounds of the release-year range. The upper bound follows the clock, so next year's announced
 *  games stay selectable; both ends are shared by the filter state, the query and the slider. */
const val MinReleaseYear = 1970

val MaxReleaseYear = Clock.System.now().toLocalDateTime(TimeZone.UTC).year + 1

enum class ReleaseStatusFilter {
    ALL,
    RELEASED,
    UPCOMING,
}

enum class SortField {
    POPULARITY,
    USER_RATING,
    CRITICS_RATING,
    RELEASE_DATE,
    NAME,
}

enum class SortDirection {
    DESC,
    ASC,
}

/** A company's involvement in a game; maps to the boolean flags on IGDB's `involved_companies`. */
enum class CompanyRole(val igdbField: String) {
    DEVELOPER("developer"),
    PUBLISHER("publisher"),
    PORTING("porting"),
    SUPPORTING("supporting"),
}

/** The filter sections whose options are looked up on IGDB instead of being a fixed list. */
enum class FilterSearchTarget {
    COMPANY,
    FRANCHISE,
    ENGINE,
    KEYWORD,
}

data class NamedSearchResult(
    val id: Int,
    val name: String,
    val logoImageId: String? = null
)

/** Query, results and loading flag of a single [FilterSearchTarget] lookup. */
data class FilterSearchState(
    val query: String = "",
    val results: List<NamedSearchResult> = emptyList(),
    val loading: Boolean = false,
)

data class GameListFilterState(
    val searchQuery: String = "",
    val sortField: SortField = SortField.POPULARITY,
    val sortDirection: SortDirection = SortDirection.DESC,
    val minUserRating: Float = 0f,
    val maxUserRating: Float = MaxRating,
    val minCriticsRating: Float = 0f,
    val maxCriticsRating: Float = MaxRating,
    val minReleaseYear: Int = MinReleaseYear,
    val maxReleaseYear: Int = MaxReleaseYear,
    val releaseStatus: ReleaseStatusFilter = ReleaseStatusFilter.ALL,
    val platformIds: Set<Int> = emptySet(),
    val playerPerspectiveIds: Set<Int> = emptySet(),
    val categoryIds: Set<Int> = emptySet(),
    val statusIds: Set<Int> = emptySet(),
    val genreIds: Set<Int> = emptySet(),
    val themeIds: Set<Int> = emptySet(),
    val gameModeIds: Set<Int> = emptySet(),
    val companyIds: Set<Int> = emptySet(),
    /** Per-company roles. A company absent from the map, or mapped to an empty set, matches any role. */
    val companyRoles: Map<Int, Set<CompanyRole>> = emptyMap(),
    val franchiseIds: Set<Int> = emptySet(),
    val gameEngineIds: Set<Int> = emptySet(),
    val keywordIds: Set<Int> = emptySet(),
    val minHoursToBeat: Float = 0f,
    val maxHoursToBeat: Float = MaxHoursToBeat,
    /** Only applies in library scope. */
    val library: LibraryFilterState = LibraryFilterState(),
)

/**
 * Backs the search bar's expanded pane: a paginated, filterable game grid.
 *
 * With no filter and a [section] set it replays that Discover carousel's query so "see all"
 * paginates exactly what the carousel previewed; as soon as any filter or query is applied, it
 * switches to a plain filtered games query. With an [eventId] the list is scoped to that IGDB
 * event's games, with a [libraryStatus] to the user's library entries in that status.
 */
@KoinViewModel
@Suppress("TooManyFunctions")
class GameListViewModel(
    @InjectedParam private val section: DiscoverSection?,
    @InjectedParam private val eventId: Int?,
    @InjectedParam preset: GameListPreset?,
    @InjectedParam private val libraryStatus: GameLibraryStatus?,
) : StateViewModel<GameListViewModel.UiState>(UiState.from(preset)) {
    /** Immutable state of the search results pane. */
    data class UiState(
        /** The scoped event with its full details, once loaded, backs the list header. */
        val event: Event? = null,
        /** In library scope, the user's entry for each listed game id, for the card badges. */
        val libraryEntries: Map<Long, LibraryEntry> = emptyMap(),
        val games: List<Game> = emptyList(),
        val loading: Boolean = false,
        val endReached: Boolean = false,
        val columnCount: Int = 3,
        val filterState: GameListFilterState = GameListFilterState(),
        val showFilterSheet: Boolean = false,
        val filterSearches: Map<FilterSearchTarget, FilterSearchState> = emptyMap(),
        /** Options shown by a searchable filter section before the user types anything. */
        val defaultOptions: Map<FilterSearchTarget, List<NamedSearchResult>> = emptyMap(),
        /**
         * Every option ever shown by a searchable section, by id: a selection keeps its name after
         * the query moves on and while the sheet is closed, since the filter state only stores ids.
         */
        val knownOptions: Map<FilterSearchTarget, Map<Int, NamedSearchResult>> = emptyMap(),
    ) {
        companion object {
            /** Initial state, pre-filtered on [preset] when the list was opened from a game's metadata. */
            fun from(preset: GameListPreset?): UiState {
                if (preset == null) return UiState()
                val target = preset.searchTarget
                return UiState(
                    filterState = preset.applyTo(GameListFilterState()),
                    knownOptions = if (target != null) {
                        mapOf(target to mapOf(preset.id to NamedSearchResult(preset.id, preset.name)))
                    } else {
                        emptyMap()
                    }
                )
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val PREFETCH_THRESHOLD = 6
        const val FILTER_SEARCH_LIMIT = 10
        const val DEBOUNCE_MILLIS = 300L
        const val SECONDS_PER_HOUR = 3600
        const val DEFAULT_OPTIONS_SAMPLE_SIZE = 60
        const val DEFAULT_OPTIONS_PER_TARGET = 12
        const val MIN_RATINGS_FOR_SAMPLE = 300
    }

    private val igdb: IgdbClient by inject()
    private var offset = 0
    private val filterSearchJobs = mutableMapOf<FilterSearchTarget, Job>()
    private var defaultOptionsJob: Job? = null
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    /** The event's game ids, fetched once per event scope; see [eventGameIdPage]. */
    private var eventGameIds: List<Int>? = null

    /** The library status' game ids, fetched once per library scope; see [libraryGameIdPage]. */
    private var libraryGameIds: List<Int>? = null

    init {
        load(reset = true)
    }

    fun setColumnCount(count: Int) {
        update { copy(columnCount = count.coerceIn(MinColumns, MaxColumns)) }
    }

    fun toggleFilterSheet(show: Boolean) {
        update { copy(showFilterSheet = show) }
        if (show) loadDefaultFilterOptions()
    }

    /**
     * Populates the empty-query options of the searchable filter sections.
     *
     * IGDB exposes no popularity metric on companies, franchises, engines or keywords, so the
     * options are derived: sample the most-rated games and rank each facet by how often it occurs.
     * That keeps names and logos authoritative instead of hardcoding ids that drift (two of the
     * previously hardcoded company ids pointed at the wrong companies entirely).
     */
    private fun loadDefaultFilterOptions() {
        if (state.defaultOptions.isNotEmpty() || defaultOptionsJob?.isActive == true) return
        defaultOptionsJob = viewModelScope.launch {
            val result = safeRequest {
                igdb.getGames {
                    fields(
                        Game.field.involved_companies.company.name,
                        Game.field.involved_companies.company.logo.image_id,
                        Game.field.franchises.name,
                        Game.field.game_engines.name,
                        Game.field.game_engines.logo.image_id,
                    )
                    where { Game.field.total_rating_count greaterThan MIN_RATINGS_FOR_SAMPLE }
                    sort(Game.field.total_rating_count, SortOrder.DESC)
                    limit(DEFAULT_OPTIONS_SAMPLE_SIZE)
                }
            }
            if (result.isErr) return@launch

            val games = result.unwrap().games
            val options = mapOf(
                FilterSearchTarget.COMPANY to games.rankBy { game ->
                    game.involved_companies.mapNotNull { it.company }
                        .map { NamedSearchResult(it.id.toInt(), it.name, it.logo?.image_id) }
                },
                FilterSearchTarget.FRANCHISE to games.rankBy { game ->
                    game.franchises.map { NamedSearchResult(it.id.toInt(), it.name) }
                },
                FilterSearchTarget.ENGINE to games.rankBy { game ->
                    game.game_engines.map { NamedSearchResult(it.id.toInt(), it.name, it.logo?.image_id) }
                },
            )
            update {
                copy(
                    defaultOptions = options,
                    knownOptions = options.entries.fold(knownOptions) { known, (target, list) ->
                        known.withOptions(target, list)
                    }
                )
            }
        }
    }

    /** Ranks the facet values extracted by [extract] by how many sampled games mention them. */
    private fun List<Game>.rankBy(extract: (Game) -> List<NamedSearchResult>): List<NamedSearchResult> =
        flatMap(extract)
            .groupingBy { it.id }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(DEFAULT_OPTIONS_PER_TARGET)
            .mapNotNull { entry -> firstNotNullOfOrNull { game -> extract(game).firstOrNull { it.id == entry.key } } }

    /**
     * Applies the query typed in the search bar after [DEBOUNCE_MILLIS], so a burst of keystrokes
     * costs one IGDB request instead of one per character. Use [submitSearchQuery] to skip the wait.
     */
    fun setSearchQuery(query: String) {
        searchJob?.cancel()
        if (state.filterState.searchQuery == query) return
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MILLIS.milliseconds)
            updateFilter(state.filterState.copy(searchQuery = query))
        }
    }

    /** Applies the query right away, for the deliberate gestures: the IME search action and clear. */
    fun submitSearchQuery(query: String) {
        searchJob?.cancel()
        if (state.filterState.searchQuery == query) return
        updateFilter(state.filterState.copy(searchQuery = query))
    }

    fun updateFilter(newFilterState: GameListFilterState) {
        update { copy(filterState = newFilterState) }
        load(reset = true)
    }

    /** Clears every filter but the query: that one has its own affordance, the search bar. */
    fun resetFilter() {
        update {
            copy(
                filterState = GameListFilterState(searchQuery = filterState.searchQuery),
                filterSearches = emptyMap()
            )
        }
        load(reset = true)
    }

    /**
     * Debounced lookup of the options of a filter section.
     *
     * These endpoints are **not** searchable on IGDB (only Characters, Collections, Games,
     * Platforms and Themes are), so the query is a case-insensitive `name` match instead.
     */
    fun searchFilterOptions(target: FilterSearchTarget, query: String) {
        update { copy(filterSearches = filterSearches.with(target) { copy(query = query) }) }
        filterSearchJobs.remove(target)?.cancel()
        if (query.isBlank()) {
            update {
                copy(filterSearches = filterSearches.with(target) { copy(results = emptyList(), loading = false) })
            }
            return
        }

        filterSearchJobs[target] = viewModelScope.launch {
            delay(DEBOUNCE_MILLIS.milliseconds)
            update { copy(filterSearches = filterSearches.with(target) { copy(loading = true) }) }
            val results = fetchFilterOptions(target, query)
            update {
                copy(
                    filterSearches = filterSearches.with(target) { copy(results = results, loading = false) },
                    knownOptions = knownOptions.withOptions(target, results)
                )
            }
        }
    }

    fun onEndReached(lastVisibleIndex: Int) {
        if (state.loading || state.endReached) return
        if (lastVisibleIndex >= state.games.lastIndex - PREFETCH_THRESHOLD) {
            load(reset = false)
        }
    }

    private fun Map<FilterSearchTarget, FilterSearchState>.with(
        target: FilterSearchTarget,
        reducer: FilterSearchState.() -> FilterSearchState
    ) = this + (target to (this[target] ?: FilterSearchState()).reducer())

    private fun Map<FilterSearchTarget, Map<Int, NamedSearchResult>>.withOptions(
        target: FilterSearchTarget,
        options: List<NamedSearchResult>
    ) = this + (target to (this[target].orEmpty() + options.associateBy(NamedSearchResult::id)))

    private suspend fun fetchFilterOptions(target: FilterSearchTarget, query: String): List<NamedSearchResult> =
        when (target) {
            FilterSearchTarget.COMPANY -> named({
                igdb.getCompanies {
                    fields(Company.field.name, Company.field.logo.image_id)
                    where { Company.field.name contains query }
                    limit(FILTER_SEARCH_LIMIT)
                }
            }) { result -> result.companies.map { NamedSearchResult(it.id.toInt(), it.name, it.logo?.image_id) } }

            FilterSearchTarget.FRANCHISE -> named({
                igdb.getFranchises {
                    fields(Franchise.field.name)
                    where { Franchise.field.name contains query }
                    limit(FILTER_SEARCH_LIMIT)
                }
            }) { result -> result.franchises.map { NamedSearchResult(it.id.toInt(), it.name) } }

            FilterSearchTarget.ENGINE -> named({
                igdb.getGameEngines {
                    fields(GameEngine.field.name, GameEngine.field.logo.image_id)
                    where { GameEngine.field.name contains query }
                    limit(FILTER_SEARCH_LIMIT)
                }
            }) { result -> result.gameengines.map { NamedSearchResult(it.id.toInt(), it.name, it.logo?.image_id) } }

            FilterSearchTarget.KEYWORD -> named({
                igdb.getKeywords {
                    fields(Keyword.field.name)
                    where { Keyword.field.name contains query }
                    limit(FILTER_SEARCH_LIMIT)
                }
            }) { result -> result.keywords.map { NamedSearchResult(it.id.toInt(), it.name) } }
        }

    private suspend fun <T> named(
        request: suspend () -> T,
        map: (T) -> List<NamedSearchResult>
    ): List<NamedSearchResult> {
        val result = safeRequest(request)
        return if (result.isOk) map(result.unwrap()) else emptyList()
    }

    /**
     * Loads a page, replacing any load still in flight.
     *
     * Filters can change faster than IGDB answers, and a stale response would append games for the
     * previous filter and clobber [offset] and `endReached`, so the pending load is cancelled first.
     */
    private fun load(reset: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadPage(reset) }
    }

    private suspend fun loadPage(reset: Boolean) {
        if (reset) {
            offset = 0
            // The library filters are applied by the backend, so a new filter needs a new fetch.
            libraryGameIds = null
            update { copy(games = emptyList(), endReached = false) }
        }
        if (state.endReached) return

        update { copy(loading = true) }
        // A page the other filters empty entirely changes nothing on screen, so the grid would
        // never ask for the next one: keep going until something shows up or the source ends.
        var page: Page
        do {
            page = fetchPage(offset)
            if (page.sourceFull) offset += PAGE_SIZE
        } while (page.games.isEmpty() && page.sourceFull)
        update {
            copy(
                games = games + page.games,
                loading = false,
                endReached = !page.sourceFull,
            )
        }
    }

    /**
     * One page of results.
     *
     * [sourceFull] tracks whether the query that *drives pagination* returned a full page, which is
     * not the same as [games] being full: when an id-source endpoint (popularity or time to beat)
     * feeds the games query, the other filters shrink the page afterward. Deriving "end reached"
     * from [games] would stop pagination on the first partially filtered page.
     */
    private data class Page(val games: List<Game>, val sourceFull: Boolean)

    @Suppress("ReturnCount")
    private suspend fun fetchPage(offset: Int): Page {
        val filter = state.filterState
        val isCustomFilterActive = filter.isActive
        val gameIds = fetchSourceGameIds(filter, offset)
        val sourceFull = gameIds?.let { it.size >= PAGE_SIZE }
        if (gameIds != null && gameIds.isEmpty()) return Page(emptyList(), sourceFull = false)

        val result = safeRequest {
            igdb.getGames {
                fields(
                    Game.field.name,
                    Game.field.cover.image_id,
                    Game.field.first_release_date,
                    Game.field.rating,
                    Game.field.aggregated_rating
                )

                if (filter.searchQuery.isNotBlank()) {
                    search(filter.searchQuery)
                }

                where {
                    if (gameIds != null) {
                        Game.field.id inAny gameIds.map(Int::toString)
                    }
                    applyFilters(filter)
                }

                // With an id source the page is already chosen upstream: sorting and offsetting
                // here would reshuffle and skip within that page.
                if (gameIds == null) {
                    // The section's query only runs with no custom filter, when the clause above is
                    // empty and emits nothing: the library's `where` replaces instead of appending.
                    if (isCustomFilterActive) applySort(filter) else section?.baseQuery?.invoke(this)
                    offset(offset)
                }
                limit(PAGE_SIZE)
            }
        }
        // A failed request ends the list like an exhausted source, so the caller never retries it.
        if (result.isErr) return Page(emptyList(), sourceFull = false)
        val games = result.unwrap().games
        return Page(gameIds?.let(games::sortedByIds) ?: games, sourceFull = sourceFull ?: (games.size >= PAGE_SIZE))
    }

    /** One page of the ids that drive pagination, or null when the games query paginates itself. */
    private suspend fun fetchSourceGameIds(filter: GameListFilterState, offset: Int): List<Int>? {
        // Time to beat lives on its own endpoint keyed by game_id, so when it is filtered, it takes
        // over pagination from the section's popularity query — the two cannot both drive it.
        val popscoreSection = section?.takeIf { !filter.isActive && it.popscoreQuery != null }
        // In event or library scope the scope's own game ids drive pagination and win over the
        // other id sources, so the time-to-beat filter is inert there.
        return when {
            eventId != null -> eventGameIdPage(eventId, offset)
            libraryStatus != null -> libraryGameIdPage(libraryStatus, offset)
            filter.hasTimeToBeatFilter -> fetchTimeToBeatGameIds(filter, offset)
            popscoreSection != null -> fetchPopScoreGameIds(popscoreSection, offset)
            else -> null
        }
    }

    /**
     * One page of the event's game ids.
     *
     * IGDB returns the whole `games` array on the event (verified up to ~500 entries), so the ids
     * are fetched once — together with the details the header shows — and paged client-side.
     * ponytail: no cap handling — if an event ever exceeds what IGDB expands, the tail is missing.
     */
    private suspend fun eventGameIdPage(eventId: Int, offset: Int): List<Int> {
        val ids = eventGameIds ?: fetchEvent(eventId).also { eventGameIds = it }
        return ids.drop(offset).take(PAGE_SIZE)
    }

    /** Loads the event, publishes it for the header and returns its game ids. */
    private suspend fun fetchEvent(eventId: Int): List<Int> {
        val result = safeRequest {
            igdb.getEvents {
                fields(
                    Event.field.name,
                    Event.field.description,
                    Event.field.start_time,
                    Event.field.end_time,
                    Event.field.time_zone,
                    Event.field.live_stream_url,
                    Event.field.event_logo.image_id,
                    Event.field.event_networks.url,
                    Event.field.event_networks.network_type.name,
                    Event.field.games.id,
                )
                where { Event.field.id equalTo eventId.toString() }
            }
        }
        val event = result.get()?.events?.firstOrNull() ?: return emptyList()
        update { copy(event = event) }
        return event.games.map { it.id.toInt() }
    }

    /**
     * One page of the library status' game ids, in the order of the library sort.
     *
     * The backend pages differently from IGDB, so — as for events — every entry matching the
     * library filters is fetched once and paged client-side.
     */
    private suspend fun libraryGameIdPage(status: GameLibraryStatus, offset: Int): List<Int> {
        val ids = libraryGameIds ?: fetchLibraryEntries(status, state.filterState.library).also { libraryGameIds = it }
        return ids.drop(offset).take(PAGE_SIZE)
    }

    /** Loads the user's entries in [status], publishes them for the card badges and returns their game ids. */
    private suspend fun fetchLibraryEntries(status: GameLibraryStatus, filter: LibraryFilterState): List<Int> {
        val entries = safeRequest { LibraryEntry.currentUserEntries(status).applyFilter(filter).allPages() }
            .get()
            .orEmpty()
        update { copy(libraryEntries = entries.associateBy { it.gameId.toLong() }) }
        return entries.map { it.gameId }
    }

    private suspend fun fetchTimeToBeatGameIds(filter: GameListFilterState, offset: Int): List<Int> {
        val result = safeRequest {
            igdb.getGameTimeToBeat {
                fields(GameTimeToBeat.field.game_id)
                where {
                    if (filter.minHoursToBeat > 0f) {
                        GameTimeToBeat.field.normally greaterThanOrEqual (filter.minHoursToBeat * SECONDS_PER_HOUR).toLong()
                    }
                    if (filter.maxHoursToBeat < MaxHoursToBeat) {
                        GameTimeToBeat.field.normally lessThanOrEqual (filter.maxHoursToBeat * SECONDS_PER_HOUR).toLong()
                    }
                }
                // `count` is how many players submitted a time, so the best-attested entries come first.
                sort(GameTimeToBeat.field.count, SortOrder.DESC)
                limit(PAGE_SIZE)
                offset(offset)
            }
        }
        return if (result.isOk) result.unwrap().gametimetobeats.map { it.game_id } else emptyList()
    }

    private suspend fun fetchPopScoreGameIds(section: DiscoverSection, offset: Int): List<Int> {
        val result = safeRequest {
            igdb.multiquery {
                query(IgdbEndpoint.POPULARITY_PRIMITIVE, section.name) {
                    fields(PopularityPrimitive.field.game_id)
                    section.popscoreQuery?.invoke(this)
                    limit(PAGE_SIZE)
                    offset(offset)
                }
            }
        }
        if (result.isErr) return emptyList()
        return result.unwrap().multiqueryResults<PopularityPrimitive>(section.name).map { it.game_id }
    }
}

/** The `where` clauses of every filter the user can set, ordered as they appear in the filter sheet. */
@Suppress("CyclomaticComplexMethod")
private fun ApicalypseQueryBuilderWhereBuilder.applyFilters(filter: GameListFilterState) {
    if (filter.minUserRating > 0f) {
        Game.field.rating greaterThanOrEqual filter.minUserRating.toDouble()
    }
    if (filter.maxUserRating < MaxRating) {
        Game.field.rating lessThanOrEqual filter.maxUserRating.toDouble()
    }

    if (filter.minCriticsRating > 0f) {
        Game.field.aggregated_rating greaterThanOrEqual filter.minCriticsRating.toDouble()
    }
    if (filter.maxCriticsRating < MaxRating) {
        Game.field.aggregated_rating lessThanOrEqual filter.maxCriticsRating.toDouble()
    }

    when (filter.releaseStatus) {
        ReleaseStatusFilter.RELEASED -> alreadyReleased()
        ReleaseStatusFilter.UPCOMING -> notYetReleased()
        ReleaseStatusFilter.ALL -> {}
    }

    if (filter.minReleaseYear > MinReleaseYear) {
        val startEpoch = LocalDateTime(filter.minReleaseYear, 1, 1, 0, 0).toInstant(TimeZone.UTC).epochSeconds
        Game.field.first_release_date greaterThanOrEqual startEpoch
    }
    if (filter.maxReleaseYear < MaxReleaseYear) {
        // Last second of the year: the start of the next one, minus one.
        val endEpoch = LocalDateTime(filter.maxReleaseYear + 1, 1, 1, 0, 0).toInstant(TimeZone.UTC).epochSeconds - 1
        Game.field.first_release_date lessThanOrEqual endEpoch
    }

    anyOf(Game.field.platforms, filter.platformIds)
    anyOf(Game.field.player_perspectives, filter.playerPerspectiveIds)
    anyOf(Game.field.category, filter.categoryIds)
    anyOf(Game.field.status, filter.statusIds)
    anyOf(Game.field.genres, filter.genreIds)
    anyOf(Game.field.themes, filter.themeIds)
    anyOf(Game.field.game_modes, filter.gameModeIds)
    anyOf(Game.field.franchises, filter.franchiseIds)
    anyOf(Game.field.game_engines, filter.gameEngineIds)
    anyOf(Game.field.keywords, filter.keywordIds)

    filter.companiesClause()?.let { raw(it) }
}

/** `inAny` over an id set, skipped when the set is empty (an empty `= ()` is an IGDB syntax error). */
private fun ApicalypseQueryBuilderWhereBuilder.anyOf(field: IgdbRequestField<*>, ids: Set<Int>) {
    if (ids.isNotEmpty()) field inAny ids.map(Int::toString)
}

private fun ApicalypseQueryBuilderWhereBuilder.anyOf(field: IgdbRequestFieldDsl<*, *>, ids: Set<Int>) {
    if (ids.isNotEmpty()) field inAny ids.map(Int::toString)
}

/** Sort clause of a custom filter; popularity falls back to the user rating outside a section's query. */
private fun ApicalypseQueryBuilder.applySort(filter: GameListFilterState) {
    val order = if (filter.sortDirection == SortDirection.DESC) SortOrder.DESC else SortOrder.ASC
    // IGDB rejects a query carrying both `search` and `sort`: search results are relevancy-ordered.
    when (if (filter.searchQuery.isNotBlank()) null else filter.sortField) {
        null -> {}
        SortField.USER_RATING -> sort(Game.field.rating, order)
        SortField.CRITICS_RATING -> sort(Game.field.aggregated_rating, order)
        SortField.RELEASE_DATE -> sort(Game.field.first_release_date, order)
        SortField.NAME -> sort(Game.field.name, order)
        SortField.POPULARITY -> sort(Game.field.rating, order)
    }
}

internal val DefaultFilterState = GameListFilterState()

/**
 * True when anything beyond the section's own default query is set.
 *
 * Compared against the default instance rather than field by field: a new filter is covered the
 * moment it is added to [GameListFilterState], with no second list to keep in sync.
 */
val GameListFilterState.isActive: Boolean
    get() = this != DefaultFilterState

/**
 * True when a filter other than the search bar query is set.
 *
 * The query has its own visible affordance, so it must not light up the filter button.
 */
val GameListFilterState.hasActiveFilters: Boolean
    get() = copy(searchQuery = "").isActive

/** True when the time-to-beat range is narrower than the full slider span. */
val GameListFilterState.hasTimeToBeatFilter: Boolean
    get() = minHoursToBeat > 0f || maxHoursToBeat < MaxHoursToBeat

/**
 * The `involved_companies` clause, or null when no company is selected.
 *
 * Each company is its own-parenthesised group, so its roles apply to that company alone; the groups
 * are OR-ed together, as are the roles inside a group. A company with no role matches any role.
 */
private fun GameListFilterState.companiesClause(): String? = companyIds
    .takeIf { it.isNotEmpty() }
    ?.joinToString(" | ") { companyId ->
        val roles = companyRoles[companyId].orEmpty()
        if (roles.isEmpty()) {
            "involved_companies.company = $companyId"
        } else {
            val roleClause = roles.joinToString(" | ") { "involved_companies.${it.igdbField} = true" }
            "(involved_companies.company = $companyId & ($roleClause))"
        }
    }
    ?.let { if (companyIds.size > 1) "($it)" else it }
