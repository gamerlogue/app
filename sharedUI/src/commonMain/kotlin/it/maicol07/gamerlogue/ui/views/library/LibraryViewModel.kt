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
import it.maicol07.gamerlogue.extensions.where
import it.maicol07.gamerlogue.safeRequest
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class LibraryViewModel : StateViewModel<LibraryViewModel.UiState>(UiState()) {
    /** Immutable state of the Library screen. */
    data class UiState(
        val loading: Boolean = false,
        val selectedSection: GameLibraryStatus? = null,
        val games: Map<GameLibraryStatus, Map<Game, LibraryEntry>> =
            GameLibraryStatus.entries.associateWith { emptyMap() },
    )

    private val igdb by inject<IgdbClient>()

    fun loadLibraryEntries(section: GameLibraryStatus? = state.selectedSection) = viewModelScope.launch {
        update { copy(loading = true) }

        val result = safeRequest { LibraryEntry.currentUserEntries(section).all().data }
        val entries = if (result.isOk) result.unwrap() else emptyList()

        val grouped = groupEntriesByStatus(entries)

        update {
            val merged = if (section == null) {
                GameLibraryStatus.entries.associateWith { grouped[it] ?: emptyMap() }
            } else {
                games + (section to (grouped[section] ?: emptyMap()))
            }
            copy(games = merged, loading = false)
        }
    }

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
            }
        }

        if (gamesResult.isErr) {
            Logger.e(gamesResult.unwrapError()) { "Error loading games for library" }
            return emptyMap()
        }

        val gamesById = gamesResult.unwrap().games.associateBy { it.id }
        return entries
            .mapNotNull { entry ->
                val game = gamesById[entry.gameId.toLong()] ?: return@mapNotNull null
                Triple(entry.status, game, entry)
            }
            .groupBy({ it.first }) { it.second to it.third }
            .mapValues { (_, pairs) -> pairs.toMap() }
    }

    fun selectSection(section: GameLibraryStatus?) {
        update { copy(selectedSection = section) }
        loadLibraryEntries(section)
    }
}
