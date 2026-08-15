package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameTimeToBeat

const val Ratio169 = 16f / 9f

/**
 * Renders the scrollable content of the Game detail screen for a loaded [game].
 *
 * The sections themselves live next to this file, grouped by concern: [GameRatingSections],
 * [GameTaxonomySections] and [GameMediaSections]. Each one bails out on its own when the game
 * carries no data for it.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
internal fun LazyListScope.gameDetailContent(
    game: Game,
    timeToBeat: GameTimeToBeat? = null,
    onGameClick: ((Game) -> Unit)? = null
) {
    item { GameHeader(game) }
    item { GameRatings(game) }
    item { GameAgeRatings(game) }
    item { GameTimeToBeatSection(timeToBeat) }
    item { GameGenresAndThemes(game) }
    item { GameMultiplayerDetails(game) }
    item { GameMedia(game) }
    item { GameDescription(game) }
    item { GameKeywords(game) }
    item { GameDetailsList(game, onGameClick = onGameClick) }
    item { GameWebsites(game) }
    item { GameRelatedCarousels(game, onGameClick = onGameClick) }
    item { Spacer(Modifier.height(12.dp)) }
}
