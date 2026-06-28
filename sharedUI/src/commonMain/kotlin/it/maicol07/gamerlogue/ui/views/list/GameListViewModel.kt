package it.maicol07.gamerlogue.ui.views.list

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.apicalypse.ApicalypseQueryBuilder
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.getGames
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.PopularityPrimitive
import at.released.igdbclient.model.UnpackedMultiQueryResult
import at.released.igdbclient.multiquery
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.extensions.update
import it.maicol07.gamerlogue.extensions.where
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

/**
 * Paginates the full list of games for a [DiscoverSection], reusing the same query that powers
 * the Discover carousel. Sections backed by a popularity-score query paginate through the
 * primitive endpoint; the others paginate the games endpoint directly.
 */
@KoinViewModel
class GameListViewModel(
    @InjectedParam private val section: DiscoverSection,
) : StateViewModel<GameListViewModel.UiState>(UiState()) {
    data class UiState(
        var games: List<Game> = emptyList(),
        var loading: Boolean = false,
        var endReached: Boolean = false,
    )

    private companion object {
        const val PageSize = 50
        const val PrefetchThreshold = 6
    }

    private val igdb: IgdbClient by inject()

    private var offset = 0

    init {
        load(reset = true)
    }

    fun onEndReached(lastVisibleIndex: Int) {
        if (state.loading || state.endReached) return
        if (lastVisibleIndex >= state.games.lastIndex - PrefetchThreshold) {
            load(reset = false)
        }
    }

    private fun load(reset: Boolean) = viewModelScope.launch {
        if (reset) {
            offset = 0
            uiState.update { games = emptyList(); endReached = false }
        }
        if (state.endReached) return@launch

        uiState.update { loading = true }
        val added = fetchPage(offset)
        uiState.update {
            games = games + added
            loading = false
            endReached = added.size < PageSize
        }
        if (added.size >= PageSize) offset += PageSize
    }

    private suspend fun fetchPage(offset: Int): List<Game> {
        val popscoreQuery = section.popscoreQuery
        val gameIds = if (popscoreQuery != null) {
            fetchPopScoreGameIds(popscoreQuery, offset).ifEmpty { return emptyList() }
        } else {
            null
        }

        val result = safeRequest {
            igdb.getGames {
                fields(Game.field.name, Game.field.cover.image_id, Game.field.first_release_date)
                if (gameIds != null) {
                    where { Game.field.id inAny gameIds.map(Int::toString) }
                } else {
                    section.baseQuery(this)
                    offset(offset)
                }
                limit(PageSize)
            }
        }
        return if (result.isOk) result.unwrap().games else emptyList()
    }

    private suspend fun fetchPopScoreGameIds(
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
