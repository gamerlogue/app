package it.maicol07.gamerlogue.ui.views.discover

import androidx.compose.ui.graphics.vector.ImageVector
import at.released.igdbclient.apicalypse.ApicalypseQueryBuilder
import at.released.igdbclient.apicalypse.SortOrder
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.PopularityPrimitive
import at.released.igdbclient.model.ReleaseDate
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.home__most_loved_games
import gamerlogue.sharedui.generated.resources.home__popular_games
import gamerlogue.sharedui.generated.resources.home__recently_released_games
import gamerlogue.sharedui.generated.resources.home__upcoming_games
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LocalFireDepartmentW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonHeartW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarShineW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.UpcomingW500Rounded
import it.maicol07.gamerlogue.extensions.alreadyReleased
import it.maicol07.gamerlogue.extensions.igdb.displayDate
import it.maicol07.gamerlogue.extensions.notYetReleased
import it.maicol07.gamerlogue.extensions.sort
import it.maicol07.gamerlogue.extensions.where
import kotlinx.serialization.Serializable
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.StringResource

/**
 * A curated collection of games, shared by the Discover carousels and the full GameList screen.
 *
 * Each section carries its own IGDB query so both surfaces stay in sync: Discover shows a
 * preview, GameList paginates the same query. Only the entry name is serialized (enum default),
 * so it is safe to embed in navigation keys.
 */
@Serializable
enum class DiscoverSection(
    val sectionTitle: StringResource,
    val icon: ImageVector,
    val baseQuery: ApicalypseQueryBuilder.() -> Unit,
    val popscoreQuery: (ApicalypseQueryBuilder.() -> Unit)? = null,
) {
    POPULAR(
        Res.string.home__popular_games,
        Icons.LocalFireDepartmentW500Rounded,
        { },
        {
            sort(PopularityPrimitive.field.value, SortOrder.DESC)
            where {
                PopularityPrimitive.field.popularity_type equalTo "1"
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

/** Star + score (0-10), or null when the game has no rating. */
internal fun Game.ratingLabel(): String? = rating.takeIf { it > 0.0 }?.let { "★ %.1f".sprintf(it / 10) }

/** Per-section metadata badge shown on a cover card. */
internal fun DiscoverSection.cardMetadata(game: Game): String? = when (this) {
    DiscoverSection.MOST_LOVED -> game.ratingLabel()
    DiscoverSection.RECENTLY_RELEASED, DiscoverSection.UPCOMING ->
        ReleaseDate(date = game.first_release_date).displayDate()
    else -> null
}
