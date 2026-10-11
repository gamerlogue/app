package it.maicol07.gamerlogue.ui.views.list

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.apicalypse.SortOrder
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.getCompanies
import at.released.igdbclient.getFranchises
import at.released.igdbclient.getGameEngines
import at.released.igdbclient.getGames
import at.released.igdbclient.getKeywords
import at.released.igdbclient.model.Company
import at.released.igdbclient.model.Franchise
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameEngine
import at.released.igdbclient.model.Keyword
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.safeRequest
import it.maicol07.gamerlogue.extensions.sort
import it.maicol07.gamerlogue.extensions.where
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * The options of the filter sheet's searchable sections, shared by every screen hosting the sheet.
 * Its jobs run in [scope], the host view model's.
 */
class FilterOptions(
    private val scope: CoroutineScope,
    knownOptions: Map<FilterSearchTarget, Map<Int, NamedSearchResult>>,
) : KoinComponent {
    data class State(
        val filterSearches: Map<FilterSearchTarget, FilterSearchState> = emptyMap(),
        /** Options shown by a searchable filter section before the user types anything. */
        val defaultOptions: Map<FilterSearchTarget, List<NamedSearchResult>> = emptyMap(),
        /**
         * Every option ever shown by a searchable section, by id: a selection keeps its name after
         * the query moves on and while the sheet is closed, since the filter state only stores ids.
         */
        val knownOptions: Map<FilterSearchTarget, Map<Int, NamedSearchResult>> = emptyMap(),
    )

    private companion object {
        const val FILTER_SEARCH_LIMIT = 10
        const val DEBOUNCE_MILLIS = 300L
        const val DEFAULT_OPTIONS_SAMPLE_SIZE = 60
        const val DEFAULT_OPTIONS_PER_TARGET = 12
        const val MIN_RATINGS_FOR_SAMPLE = 300
    }

    val state: StateFlow<State>
        field = MutableStateFlow(State(knownOptions = knownOptions))

    private val igdb: IgdbClient by inject()
    private val exceptionReporter: ExceptionReporter by inject()
    private val filterSearchJobs = mutableMapOf<FilterSearchTarget, Job>()
    private var defaultOptionsJob: Job? = null

    private fun update(reducer: State.() -> State) = state.update { it.reducer() }

    /** Forgets the typed queries and their results; the known names stay, for the chips. */
    fun clearSearches() = update { copy(filterSearches = emptyMap()) }

    /**
     * Populates the empty-query options of the searchable filter sections.
     *
     * IGDB exposes no popularity metric on companies, franchises, engines or keywords, so the
     * options are derived: sample the most-rated games and rank each facet by how often it occurs.
     * That keeps names and logos authoritative instead of hardcoding ids that drift (two of the
     * previously hardcoded company ids pointed at the wrong companies entirely).
     */
    fun loadDefaults() {
        if (state.value.defaultOptions.isNotEmpty() || defaultOptionsJob?.isActive == true) return
        defaultOptionsJob = scope.launch {
            val result = exceptionReporter.safeRequest {
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
     * Debounced lookup of the options of a filter section.
     *
     * These endpoints are **not** searchable on IGDB (only Characters, Collections, Games,
     * Platforms and Themes are), so the query is a case-insensitive `name` match instead.
     */
    fun search(target: FilterSearchTarget, query: String) {
        update { copy(filterSearches = filterSearches.with(target) { copy(query = query) }) }
        filterSearchJobs.remove(target)?.cancel()
        if (query.isBlank()) {
            update {
                copy(filterSearches = filterSearches.with(target) { copy(results = emptyList(), loading = false) })
            }
            return
        }

        filterSearchJobs[target] = scope.launch {
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
        val result = exceptionReporter.safeRequest(request)
        return if (result.isOk) map(result.unwrap()) else emptyList()
    }
}
