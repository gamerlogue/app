package it.maicol07.gamerlogue.ui.views.game

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.dsl.field.GameFieldDsl
import at.released.igdbclient.dsl.field.IgdbRequestField
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameTimeToBeat
import at.released.igdbclient.multiquery
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.currentUserEntryForGame
import it.maicol07.gamerlogue.extensions.multiqueryResults
import it.maicol07.gamerlogue.extensions.quickDraft
import it.maicol07.gamerlogue.extensions.self
import it.maicol07.gamerlogue.extensions.where
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Cover, title, and release date of a game shown in one of the detail screen's carousels. */
private fun GameFieldDsl.relatedGameFields(): List<IgdbRequestField<*>> = listOf(
    id,
    name,
    cover.image_id,
    rating,
    first_release_date,
)

/**
 * Everything the detail screen renders, in one request.
 *
 * Written with the generated field DSL rather than raw strings: a field renamed or dropped by IGDB
 * fails to compile here instead of silently returning nothing at runtime.
 */
internal val DetailFields: List<IgdbRequestField<*>> = with(Game.field) {
    listOf(
        name,
        summary,
        storyline,
        game_type.type,
        game_status.status,
        rating,
        rating_count,
        aggregated_rating,
        aggregated_rating_count,
        first_release_date,
        cover.image_id,
        artworks.image_id,
        screenshots.image_id,
        videos.name,
        videos.video_id,
        genres.name,
        themes.name,
        keywords.name,
        game_modes.name,
        player_perspectives.name,
        game_engines.name,
        franchise.name,
        franchises.name,
        collections.name,
        platforms.id,
        platforms.name,
        platforms.platform_logo.image_id,
        release_dates.date,
        release_dates.platform.self,
        release_dates.release_region.self,
        release_dates.status.name,
        involved_companies.company.name,
        involved_companies.developer,
        involved_companies.publisher,
        age_ratings.category,
        age_ratings.rating,
        age_ratings.rating_cover_url,
        alternative_names.name,
        alternative_names.comment,
        language_supports.language.name,
        language_supports.language_support_type.name,
        multiplayer_modes.campaigncoop,
        multiplayer_modes.dropin,
        multiplayer_modes.lancoop,
        multiplayer_modes.offlinecoop,
        multiplayer_modes.offlinecoopmax,
        multiplayer_modes.offlinemax,
        multiplayer_modes.onlinecoop,
        multiplayer_modes.onlinecoopmax,
        multiplayer_modes.onlinemax,
        multiplayer_modes.splitscreen,
        websites.category,
        websites.url,
        websites.trusted,
        parent_game.id,
        parent_game.name,
        parent_game.cover.image_id,
        parent_game.first_release_date,
        version_parent.id,
        version_parent.name,
        version_parent.cover.image_id,
        version_parent.first_release_date,
    ) + listOf(
        remakes,
        remasters,
        similar_games,
        dlcs,
        expansions,
        standalone_expansions,
        expanded_games,
        bundles,
        ports,
        collections.games,
    ).flatMap { it.relatedGameFields() }
}

@KoinViewModel
class GameDetailViewModel(@InjectedParam val gameId: Int) : StateViewModel<GameDetailViewModel.UiState>(UiState()) {
    /** Immutable state of the Game detail screen. */
    data class UiState(
        val game: Game? = null,
        val timeToBeat: GameTimeToBeat? = null,
        val libraryEntry: LibraryEntry? = null,
        val isLoading: Boolean = true,
        val isPlayingButtonLoading: Boolean = false,
        val isBacklogButtonLoading: Boolean = false,
    )

    private val igdb by inject<IgdbClient>()

    companion object {
        /** Sub-query names of the detail multiquery; they pick the results apart again below. */
        private const val GAME_QUERY = "game"
        private const val TIME_TO_BEAT_QUERY = "ttb"

        @Composable
        fun inject(gameId: Int): GameDetailViewModel = koinViewModel(parameters = { parametersOf(gameId) })
    }

    init {
        viewModelScope.launch { loadGameDetails() }
        loadLibraryEntry()
    }

    suspend fun loadGameDetails() {
        update { copy(isLoading = true) }
        val result = safeRequest {
            igdb.multiquery {
                query(IgdbEndpoint.GAME, GAME_QUERY) {
                    fields(*DetailFields.toTypedArray())
                    where { Game.field.id equalTo gameId.toString() }
                    limit(1)
                }
                query(IgdbEndpoint.GAME_TIME_TO_BEAT, TIME_TO_BEAT_QUERY) {
                    fields(
                        GameTimeToBeat.field.completely,
                        GameTimeToBeat.field.hastily,
                        GameTimeToBeat.field.normally,
                        GameTimeToBeat.field.game_id,
                    )
                    where { GameTimeToBeat.field.game_id equalTo gameId.toString() }
                    limit(1)
                }
            }
        }

        if (result.isOk) {
            val responses = result.unwrap()
            val fetchedGame = responses.multiqueryResults<Game>(GAME_QUERY).firstOrNull()
            val fetchedTtb = responses.multiqueryResults<GameTimeToBeat>(TIME_TO_BEAT_QUERY).firstOrNull()
            update { copy(game = fetchedGame ?: state.game, timeToBeat = fetchedTtb, isLoading = false) }
        } else {
            update { copy(isLoading = false) }
        }
    }

    fun loadLibraryEntry(): Job = viewModelScope.launch {
        val result = safeRequest { LibraryEntry.currentUserEntryForGame(gameId).firstOrNull().data }
        update { copy(libraryEntry = if (result.isOk) result.unwrap() else null) }
    }

    fun toggleGamePlaying() = toggleStatus(GameLibraryStatus.PLAYING) { copy(isPlayingButtonLoading = it) }

    fun toggleGameBacklog() = toggleStatus(GameLibraryStatus.BACKLOG) { copy(isBacklogButtonLoading = it) }

    /**
     * Applies [status] to the library entry or removes the entry when it already has that status.
     *
     * [setLoading] flips the button's own spinner, which is the only thing that differs between the
     * two toggles. Failures are reported by [safeRequest] and leave the state untouched.
     */
    private fun toggleStatus(
        status: GameLibraryStatus,
        setLoading: UiState.(Boolean) -> UiState,
    ) = viewModelScope.launch {
        update { setLoading(true) }
        if (state.libraryEntry?.status == status) {
            removeGameLibraryEntry()
        } else {
            applyStatus(status)
        }
        update { setLoading(false) }
    }

    private suspend fun applyStatus(status: GameLibraryStatus) {
        val game = state.game ?: return
        val draft = LibraryEntry.quickDraft(
            game = game,
            status = status,
            user = null,
            existing = state.libraryEntry
        )
        val result = safeRequest { draft.save() }
        if (result.isOk) {
            update { copy(libraryEntry = draft) }
        }
    }

    private suspend fun removeGameLibraryEntry() {
        val current = state.libraryEntry ?: return
        val result = safeRequest { current.destroy() }
        if (result.isOk) {
            update { copy(libraryEntry = null) }
        }
    }
}
