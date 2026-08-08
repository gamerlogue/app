package it.maicol07.gamerlogue.ui.views.list

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.apicalypse.ApicalypseQueryBuilder
import at.released.igdbclient.apicalypse.SortOrder
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.getCompanies
import at.released.igdbclient.getFranchises
import at.released.igdbclient.getGameEngines
import at.released.igdbclient.getGames
import at.released.igdbclient.getGameTimeToBeat
import at.released.igdbclient.getKeywords
import at.released.igdbclient.model.Company
import at.released.igdbclient.model.Franchise
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameEngine
import at.released.igdbclient.model.GameTimeToBeat
import at.released.igdbclient.model.Keyword
import at.released.igdbclient.model.PopularityPrimitive
import at.released.igdbclient.model.UnpackedMultiQueryResult
import at.released.igdbclient.multiquery
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.extensions.alreadyReleased
import it.maicol07.gamerlogue.extensions.notYetReleased
import it.maicol07.gamerlogue.extensions.sort
import it.maicol07.gamerlogue.extensions.where
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

/** Selectable range of grid columns, shared by the view model and the filter sheet's slider. */
const val MinColumns = 1
const val MaxColumns = 10

/** Upper bound of the "time to beat" slider, in hours; at the maximum the filter is off. */
const val MaxHoursToBeat = 100f

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
    val maxUserRating: Float = 100f,
    val minCriticsRating: Float = 0f,
    val maxCriticsRating: Float = 100f,
    val minReleaseYear: Int = 1970,
    val maxReleaseYear: Int = 2026,
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
)

/**
 * Backs the search bar's expanded pane: a paginated, filterable game grid.
 *
 * With no filter and a [UiState.section] set it replays that Discover carousel's query so
 * "see all" paginates exactly what the carousel previewed; as soon as any filter or query is
 * applied it switches to a plain filtered games query.
 */
@KoinViewModel
class GameListViewModel : StateViewModel<GameListViewModel.UiState>(UiState()) {
    /** Immutable state of the search results pane. */
    data class UiState(
        val section: DiscoverSection? = null,
        val games: List<Game> = emptyList(),
        val loading: Boolean = false,
        val endReached: Boolean = false,
        val columnCount: Int = 3,
        val filterState: GameListFilterState = GameListFilterState(),
        val showFilterSheet: Boolean = false,
        val filterSearches: Map<FilterSearchTarget, FilterSearchState> = emptyMap(),
        /** Options shown by a searchable filter section before the user types anything. */
        val defaultOptions: Map<FilterSearchTarget, List<NamedSearchResult>> = emptyMap(),
    )

    private companion object {
        const val PageSize = 50
        const val PrefetchThreshold = 6
        const val FilterSearchLimit = 10
        const val DebounceMillis = 300L
        const val SecondsPerHour = 3600
        const val DefaultOptionsSampleSize = 60
        const val DefaultOptionsPerTarget = 12
        const val MinRatingsForSample = 300
    }

    private val igdb: IgdbClient by inject()
    private var offset = 0
    private var started = false
    private val filterSearchJobs = mutableMapOf<FilterSearchTarget, Job>()
    private var defaultOptionsJob: Job? = null
    private var loadJob: Job? = null

