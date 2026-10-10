package it.maicol07.gamerlogue.ui.views.game

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
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.currentUserEntryForGame
import it.maicol07.gamerlogue.extensions.multiqueryResults
import it.maicol07.gamerlogue.extensions.quickDraft
import it.maicol07.gamerlogue.extensions.self
import it.maicol07.gamerlogue.extensions.where
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

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
        url,
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
        age_ratings.organization.name,
        age_ratings.rating_category.rating,
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
        /** Other editions of [game] (IGDB versions whose `version_parent` it is). */
        val editions: List<Game> = emptyList(),
        val timeToBeat: GameTimeToBeat? = null,
        val libraryEntry: LibraryEntry? = null,
        val isLoading: Boolean = true,
        /** The last load failed (network, server), as opposed to IGDB returning no such game. */
        val isLoadError: Boolean = false,
        /** The library is per-user: signed out, there is no entry to show or edit. */
        val isAuthenticated: Boolean = false,
        /** Status whose toggle is in flight, so only that button shows a spinner. */
        val pendingStatus: GameLibraryStatus? = null,
    )

    private val igdb by inject<IgdbClient>()
    private val authProvider by inject<AuthTokenProvider>()

    companion object {
        /** Sub-query names of the detail multiquery; they pick the results apart again below. */
        private const val GAME_QUERY = "game"
        private const val TIME_TO_BEAT_QUERY = "ttb"
        private const val EDITIONS_QUERY = "editions"
        private const val EDITIONS_LIMIT = 50
    }

    init {
        loadGameDetails()
        viewModelScope.launch {
            authProvider.session.map { it.isAuthenticated }.distinctUntilChanged().collect { authenticated ->
                update { copy(isAuthenticated = authenticated, libraryEntry = null) }
                if (authenticated) loadLibraryEntry()
            }
        }
    }

    fun loadGameDetails(): Job = viewModelScope.launch {
        update { copy(isLoading = true, isLoadError = false) }
        val result = safeRequest {
            igdb.multiquery {
                query(IgdbEndpoint.GAME, GAME_QUERY) {
                    // fields() only takes varargs; copying ~100 references once per load is noise next to the request.
                    @Suppress("SpreadOperator")
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
                query(IgdbEndpoint.GAME, EDITIONS_QUERY) {
                    @Suppress("SpreadOperator")
                    fields(*Game.field.relatedGameFields().toTypedArray())
                    where { Game.field.version_parent equalTo gameId.toString() }
                    limit(EDITIONS_LIMIT)
                }
            }
        }

        if (result.isOk) {
            val responses = result.unwrap()
            val fetchedGame = responses.multiqueryResults<Game>(GAME_QUERY).firstOrNull()
            val fetchedTtb = responses.multiqueryResults<GameTimeToBeat>(TIME_TO_BEAT_QUERY).firstOrNull()
            val editions = responses.multiqueryResults<Game>(EDITIONS_QUERY)
            update { copy(game = fetchedGame ?: state.game, editions = editions, timeToBeat = fetchedTtb, isLoading = false) }
        } else {
            update { copy(isLoading = false, isLoadError = true) }
        }
    }

    fun loadLibraryEntry(): Job = viewModelScope.launch {
        val result = safeRequest { LibraryEntry.currentUserEntryForGame(gameId).firstOrNull().data }
        // A failed reload keeps the known entry: clearing it would offer "add" and invite a duplicate.
        if (result.isOk) update { copy(libraryEntry = result.unwrap()) }
    }

    /**
     * Applies [status] to the library entry or removes the entry when it already has that status.
     *
     * Failures are reported by [safeRequest] and leave the state untouched.
     */
    fun toggleStatus(status: GameLibraryStatus) = viewModelScope.launch {
        update { copy(pendingStatus = status) }
        if (state.libraryEntry?.status == status) {
            removeGameLibraryEntry()
        } else {
            applyStatus(status)
        }
        update { copy(pendingStatus = null) }
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
