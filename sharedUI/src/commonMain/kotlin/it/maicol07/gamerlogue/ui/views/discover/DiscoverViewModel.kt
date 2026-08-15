package it.maicol07.gamerlogue.ui.views.discover

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.PopularityPrimitive
import at.released.igdbclient.multiquery
import com.github.michaelbull.result.unwrap
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.extensions.multiqueryResults
import it.maicol07.gamerlogue.extensions.where
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

const val SectionGameLimit = 50

@OptIn(ExperimentalTime::class)
@KoinViewModel
class DiscoverViewModel : StateViewModel<DiscoverViewModel.UiState>(UiState()) {
    /** Immutable state of the Discover screen, one entry per [DiscoverSection]. */
    data class UiState(
        val sections: Map<DiscoverSection, SectionUiState> =
            DiscoverSection.entries.associateWith { SectionUiState() },
    )

    /** State of a single Discover section. */
    data class SectionUiState(
        val games: List<Game> = emptyList(),
        val loading: Boolean = false,
    )

    private val igdb by inject<IgdbClient>()

    init {
        loadGames()
    }

    fun loadGames() = viewModelScope.launch {
        setAllLoading(true)

        val popScores = loadPopScores()

        val result = safeRequest {
            igdb.multiquery {
                for (section in DiscoverSection.entries) {
                    query(IgdbEndpoint.GAME, section.name) {
                        fields(
                            Game.field.name,
                            Game.field.cover.image_id,
                            Game.field.rating,
                            Game.field.first_release_date,
                            Game.field.artworks.image_id,
                            Game.field.screenshots.image_id,
                        )
                        if (popScores.containsKey(section)) {
                            val ids = popScores[section] ?: emptyList()
                            where {
                                Game.field.id inAny ids.map(Int::toString)
                            }
                        }
                        section.baseQuery(this)
                        limit(SectionGameLimit)
                    }
                }
            }
        }

        if (result.isOk) {
            val responses = result.unwrap()
            for (response in responses) {
                val section = DiscoverSection.valueOf(response.name)
                updateSection(section) {
                    it.copy(games = responses.multiqueryResults(response.name), loading = false)
                }
            }
        } else {
            setAllLoading(false)
        }
    }

    private suspend fun loadPopScores(): Map<DiscoverSection, List<Int>> {
        val sectionsWithPopscore = DiscoverSection.entries.filter { it.popscoreQuery != null }

        val popScoreResults = safeRequest {
            igdb.multiquery {
                for (section in sectionsWithPopscore) {
                    query(IgdbEndpoint.POPULARITY_PRIMITIVE, section.name) {
                        fields(PopularityPrimitive.field.game_id)
                        section.popscoreQuery?.invoke(this)
                        limit(SectionGameLimit)
                    }
                }
            }
        }

        if (popScoreResults.isErr) return emptyMap()

        val responses = popScoreResults.unwrap()
        return responses.associate { response ->
            val ids = responses.multiqueryResults<PopularityPrimitive>(response.name).map { it.game_id }
            DiscoverSection.valueOf(response.name) to ids
        }
    }

    private fun setAllLoading(loading: Boolean) = update {
        copy(sections = sections.mapValues { it.value.copy(loading = loading) })
    }

    private fun updateSection(section: DiscoverSection, transform: (SectionUiState) -> SectionUiState) = update {
        copy(sections = sections + (section to transform(sections[section] ?: SectionUiState())))
    }
}