    /**
     * Loads the first page, optionally scoped to a Discover section so "see all" paginates exactly
     * the query that carousel previewed. Idempotent: the destination re-runs it on every recomposition
     * after a configuration change or a trip to the game detail, which must not reset the list.
     */
    fun start(section: DiscoverSection?) {
        if (started) return
        started = true
        update { copy(section = section) }
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
                    where { Game.field.total_rating_count greaterThan MinRatingsForSample }
                    sort(Game.field.total_rating_count, SortOrder.DESC)
                    limit(DefaultOptionsSampleSize)
                }
            }
            if (result.isErr) return@launch

            val games = result.unwrap().games
            update {
                copy(
                    defaultOptions = mapOf(
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
            .take(DefaultOptionsPerTarget)
            .mapNotNull { entry -> firstNotNullOfOrNull { game -> extract(game).firstOrNull { it.id == entry.key } } }

    /** Applies the query typed in the search bar; no-op when it did not change. */
    fun setSearchQuery(query: String) {
        if (state.filterState.searchQuery == query) return
        updateFilter(state.filterState.copy(searchQuery = query))
    }

    fun updateFilter(newFilterState: GameListFilterState) {
        update { copy(filterState = newFilterState) }
        load(reset = true)
    }

    fun resetFilter() {
        update { copy(filterState = GameListFilterState(), filterSearches = emptyMap()) }
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
            update { copy(filterSearches = filterSearches.with(target) { copy(results = emptyList(), loading = false) }) }
            return
        }

        filterSearchJobs[target] = viewModelScope.launch {
            delay(DebounceMillis)
            update { copy(filterSearches = filterSearches.with(target) { copy(loading = true) }) }
            val results = fetchFilterOptions(target, query)
            update { copy(filterSearches = filterSearches.with(target) { copy(results = results, loading = false) }) }
        }
    }

    fun onEndReached(lastVisibleIndex: Int) {
        if (state.loading || state.endReached) return
        if (lastVisibleIndex >= state.games.lastIndex - PrefetchThreshold) {
            load(reset = false)
        }
    }

    private fun Map<FilterSearchTarget, FilterSearchState>.with(
        target: FilterSearchTarget,
        reducer: FilterSearchState.() -> FilterSearchState
    ) = this + (target to (this[target] ?: FilterSearchState()).reducer())

    private suspend fun fetchFilterOptions(target: FilterSearchTarget, query: String): List<NamedSearchResult> =
        when (target) {
            FilterSearchTarget.COMPANY -> named({
                igdb.getCompanies {
                    fields(Company.field.name, Company.field.logo.image_id)
                    where { Company.field.name contains query }
                    limit(FilterSearchLimit)
                }
            }) { result -> result.companies.map { NamedSearchResult(it.id.toInt(), it.name, it.logo?.image_id) } }

            FilterSearchTarget.FRANCHISE -> named({
                igdb.getFranchises {
                    fields(Franchise.field.name)
                    where { Franchise.field.name contains query }
                    limit(FilterSearchLimit)
                }
            }) { result -> result.franchises.map { NamedSearchResult(it.id.toInt(), it.name) } }

            FilterSearchTarget.ENGINE -> named({
                igdb.getGameEngines {
                    fields(GameEngine.field.name, GameEngine.field.logo.image_id)
                    where { GameEngine.field.name contains query }
                    limit(FilterSearchLimit)
                }
            }) { result -> result.gameengines.map { NamedSearchResult(it.id.toInt(), it.name, it.logo?.image_id) } }

            FilterSearchTarget.KEYWORD -> named({
                igdb.getKeywords {
                    fields(Keyword.field.name)
                    where { Keyword.field.name contains query }
                    limit(FilterSearchLimit)
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
            update { copy(games = emptyList(), endReached = false) }
        }
        if (state.endReached) return

        update { copy(loading = true) }
        val page = fetchPage(offset)
        update {
            copy(
                games = games + page.games,
                loading = false,
                endReached = !page.sourceFull,
            )
        }
        if (page.sourceFull) offset += PageSize
    }

    /**
     * One page of results.
     *
     * [sourceFull] tracks whether the query that *drives pagination* returned a full page, which is
     * not the same as [games] being full: when an id-source endpoint (popularity or time to beat)
     * feeds the games query, the other filters shrink the page afterwards. Deriving "end reached"
     * from [games] would stop pagination on the first partially-filtered page.
     */
    private data class Page(val games: List<Game>, val sourceFull: Boolean)

    @Suppress("CyclomaticComplexMethod", "LongMethod", "ReturnCount")
    private suspend fun fetchPage(offset: Int): Page {
        val filter = state.filterState
        val section = state.section
        val isCustomFilterActive = filter.isActive

        // Time to beat lives on its own endpoint keyed by game_id, so when it is filtered it takes
        // over pagination from the section's popularity query — the two cannot both drive it.
        val popscoreQuery = if (!isCustomFilterActive) section?.popscoreQuery else null
        val gameIds = when {
            filter.hasTimeToBeatFilter -> fetchTimeToBeatGameIds(filter, offset)
            popscoreQuery != null -> fetchPopScoreGameIds(section!!, popscoreQuery, offset)
            else -> null
        }
        val sourceFull = gameIds?.let { it.size >= PageSize }
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
                    if (!isCustomFilterActive) {
                        section?.baseQuery?.invoke(this@getGames)
                    }

                    if (filter.minUserRating > 0f) {
                        Game.field.rating greaterThanOrEqual filter.minUserRating.toDouble()
                    }
                    if (filter.maxUserRating < 100f) {
                        Game.field.rating lessThanOrEqual filter.maxUserRating.toDouble()
                    }

                    if (filter.minCriticsRating > 0f) {
                        Game.field.aggregated_rating greaterThanOrEqual filter.minCriticsRating.toDouble()
                    }
                    if (filter.maxCriticsRating < 100f) {
                        Game.field.aggregated_rating lessThanOrEqual filter.maxCriticsRating.toDouble()
                    }

                    when (filter.releaseStatus) {
                        ReleaseStatusFilter.RELEASED -> alreadyReleased()
                        ReleaseStatusFilter.UPCOMING -> notYetReleased()
                        ReleaseStatusFilter.ALL -> {}
                    }

                    if (filter.minReleaseYear > 1970) {
                        val startEpoch = LocalDateTime(filter.minReleaseYear, 1, 1, 0, 0).toInstant(TimeZone.UTC).epochSeconds
                        Game.field.first_release_date greaterThanOrEqual startEpoch
                    }
                    if (filter.maxReleaseYear < 2026) {
                        val endEpoch = LocalDateTime(filter.maxReleaseYear, 12, 31, 23, 59, 59).toInstant(TimeZone.UTC).epochSeconds
                        Game.field.first_release_date lessThanOrEqual endEpoch
                    }

                    if (filter.platformIds.isNotEmpty()) {
                        Game.field.platforms inAny filter.platformIds.map(Int::toString)
                    }

                    if (filter.playerPerspectiveIds.isNotEmpty()) {
                        Game.field.player_perspectives inAny filter.playerPerspectiveIds.map(Int::toString)
                    }

                    if (filter.categoryIds.isNotEmpty()) {
                        Game.field.category inAny filter.categoryIds.map(Int::toString)
                    }

                    if (filter.statusIds.isNotEmpty()) {
                        Game.field.status inAny filter.statusIds.map(Int::toString)
                    }

                    if (filter.genreIds.isNotEmpty()) {
                        Game.field.genres inAny filter.genreIds.map(Int::toString)
                    }

                    if (filter.themeIds.isNotEmpty()) {
                        Game.field.themes inAny filter.themeIds.map(Int::toString)
                    }

                    if (filter.gameModeIds.isNotEmpty()) {
                        Game.field.game_modes inAny filter.gameModeIds.map(Int::toString)
                    }

                    filter.companiesClause()?.let { raw(it) }

                    if (filter.franchiseIds.isNotEmpty()) {
                        Game.field.franchises inAny filter.franchiseIds.map(Int::toString)
                    }

                    if (filter.gameEngineIds.isNotEmpty()) {
                        Game.field.game_engines inAny filter.gameEngineIds.map(Int::toString)
                    }

                    if (filter.keywordIds.isNotEmpty()) {
                        Game.field.keywords inAny filter.keywordIds.map(Int::toString)
                    }
                }

                // With an id source the page is already chosen upstream: sorting and offsetting
                // here would reshuffle and skip within that page.
                if (gameIds == null) {
                    val order = if (filter.sortDirection == SortDirection.DESC) SortOrder.DESC else SortOrder.ASC
                    // IGDB rejects a query carrying both `search` and `sort`: search results are relevancy-ordered.
                    when (if (filter.searchQuery.isNotBlank()) null else filter.sortField) {
                        null -> {}
                        SortField.USER_RATING -> sort(Game.field.rating, order)
                        SortField.CRITICS_RATING -> sort(Game.field.aggregated_rating, order)
                        SortField.RELEASE_DATE -> sort(Game.field.first_release_date, order)
                        SortField.NAME -> sort(Game.field.name, order)
                        SortField.POPULARITY -> {
                            if (isCustomFilterActive) {
                                sort(Game.field.rating, order)
                            } else {
                                section?.baseQuery?.invoke(this@getGames)
                            }
                        }
                    }

                    offset(offset)
                }
                limit(PageSize)
            }
        }
        val games = if (result.isOk) result.unwrap().games else emptyList()
        return Page(games, sourceFull = sourceFull ?: (games.size >= PageSize))
    }

    private suspend fun fetchTimeToBeatGameIds(filter: GameListFilterState, offset: Int): List<Int> {
        val result = safeRequest {
            igdb.getGameTimeToBeat {
                fields(GameTimeToBeat.field.game_id)
                where {
                    if (filter.minHoursToBeat > 0f) {
                        GameTimeToBeat.field.normally greaterThanOrEqual (filter.minHoursToBeat * SecondsPerHour).toLong()
                    }
                    if (filter.maxHoursToBeat < MaxHoursToBeat) {
                        GameTimeToBeat.field.normally lessThanOrEqual (filter.maxHoursToBeat * SecondsPerHour).toLong()
                    }
                }
                // `count` is how many players submitted a time, so the best-attested entries come first.
                sort(GameTimeToBeat.field.count, SortOrder.DESC)
                limit(PageSize)
                offset(offset)
            }
        }
        return if (result.isOk) result.unwrap().gametimetobeats.map { it.game_id.toInt() } else emptyList()
    }

    private suspend fun fetchPopScoreGameIds(
        section: DiscoverSection,
        popscoreQuery: ApicalypseQueryBuilder.() -> Unit,
        offset: Int,
    ): List<Int> {
        val result = safeRequest {
            igdb.multiquery {
                query(IgdbEndpoint.POPULARITY_PRIMITIVE, section.name) {
                    fields(PopularityPrimitive.field.game_id)
                    popscoreQuery(this)
                    limit(PageSize)
                    offset(offset)
                }
            }
        }
        if (result.isErr) return emptyList()

        @Suppress("UNCHECKED_CAST")
        val responseList = result.unwrap() as? List<UnpackedMultiQueryResult<PopularityPrimitive>>
        return responseList?.firstOrNull()?.results?.map { it.game_id } ?: emptyList()
    }
}

/** True when anything beyond the section's own default query is set. */
val GameListFilterState.isActive: Boolean
    get() = searchQuery.isNotBlank() ||
        sortField != SortField.POPULARITY ||
        sortDirection != SortDirection.DESC ||
        minUserRating > 0f || maxUserRating < 100f ||
        minCriticsRating > 0f || maxCriticsRating < 100f ||
        minReleaseYear > 1970 || maxReleaseYear < 2026 ||
        releaseStatus != ReleaseStatusFilter.ALL ||
        platformIds.isNotEmpty() ||
        playerPerspectiveIds.isNotEmpty() ||
        categoryIds.isNotEmpty() ||
        statusIds.isNotEmpty() ||
        genreIds.isNotEmpty() ||
        themeIds.isNotEmpty() ||
        gameModeIds.isNotEmpty() ||
        companyIds.isNotEmpty() ||
        companyRoles.values.any { it.isNotEmpty() } ||
        franchiseIds.isNotEmpty() ||
        gameEngineIds.isNotEmpty() ||
        keywordIds.isNotEmpty() ||
        hasTimeToBeatFilter

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
 * Each company is its own parenthesised group so its roles apply to that company alone; the groups
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
