package it.maicol07.gamerlogue.extensions.igdb

import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree

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
