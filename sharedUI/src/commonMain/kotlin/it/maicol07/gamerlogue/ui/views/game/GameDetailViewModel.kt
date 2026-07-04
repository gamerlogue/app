package it.maicol07.gamerlogue.ui.views.game

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.getGames
import at.released.igdbclient.model.Game
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.currentUserEntryForGame
import it.maicol07.gamerlogue.extensions.quickDraft
import it.maicol07.gamerlogue.ui.views.game.GameHandoff.take
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * In-memory handoff of the [Game] already loaded by a list screen to the detail screen, so the
 * detail can render its hero instantly instead of waiting for [GameDetailViewModel.loadGameDetails].
 * ponytail: plain map, single-entry-per-id, read-once. Cleared on [take]; survives only until the
 * detail VM is created. Good enough for navigation handoff — no caching layer needed.
 */
object GameHandoff {
    private val pending = mutableMapOf<Int, Game>()
    fun put(game: Game) {
        pending[game.id.toInt()] = game
    }

    fun take(gameId: Int): Game? = pending.remove(gameId)
}

@KoinViewModel
class GameDetailViewModel(@InjectedParam val gameId: Int) : StateViewModel<GameDetailViewModel.UiState>(UiState()) {
    /** Immutable state of the Game detail screen. */
    data class UiState(
        val game: Game? = null,
        val libraryEntry: LibraryEntry? = null,
        val errorMessage: String? = null,
        val isLoading: Boolean = true,
        val isPlayingButtonLoading: Boolean = false,
        val isBacklogButtonLoading: Boolean = false,
    )

    private val igdb by inject<IgdbClient>()
    private val authTokenProvider by inject<AuthTokenProvider>()

    companion object {
        @Composable
        fun inject(gameId: Int): GameDetailViewModel = koinViewModel(parameters = { parametersOf(gameId) })

        @Suppress("unused")
        @Composable
        fun inject(game: Game): GameDetailViewModel = inject(game.id.toInt())
    }

    init {
        // Seed with the Game already loaded by the originating list so the hero (cover + banner)
        // renders from the first frame — required for the shared-element transition to have a
        // target while the full detail query is still in flight.
        GameHandoff.take(gameId)?.let { seed -> update { copy(game = seed) } }
        viewModelScope.launch { loadGameDetails() }
        loadLibraryEntry()
    }

    suspend fun loadGameDetails() {
        update { copy(isLoading = true) }
        val result = safeRequest {
            igdb.getGames {
                fields(
                    "aggregated_rating",
                    "artworks.image_id",
                    "cover.image_id",
                    "first_release_date",
                    "genres.name",
                    "involved_companies.company.name",
                    "involved_companies.developer",
                    "involved_companies.publisher",
                    "name",
                    "platforms.id",
                    "platforms.name",
                    "platforms.platform_logo.image_id",
                    "rating",
                    "release_dates.date",
                    "release_dates.platform",
                    "release_dates.release_region",
                    "release_dates.status.name",
                    "screenshots.image_id",
                    "themes.name",
                    "summary",
                )
                where("id = $gameId")
                limit(1)
            }
        }
        val game = if (result.isOk) result.unwrap().games.firstOrNull() else null
        update { copy(game = game, isLoading = false) }
    }

    fun loadLibraryEntry(): Job = viewModelScope.launch {
        val result = safeRequest { LibraryEntry.currentUserEntryForGame(gameId).firstOrNull().data }
        update { copy(libraryEntry = if (result.isOk) result.unwrap() else null) }
    }

    fun toggleGamePlaying() = viewModelScope.launch {
        update { copy(isPlayingButtonLoading = true) }
        try {
            if (state.libraryEntry?.status == GameLibraryStatus.PLAYING) {
                removeGameLibraryEntry()
            } else {
                applyStatus(GameLibraryStatus.PLAYING)
            }
        } catch (e: Exception) {
            update { copy(errorMessage = e.message) }
        }
        update { copy(isPlayingButtonLoading = false) }
    }

    fun toggleGameBacklog() = viewModelScope.launch {
        update { copy(isBacklogButtonLoading = true) }
        try {
            if (state.libraryEntry?.status == GameLibraryStatus.BACKLOG) {
                removeGameLibraryEntry()
            } else {
                applyStatus(GameLibraryStatus.BACKLOG)
            }
        } catch (e: Exception) {
            update { copy(errorMessage = e.message) }
        }
        update { copy(isBacklogButtonLoading = false) }
    }

    private suspend fun removeGameLibraryEntry() {
        if (state.libraryEntry != null) {
            applyStatus(GameLibraryStatus.BACKLOG)
        }
    }

    private suspend fun applyStatus(status: GameLibraryStatus) {
        val game = state.game ?: return
        safeRequest { LibraryEntry.quickDraft(game, status, authTokenProvider.currentUser, state.libraryEntry).save() }
        loadLibraryEntry().join()
    }
}
