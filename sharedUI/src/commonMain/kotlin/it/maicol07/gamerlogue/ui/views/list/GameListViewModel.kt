package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.apicalypse.SortOrder
import at.released.igdbclient.model.Game
import at.released.igdbclient.multiquery
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.safeRequest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class GameListViewModel(
    private val type: GameListType,
) : ViewModel(), KoinComponent {
    companion object {
        private const val YEAR_SECONDS = 31_556_926
        private const val LIMIT = 50
        private const val MILLIS_IN_SECOND = 1000
        private const val PREFETCH_THRESHOLD = 6
        private const val SECONDS_IN_DAY = 86_400
    }
    private val igdb: IgdbClient by inject()

    enum class GameSort { RatingDesc, ReleaseDateAsc, NameAsc }

    // Stato pubblico
    val games = mutableStateListOf<Game>()
    val isLoading = mutableStateOf(false)
    val endReached = mutableStateOf(false)
    val sort = mutableStateOf(
        when (type) {
            GameListType.Popular -> GameSort.RatingDesc
            GameListType.Upcoming -> GameSort.ReleaseDateAsc
        }
    )

    // Per Upcoming: null = tutti i futuri, oppure finestre comode
    val timeframeDays = mutableStateOf<Int?>(null)

    // Stato interno
    private var offset = 0

    init { load(reset = true) }

    fun setSort(newSort: GameSort) {
        if (sort.value == newSort) return
        sort.value = newSort
        load(reset = true)
    }

    fun setTimeframe(days: Int?) {
        if (timeframeDays.value == days) return
        timeframeDays.value = days
        load(reset = true)
    }

    fun onEndReached(lastVisibleIndex: Int) {
        if (isLoading.value || endReached.value) return
        if (lastVisibleIndex >= games.lastIndex - PREFETCH_THRESHOLD) {
            load(reset = false)
        }
    }

    private fun load(reset: Boolean) = viewModelScope.launch {
        if (reset) {
            offset = 0
            endReached.value = false
            games.clear()
        }
        if (endReached.value) return@launch

        isLoading.value = true
        val result = safeRequest {
            when (type) {
                GameListType.Popular -> queryPopular(offset, LIMIT, sort.value)
                GameListType.Upcoming -> queryUpcoming(offset, LIMIT, sort.value, timeframeDays.value)
            }
        }
        if (result.isOk) {
            val response = result.unwrap()

            @Suppress("UNCHECKED_CAST")
            val list = response.firstOrNull()?.results as? List<Game>
            val added = list.orEmpty()
            games.addAll(added)
            if (added.size < LIMIT) endReached.value = true else offset += LIMIT
        }
        isLoading.value = false
    }

    @OptIn(ExperimentalTime::class)
    @Suppress("NewApi")
    private fun nowSeconds(): Long = Clock.System.now().toEpochMilliseconds() / MILLIS_IN_SECOND

    private suspend fun queryPopular(
        offset: Int,
        limit: Int,
        sort: GameSort
    ) = igdb.multiquery {
        val now = nowSeconds()
        query(IgdbEndpoint.GAME, "Popular list") {
            fields("name", "cover.image_id", "first_release_date")
            // finestra: ultimo anno
            where(
                "parent_game = null & follows > 5 & first_release_date < " + now +
                    " & first_release_date > " + (now - YEAR_SECONDS)
            )
            when (sort) {
                GameSort.RatingDesc -> sort("rating", SortOrder.DESC)
                GameSort.ReleaseDateAsc -> sort("first_release_date", SortOrder.ASC)
                GameSort.NameAsc -> sort("name", SortOrder.ASC)
            }
            limit(limit)
            offset(offset)
        }
    }

    private suspend fun queryUpcoming(
        offset: Int,
        limit: Int,
        sort: GameSort,
        timeframeDays: Int?
    ) = igdb.multiquery {
        val now = nowSeconds()
        val upperBound = timeframeDays?.let { now + it * SECONDS_IN_DAY }
        query(IgdbEndpoint.GAME, "Upcoming list") {
            fields("name", "cover.image_id", "first_release_date")
            val whereBase = StringBuilder("parent_game = null & first_release_date > ").append(now)
            if (upperBound != null) whereBase.append(" & first_release_date < ").append(upperBound)
            where(whereBase.toString())
            when (sort) {
                GameSort.RatingDesc -> sort("rating", SortOrder.DESC)
                GameSort.ReleaseDateAsc -> sort("first_release_date", SortOrder.ASC)
                GameSort.NameAsc -> sort("name", SortOrder.ASC)
            }
            limit(limit)
            offset(offset)
        }
    }
}
