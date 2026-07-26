package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.runtime.Composable
import at.released.igdbclient.model.GameCategoryEnum
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game_category__bundle
import gamerlogue.sharedui.generated.resources.game_category__dlc
import gamerlogue.sharedui.generated.resources.game_category__episode
import gamerlogue.sharedui.generated.resources.game_category__expanded_game
import gamerlogue.sharedui.generated.resources.game_category__expansion
import gamerlogue.sharedui.generated.resources.game_category__fork
import gamerlogue.sharedui.generated.resources.game_category__main_game
import gamerlogue.sharedui.generated.resources.game_category__mod
import gamerlogue.sharedui.generated.resources.game_category__pack_addon
import gamerlogue.sharedui.generated.resources.game_category__port
import gamerlogue.sharedui.generated.resources.game_category__remake
import gamerlogue.sharedui.generated.resources.game_category__remaster
import gamerlogue.sharedui.generated.resources.game_category__season
import gamerlogue.sharedui.generated.resources.game_category__standalone_expansion
import gamerlogue.sharedui.generated.resources.game_category__unknown
import gamerlogue.sharedui.generated.resources.game_category__update
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val GameCategoryEnum.localizedName: String
    @Composable
    get() {
        val s = when (this) {
            GameCategoryEnum.MAIN_GAME -> Res.string.game_category__main_game
            GameCategoryEnum.DLC_ADDON -> Res.string.game_category__dlc
            GameCategoryEnum.EXPANSION -> Res.string.game_category__expansion
            GameCategoryEnum.BUNDLE -> Res.string.game_category__bundle
            GameCategoryEnum.STANDALONE_EXPANSION -> Res.string.game_category__standalone_expansion
            GameCategoryEnum.MOD -> Res.string.game_category__mod
            GameCategoryEnum.EPISODE -> Res.string.game_category__episode
            GameCategoryEnum.SEASON -> Res.string.game_category__season
            GameCategoryEnum.REMAKE -> Res.string.game_category__remake
            GameCategoryEnum.REMASTER -> Res.string.game_category__remaster
            GameCategoryEnum.EXPANDED_GAME -> Res.string.game_category__expanded_game
            GameCategoryEnum.PORT -> Res.string.game_category__port
            GameCategoryEnum.FORK -> Res.string.game_category__fork
            GameCategoryEnum.PACK -> Res.string.game_category__pack_addon
            GameCategoryEnum.UPDATE -> Res.string.game_category__update
            else -> name.ifEmpty { Res.string.game_category__unknown }
        }
        return if (s is StringResource) stringResource(s) else s.toString()
    }
