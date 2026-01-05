package it.maicol07.gamerlogue.ui.views.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.getGames
import at.released.igdbclient.model.Game
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.safeRequest
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.gamerlogue.ui.views.library.LibraryViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class GameDetailViewModel(val gameId: Int) : ViewModel(), KoinComponent {
    private val igdb by inject<IgdbClient>()
    private val libraryViewModel by inject<LibraryViewModel>()

    var game by mutableStateOf<Game?>(null)
    var libraryEntry by mutableStateOf<LibraryEntry?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    // Loading states
    var isLoading by mutableStateOf(true)
    var isPlayingButtonLoading by mutableStateOf(false)
    var isBacklogButtonLoading by mutableStateOf(false)

    companion object {
        @Composable
        fun inject(gameId: Int): GameDetailViewModel = koinViewModel(parameters = { parametersOf(gameId) })

        @Composable
        fun inject(game: Game): GameDetailViewModel = inject(game.id.toInt())
    }

    init {
        viewModelScope.launch { loadGameDetails() }
        loadLibraryEntry()
    }

    suspend fun loadGameDetails() {
        isLoading = true
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
        if (result.isOk) {
            val response = result.unwrap()
            game = response.games.firstOrNull()
        }
        isLoading = false
    }

    fun loadLibraryEntry() = viewModelScope.launch {
        val result = libraryViewModel.getLibraryEntryForGame(gameId)
        libraryEntry = if (result.isOk) result.unwrap() else null
    }

    fun toggleGamePlaying() = viewModelScope.launch {
        isPlayingButtonLoading = true

        try {
            if (libraryEntry?.status == GameLibraryStatus.PLAYING) {
                removeGameLibraryEntry()
            } else {
                libraryViewModel.quickToggleGameLibraryEntry(game!!, GameLibraryStatus.PLAYING, libraryEntry)
                loadLibraryEntry().join()
            }
        } catch (e: Exception) {
            errorMessage = e.message
        }

        isPlayingButtonLoading = false
    }

    fun toggleGameBacklog() = viewModelScope.launch {
        isBacklogButtonLoading = true
        try {
            if (libraryEntry?.status == GameLibraryStatus.BACKLOG) {
                removeGameLibraryEntry()
            } else {
                libraryViewModel.quickToggleGameLibraryEntry(game!!, GameLibraryStatus.BACKLOG, libraryEntry)
                loadLibraryEntry().join()
            }
        } catch (e: Exception) {
            errorMessage = e.message
        }
        isBacklogButtonLoading = false
    }

    suspend fun removeGameLibraryEntry() {
        if (libraryEntry != null) {
            libraryViewModel.quickToggleGameLibraryEntry(game!!, GameLibraryStatus.BACKLOG, libraryEntry)
            loadLibraryEntry().join()
        }
    }
}
