package it.maicol07.gamerlogue.ui.views.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.getGames
import at.released.igdbclient.model.Game
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.where
import it.maicol07.gamerlogue.safeRequest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LibraryViewModel : ViewModel(), KoinComponent {
    val igdb by inject<IgdbClient>()
    val authTokenProvider by inject<AuthTokenProvider>()

    var libraryLoading by mutableStateOf(false)
    var selectedSection by mutableStateOf<GameLibraryStatus?>(null)
        private set

    var libraryGames = mapOf<GameLibraryStatus, SnapshotStateMap<Game, LibraryEntry>>(
        GameLibraryStatus.PLAYING to mutableStateMapOf(),
        GameLibraryStatus.COMPLETED to mutableStateMapOf(),
        GameLibraryStatus.PAUSED to mutableStateMapOf(),
        GameLibraryStatus.ABANDONED to mutableStateMapOf(),
        GameLibraryStatus.BACKLOG to mutableStateMapOf()
    )

    fun loadLibraryEntries(section: GameLibraryStatus? = null) = viewModelScope.launch {
        libraryLoading = true

        if (section != null) {
            libraryGames[section]?.clear()
        } else {
            for (sec in GameLibraryStatus.entries) {
                libraryGames[sec]?.clear()
            }
        }

        val result = safeRequest {
            LibraryEntry
                .scope {
                    if (section != null) {
                        where("status", section.name)
                    }
                    where("current_user", "true")
                }
                .all()
                .data
        }

        if (result.isOk && result.unwrap().isNotEmpty()) {
            val entries = result.unwrap()
            val allGameIds = entries.map { it.gameId }.toSet()
            val allGamesResult = safeRequest {
                igdb.getGames {
                    fields(Game.field.name, Game.field.cover.image_id)
                    where {
                        "id" inAny allGameIds.map { it.toString() }
                    }
                }
            }

            if (allGamesResult.isOk) {
                val allGames = allGamesResult.unwrap().games.associateBy { it.id }
                for (entry in entries) {
                    val game = allGames[entry.gameId.toLong()] ?: continue
                    libraryGames[entry.status]?.put(game, entry)
                }
            } else {
                println("Error loading games for library: ${allGamesResult.unwrapError()}")
            }
        }

        libraryLoading = false
    }

    fun selectSection(section: GameLibraryStatus?) {
        selectedSection = section
        loadLibraryEntries(section)
    }

    suspend fun quickToggleGameLibraryEntry(game: Game, status: GameLibraryStatus, existingEntry: LibraryEntry? = null) {
        val entry = existingEntry ?: LibraryEntry()
        entry.gameId = game.id.toInt()
        entry.owned = false
        entry.user = authTokenProvider.currentUser
        entry.status = status
        updateLibraryEntry(entry)
    }

    suspend fun updateLibraryEntry(entry: LibraryEntry) = safeRequest { entry.save() }
    suspend fun removeLibraryEntry(entry: LibraryEntry) = safeRequest { entry.destroy() }

    suspend fun getLibraryEntryForGame(game: Game) = getLibraryEntryForGame(game.id)
    suspend fun getLibraryEntryForGame(gameId: Number) = safeRequest {
        LibraryEntry
            .where("game_id", gameId)
            .extraParam("current_user", "true")
            .firstOrNull()
            .data
    }
}

