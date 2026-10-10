package it.maicol07.gamerlogue.extensions.igdb

import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameCategoryEnum
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import net.sergeych.sprintf.sprintf
import kotlin.time.Clock

/**
 * The detail destination for this game, carrying the cover and name so the target screen can draw
 * its header before the full game is fetched.
 */
val Game.detailNavKey: RootNavTree.GameDetail
    get() = RootNavTree.GameDetail(
        gameId = id.toInt(),
        coverImageId = cover?.image_id,
        gameName = name,
    )

/**
 * Whether the game is already out. Needs `first_release_date` among the fetched fields: a missing
 * date reads as unreleased (TBA), so a thinner field list would lock the game to the backlog.
 */
fun Game.isReleased(): Boolean =
    first_release_date?.let { it.getEpochSecond() <= Clock.System.now().epochSeconds } ?: false

/**
 * The game a library entry is keyed by: the base game for an edition, the game itself otherwise.
 * Needs `version_parent.id` among the fetched fields, or an edition reads as a base game.
 */
val Game.baseGameId: Int
    get() = (version_parent ?: this).id.toInt()

/**
 * Bundles and minor add-ons (DLC, packs, updates): they belong on their game's page, not in a list of
 * games. Expansions stay, since they are played and tracked on their own.
 */
val BUNDLE_OR_ADDON_GAME_TYPES: List<GameCategoryEnum> =
    listOf(GameCategoryEnum.DLC_ADDON, GameCategoryEnum.BUNDLE, GameCategoryEnum.PACK, GameCategoryEnum.UPDATE)

/**
 * Whether the game belongs in a list of games: neither an edition nor one of the [BUNDLE_OR_ADDON_GAME_TYPES].
 * Needs `game_type` and `version_parent` among the fetched fields, or every game passes.
 */
val Game.isBaseGame: Boolean
    get() = version_parent == null && BUNDLE_OR_ADDON_GAME_TYPES.none { it.value.toLong() == game_type?.id }

/** Score (0-10) with one decimal, or null when the game has no rating. */
fun Game.ratingScore(): String? = rating.takeIf { it > 0.0 }?.let { "%.1f".sprintf(it / 10) }

/** Star + score (0-10), or null when the game has no rating. */
fun Game.ratingLabel(): String? = ratingScore()?.let { "★ $it" }

/** These games in the order of [ids]: IGDB ignores the order of an `id = (...)` filter. */
fun List<Game>.sortedByIds(ids: List<Int>): List<Game> {
    val rank = ids.withIndex().associate { (index, id) -> id.toLong() to index }
    return sortedBy { rank.getValue(it.id) }
}
