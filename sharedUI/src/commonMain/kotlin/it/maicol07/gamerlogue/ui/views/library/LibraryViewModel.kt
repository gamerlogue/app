package it.maicol07.gamerlogue.ui.views.library

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.getGames
import at.released.igdbclient.model.Game
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.currentUserEntries
import it.maicol07.gamerlogue.extensions.forEachPage
import it.maicol07.gamerlogue.extensions.where
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

@KoinViewModel
class LibraryViewModel : StateViewModel<LibraryViewModel.UiState>(UiState()) {
    /** Immutable state of the Library screen. */
    data class UiState(
        val loading: Boolean = false,
        val selectedSection: GameLibraryStatus? = null,
        val games: Map<GameLibraryStatus, Map<Game, LibraryEntry>> =
            GameLibraryStatus.entries.associateWith { emptyMap() },
    )

    private val igdb by inject<IgdbClient>()

    /** Loads every status in one pass; section switching then filters the already-loaded state. */
    fun loadLibraryEntries() = viewModelScope.launch {
        update {
            copy(loading = true, games = GameLibraryStatus.entries.associateWith { emptyMap() })
        }

        val result = safeRequest {
            LibraryEntry.currentUserEntries().forEachPage { page ->
                val grouped = groupEntriesByStatus(page)
                update { copy(games = mergeGames(games, grouped)) }
            }
        }
        if (result.isErr) Logger.e(result.unwrapError()) { "Error loading library entries" }
        update { copy(loading = false) }
    }

    private fun mergeGames(
        existing: Map<GameLibraryStatus, Map<Game, LibraryEntry>>,
        add: Map<GameLibraryStatus, Map<Game, LibraryEntry>>,
    ): Map<GameLibraryStatus, Map<Game, LibraryEntry>> =
        existing + add.mapValues { (status, games) -> existing[status].orEmpty() + games }

    private suspend fun groupEntriesByStatus(
        entries: List<LibraryEntry>
    ): Map<GameLibraryStatus, Map<Game, LibraryEntry>> {
        if (entries.isEmpty()) return emptyMap()

        val allGameIds = entries.map { it.gameId }.toSet()
        val gamesResult = safeRequest {
            igdb.getGames {
                fields(Game.field.name, Game.field.cover.image_id)
                where {
                    "id" inAny allGameIds.map { it.toString() }
                }
                // Without an explicit limit IGDB returns only 10, dropping the rest of the page.
                limit(allGameIds.size.coerceIn(1, 500))
            }
        }

        if (gamesResult.isErr) {
            Logger.e(gamesResult.unwrapError()) { "Error loading games for library" }
            return emptyMap()
        }

        val gamesById = gamesResult.unwrap().games.associateBy { it.id }
        return entries
            .mapNotNull { entry -> gamesById[entry.gameId.toLong()]?.let { entry.status to (it to entry) } }
            .groupBy({ it.first }) { it.second }
            .mapValues { (_, pairs) -> pairs.toMap() }
    }

    fun selectSection(section: GameLibraryStatus?) = update { copy(selectedSection = section) }
}
