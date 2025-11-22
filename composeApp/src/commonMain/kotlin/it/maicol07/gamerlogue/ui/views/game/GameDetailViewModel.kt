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
import it.maicol07.gamerlogue.safeRequest
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class GameDetailViewModel(val gameId: Int) : ViewModel(), KoinComponent {
    val igdb by inject<IgdbClient>()
    var game by mutableStateOf<Game?>(null)
    var isLoading by mutableStateOf(true)

    companion object {
        @Composable
        fun inject(gameId: Int): GameDetailViewModel = koinViewModel(parameters = { parametersOf(gameId) })

        @Composable
        fun inject(game: Game): GameDetailViewModel = inject(game.id.toInt())
    }

    init {
        viewModelScope.launch {
            loadGameDetails()
        }
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
}
