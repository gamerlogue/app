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
import it.maicol07.gamerlogue.extensions.totalItems
import it.maicol07.gamerlogue.extensions.where
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

/** Backs the Library screen: a preview of each status, whose full list opens in GameList. */
@KoinViewModel
class LibraryViewModel : StateViewModel<LibraryViewModel.UiState>(UiState()) {
    /** One status' preview: its first games and how many the user has in total. */
    data class SectionUiState(
        val loading: Boolean = true,
        val error: Boolean = false,
        val entries: Map<Game, LibraryEntry> = emptyMap(),
        /** Total entries in the status, from the page meta; null until loaded. */
        val count: Int? = null,
    )

    /** Immutable state of the Library screen, one entry per [GameLibraryStatus]. */
    data class UiState(
        val sections: Map<GameLibraryStatus, SectionUiState> =
            GameLibraryStatus.entries.associateWith { SectionUiState() },
    )

    private companion object {
        const val PREVIEW_SIZE = 20
    }

    private val igdb by inject<IgdbClient>()

    /** Reloads every status preview in parallel. */
    fun loadPreviews() = viewModelScope.launch {
        update { UiState() }
        GameLibraryStatus.entries.forEach { status -> launch { setSection(status, loadSection(status)) } }
    }

    /** The status' first page (the backend's page size is fixed), whose meta also carries the total. */
    private suspend fun loadSection(status: GameLibraryStatus): SectionUiState {
        val result = safeRequest { LibraryEntry.currentUserEntries(status).all() }
        if (result.isErr) {
            Logger.e(result.unwrapError()) { "Error loading library entries" }
            return SectionUiState(loading = false, error = true)
        }
        val page = result.unwrap()
        val games = fetchGames(page.data.take(PREVIEW_SIZE)) ?: return SectionUiState(loading = false, error = true)
        return SectionUiState(loading = false, entries = games, count = page.totalItems)
    }

    /** The IGDB game of each entry, in entry order; null when IGDB fails. */
    private suspend fun fetchGames(entries: List<LibraryEntry>): Map<Game, LibraryEntry>? {
        if (entries.isEmpty()) return emptyMap()

        val gameIds = entries.map { it.gameId }.toSet()
        val result = safeRequest {
            igdb.getGames {
                fields(Game.field.name, Game.field.cover.image_id)
                where {
                    "id" inAny gameIds.map { it.toString() }
                }
                // Without an explicit limit IGDB returns only 10.
                limit(gameIds.size)
            }
        }
        if (result.isErr) {
            Logger.e(result.unwrapError()) { "Error loading games for library" }
            return null
        }

        val gamesById = result.unwrap().games.associateBy { it.id }
        return entries
            .mapNotNull { entry -> gamesById[entry.gameId.toLong()]?.let { it to entry } }
            .toMap()
    }

    private fun setSection(status: GameLibraryStatus, section: SectionUiState) =
        update { copy(sections = sections + (status to section)) }
}
