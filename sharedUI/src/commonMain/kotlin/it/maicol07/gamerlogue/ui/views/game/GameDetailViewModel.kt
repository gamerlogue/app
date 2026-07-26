package it.maicol07.gamerlogue.ui.views.game

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameTimeToBeat
import at.released.igdbclient.multiquery
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
        val timeToBeat: GameTimeToBeat? = null,
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
            igdb.multiquery {
                query(IgdbEndpoint.GAME, "game") {
                    fields(
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
                        "rating_count",
                        "release_dates.date",
                        "release_dates.platform",
                        "release_dates.release_region",
                        "release_dates.status.name",
                        "screenshots.image_id",
                        "themes.name",
                        "summary",
                        "storyline",
                        "game_modes.name",
                        "player_perspectives.name",
                        "game_engines.name",
                        "franchises.name",
                        "franchise.name",
                        "collections.name",
                        "videos.name",
                        "videos.video_id",
                        "websites.category",
                        "websites.url",
                        "websites.trusted",
                        "similar_games.id",
                        "similar_games.name",
                        "similar_games.cover.image_id",
                        "similar_games.rating",
                        "similar_games.first_release_date",
                        "dlcs.id",
                        "dlcs.name",
                        "dlcs.cover.image_id",
                        "dlcs.rating",
                        "dlcs.first_release_date",
                        "expansions.id",
                        "expansions.name",
                        "expansions.cover.image_id",
                        "expansions.rating",
                        "expansions.first_release_date",
                        "parent_game.id",
                        "parent_game.name",
                        "parent_game.cover.image_id",
                        "parent_game.first_release_date",
                        "remakes.id",
                        "remakes.name",
                        "remakes.cover.image_id",
                        "remasters.id",
                        "remasters.name",
                        "age_ratings.category",
                        "age_ratings.rating",
                        "age_ratings.rating_cover_url",
                        "keywords.name",
                        "alternative_names.name",
                        "alternative_names.comment",
                        "multiplayer_modes.campaigncoop",
                        "multiplayer_modes.dropin",
                        "multiplayer_modes.lancoop",
                        "multiplayer_modes.offlinecoop",
                        "multiplayer_modes.offlinecoopmax",
                        "multiplayer_modes.offlinemax",
                        "multiplayer_modes.onlinecoop",
                        "multiplayer_modes.onlinecoopmax",
                        "multiplayer_modes.onlinemax",
                        "multiplayer_modes.splitscreen",
                        "category",
                        "status",
                        "language_supports.language.name",
                        "language_supports.language_support_type.name",
                        "bundles.id",
                        "bundles.name",
                        "bundles.cover.image_id",
                        "bundles.rating",
                        "bundles.first_release_date",
                        "ports.id",
                        "ports.name",
                        "ports.cover.image_id",
                        "ports.rating",
                        "ports.first_release_date",
                        "standalone_expansions.id",
                        "standalone_expansions.name",
                        "standalone_expansions.cover.image_id",
                        "standalone_expansions.rating",
                        "standalone_expansions.first_release_date",
                        "expanded_games.id",
                        "expanded_games.name",
                        "expanded_games.cover.image_id",
                        "expanded_games.rating",
                        "expanded_games.first_release_date",
                        "collections.games.id",
                        "collections.games.name",
                        "collections.games.cover.image_id",
                        "collections.games.rating",
                        "collections.games.first_release_date",
                        "version_parent.id",
                        "version_parent.name",
                        "version_parent.cover.image_id",
                        "version_parent.first_release_date"
                    )
                    where("id = $gameId")
                    limit(1)
                }
                query(IgdbEndpoint.GAME_TIME_TO_BEAT, "ttb") {
                    fields(
                        "completely",
                        "hastily",
                        "normally",
                        "game_id"
                    )
                    where("game_id = $gameId")
                    limit(1)
                }
            }
        }

        if (result.isOk) {
            var fetchedGame: Game? = null
            var fetchedTtb: GameTimeToBeat? = null
            for (response in result.unwrap()) {
                when (response.name) {
                    "game" -> @Suppress("UNCHECKED_CAST") {
                        fetchedGame = (response.results as? List<Game>)?.firstOrNull()
                    }
                    "ttb" -> @Suppress("UNCHECKED_CAST") {
                        fetchedTtb = (response.results as? List<GameTimeToBeat>)?.firstOrNull()
                    }
                }
            }
            update { copy(game = fetchedGame ?: state.game, timeToBeat = fetchedTtb, isLoading = false) }
        } else {
            update { copy(isLoading = false) }
        }
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

    private suspend fun applyStatus(status: GameLibraryStatus) {
        val game = state.game ?: return
        val current = state.libraryEntry
        val draft = LibraryEntry.quickDraft(
            game = game,
            status = status,
            user = null,
            existing = current
        )
        draft.save()
        update { copy(libraryEntry = draft) }
    }

    private suspend fun removeGameLibraryEntry() {
        val current = state.libraryEntry ?: return
        current.destroy()
        update { copy(libraryEntry = null) }
    }
}
