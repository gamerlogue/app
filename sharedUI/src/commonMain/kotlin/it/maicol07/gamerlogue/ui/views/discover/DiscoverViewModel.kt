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
import it.maicol07.gamerlogue.extensions.igdb.sortedByIds
import it.maicol07.gamerlogue.extensions.multiqueryResults
import it.maicol07.gamerlogue.extensions.noBundlesOrAddons
import it.maicol07.gamerlogue.extensions.where
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

const val SectionGameLimit = 50

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
        val error: Boolean = false,
    )

    private val igdb by inject<IgdbClient>()

    init {
        loadGames()
    }

    /**
     * Sections ranked by popularity need their game ids first; the others don't wait for that
     * round trip and load in parallel.
     */
    fun loadGames() = viewModelScope.launch {
        update { copy(sections = sections.mapValues { SectionUiState(loading = true) }) }
        val (popscoreSections, plainSections) = DiscoverSection.entries.partition { it.popscoreQuery != null }
        launch { loadSections(plainSections, gameIds = emptyMap()) }
        launch { loadPopscoreSections(popscoreSections) }
    }

    private suspend fun loadPopscoreSections(sections: List<DiscoverSection>) {
        val gameIds = loadPopScores(sections)
        if (gameIds == null) {
            setSections(sections.associateWith { SectionUiState(error = true) })
            return
        }
        // An empty `id = ()` is an IGDB syntax error that would fail the whole multiquery.
        val (withIds, withoutIds) = sections.partition { gameIds[it].orEmpty().isNotEmpty() }
        setSections(withoutIds.associateWith { SectionUiState() })
        loadSections(withIds, gameIds)
    }

    /** Loads [sections] in one multiquery, restricting each to its [gameIds] entry when present. */
    private suspend fun loadSections(sections: List<DiscoverSection>, gameIds: Map<DiscoverSection, List<Int>>) {
        if (sections.isEmpty()) return
        val result = safeRequest {
            igdb.multiquery {
                for (section in sections) {
                    query(IgdbEndpoint.GAME, section.name) {
                        fields(
                            Game.field.name,
                            Game.field.cover.image_id,
                            Game.field.rating,
                            Game.field.first_release_date,
                            Game.field.artworks.image_id,
                            Game.field.screenshots.image_id,
                        )
                        // A section with its own `where` replaces this one, so it repeats these exclusions.
                        where {
                            gameIds[section]?.let { ids -> Game.field.id inAny ids.map(Int::toString) }
                            Game.field.version_parent.isNull()
                            noBundlesOrAddons()
                        }
                        section.baseQuery(this)
                        limit(SectionGameLimit)
                    }
                }
            }
        }

        setSections(
            if (result.isOk) {
                val responses = result.unwrap()
                sections.associateWith { section ->
                    val games = responses.multiqueryResults<Game>(section.name)
                    SectionUiState(games = gameIds[section]?.let(games::sortedByIds) ?: games)
                }
            } else {
                sections.associateWith { SectionUiState(error = true) }
            }
        )
    }

    /** Game ids ranked by popularity for each of [sections], or null when the request fails. */
    private suspend fun loadPopScores(sections: List<DiscoverSection>): Map<DiscoverSection, List<Int>>? {
        val result = safeRequest {
            igdb.multiquery {
                for (section in sections) {
                    query(IgdbEndpoint.POPULARITY_PRIMITIVE, section.name) {
                        fields(PopularityPrimitive.field.game_id)
                        section.popscoreQuery?.invoke(this)
                        limit(SectionGameLimit)
                    }
                }
            }
        }
        if (result.isErr) return null

        val responses = result.unwrap()
        return sections.associateWith { section ->
            responses.multiqueryResults<PopularityPrimitive>(section.name).map { it.game_id }
        }
    }

    private fun setSections(changed: Map<DiscoverSection, SectionUiState>) = update {
        copy(sections = sections + changed)
    }
}
