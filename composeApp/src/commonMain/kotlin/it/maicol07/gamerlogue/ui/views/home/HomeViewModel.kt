package it.maicol07.gamerlogue.ui.views.home

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.apicalypse.ApicalypseQueryBuilder
import at.released.igdbclient.apicalypse.SortOrder
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.PopularityPrimitive
import at.released.igdbclient.model.UnpackedMultiQueryResult
import at.released.igdbclient.multiquery
import com.github.michaelbull.result.unwrap
import gamerlogue.composeapp.generated.resources.Res
import gamerlogue.composeapp.generated.resources.home__most_loved_games
import gamerlogue.composeapp.generated.resources.home__popular_games
import gamerlogue.composeapp.generated.resources.home__recently_released_games
import gamerlogue.composeapp.generated.resources.home__upcoming_games
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LocalFireDepartmentW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonHeartW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarShineW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.UpcomingW500Rounded
import it.maicol07.gamerlogue.NavBackStack
import it.maicol07.gamerlogue.NavKeys
import it.maicol07.gamerlogue.extensions.alreadyReleased
import it.maicol07.gamerlogue.extensions.notYetReleased
import it.maicol07.gamerlogue.extensions.sort
import it.maicol07.gamerlogue.extensions.where
import it.maicol07.gamerlogue.safeRequest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

const val HomeSectionGameLimit = 50

@OptIn(ExperimentalTime::class)
class HomeViewModel : ViewModel(), KoinComponent {
    enum class HomeSectionType(
        val sectionTitle: StringResource,
        val icon: ImageVector,
        val baseQuery: ApicalypseQueryBuilder.() -> Unit,
        val popscoreQuery: (ApicalypseQueryBuilder.() -> Unit)? = null
    ) {
        POPULAR(
            Res.string.home__popular_games,
            Icons.LocalFireDepartmentW500Rounded,
            { },
            {
                sort(PopularityPrimitive.field.value, SortOrder.DESC)
                where {
                    PopularityPrimitive.field.popularity_type equals "1"
                }
            }
        ),
        MOST_LOVED(
            Res.string.home__most_loved_games,
            Icons.PersonHeartW500Rounded,
            {
                sort(Game.field.rating, SortOrder.DESC)
            }
        ),
        RECENTLY_RELEASED(
            Res.string.home__recently_released_games,
            Icons.StarShineW500Rounded,
            {
                sort(Game.field.first_release_date, SortOrder.DESC)
                where {
                    Game.field.parent_game.isNull()
                    alreadyReleased()
                }
            }
        ),
        UPCOMING(
            Res.string.home__upcoming_games,
            Icons.UpcomingW500Rounded,
            {
                sort(Game.field.first_release_date, SortOrder.ASC)
                where {
                    notYetReleased()
                }
            }
        ),
    }

    val igdb by inject<IgdbClient>()

    val games = mapOf<HomeSectionType, SnapshotStateList<Game>>(
        HomeSectionType.MOST_LOVED to mutableStateListOf(),
        HomeSectionType.UPCOMING to mutableStateListOf(),
        HomeSectionType.POPULAR to mutableStateListOf(),
        HomeSectionType.RECENTLY_RELEASED to mutableStateListOf()
    )
    val loading = mutableStateMapOf<HomeSectionType, Boolean>(
        HomeSectionType.MOST_LOVED to false,
        HomeSectionType.UPCOMING to false,
        HomeSectionType.POPULAR to false,
        HomeSectionType.RECENTLY_RELEASED to false
    )
//    var posts = mutableStateListOf<LibraryEntry>()

//    var user by mutableStateOf<User?>(null)

    val navBackStack by inject<NavBackStack>()

    init {
        loadGames()
    }

    fun loadGames() = viewModelScope.launch {
        for (section in HomeSectionType.entries) loading[section] = true

        val popScores = loadPopScores()

        val result = safeRequest {
            igdb.multiquery {
                for (sectionType in HomeSectionType.entries) {
                    query(IgdbEndpoint.GAME, sectionType.name) {
                        fields(Game.field.name, Game.field.cover.image_id)
                        if (popScores.containsKey(sectionType)) {
                            val ids = popScores[sectionType] ?: emptyList()
                            where {
                                Game.field.id inAny ids.map(Int::toString)
                            }
                        }
                        sectionType.baseQuery(this)
                        limit(HomeSectionGameLimit)
                    }
                }
            }
        }

        if (result.isOk) {
            @Suppress("UNCHECKED_CAST")
            val responseList = result.unwrap() as? List<UnpackedMultiQueryResult<Game>>
//            Log.d("IGDB", "Response: $responseList")
            for (response in (responseList ?: emptyList())) {
                response.results?.let {
                    val section = HomeSectionType.valueOf(response.name)
                    games[section]?.clear()
                    games[section]?.addAll(it)
                    loading[section] = false
                }
            }
        } else {
            for (section in HomeSectionType.entries) loading[section] = false
        }
    }

    suspend fun loadPopScores(): Map<HomeSectionType, List<Int>> {
        val sectionsWithPopscore = HomeSectionType.entries.filter { it.popscoreQuery != null }

        val popScoreResults = safeRequest {
            igdb.multiquery {
                for (sectionType in sectionsWithPopscore) {
                    query(IgdbEndpoint.POPULARITY_PRIMITIVE, sectionType.name) {
                        fields(PopularityPrimitive.field.game_id)
                        sectionType.popscoreQuery?.invoke(this)
                        limit(HomeSectionGameLimit)
                    }
                }
            }
        }

        val popScores = mutableMapOf<HomeSectionType, List<Int>>()

        if (popScoreResults.isOk) {
            @Suppress("UNCHECKED_CAST")
            val responseList = popScoreResults.unwrap() as? List<UnpackedMultiQueryResult<PopularityPrimitive>>
//            Log.d("IGDB", "Popscore Response: $responseList")
            for (response in (responseList ?: emptyList())) {
                response.results?.let {
                    val section = HomeSectionType.valueOf(response.name)
                    val ids = it.map { primitive -> primitive.game_id }
                    popScores[section] = ids
                }
            }
        }

        return popScores
    }

    fun navigateToList(sectionType: HomeSectionType) {
//        navBackStack.add(NavKeys.GameList())
    }

    fun navigateToGame(game: Game) {
        navBackStack.add(NavKeys.GameDetail(game.id.toInt()))
    }

//    fun getUser() {
//        viewModelScope.launch {
//            user = userRepository.getUserByUid(
//                Firebase.auth.currentUser?.uid!!
//            ).first()
//        }
//    }
//
//    private fun fetchPosts() {
//        viewModelScope.launch {
//            libraryRepository.getAll().collectLatest {
//                posts.clear()
//                posts.addAll(it)
//            }
//        }
//    }
}
