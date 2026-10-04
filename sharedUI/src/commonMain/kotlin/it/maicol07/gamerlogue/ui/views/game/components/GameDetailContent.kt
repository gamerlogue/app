package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameTimeToBeat
import it.maicol07.gamerlogue.ui.views.list.GameListPreset

const val Ratio169 = 16f / 9f

private val ToolbarClearance = 96.dp

/**
 * Renders the scrollable content of the Game detail screen for a loaded [game].
 *
 * The sections themselves live next to this file, grouped by concern: `GameRatingSections.kt`,
 * `GameTaxonomySections.kt` and `GameMediaSections.kt`. Each one bails out on its own when the game
 * carries no data for it.
 */
internal fun LazyListScope.gameDetailContent(
    game: Game,
    timeToBeat: GameTimeToBeat?,
    onTitleVisibilityChange: (Boolean) -> Unit,
    onGameClick: (Game) -> Unit,
    onPresetClick: (GameListPreset) -> Unit
) {
    item { GameHeader(game, onTitleVisibilityChange, onPresetClick) }
    item { GameRatings(game) }
    item { GameAgeRatings(game) }
    item { GameTimeToBeatSection(timeToBeat) }
    item { GameGenresAndThemes(game, onPresetClick) }
    item { GameMultiplayerDetails(game) }
    item { GameMedia(game) }
    item { GameDescription(game) }
    item { GameKeywords(game, onPresetClick) }
    item { GameDetailsList(game, onGameClick = onGameClick, onPresetClick = onPresetClick) }
    item { GameWebsites(game) }
    item { GameRelatedCarousels(game, onGameClick = onGameClick) }
    // Room for the floating toolbar, so the last section can scroll clear of it.
    item { Spacer(Modifier.navigationBarsPadding().height(ToolbarClearance)) }
}
