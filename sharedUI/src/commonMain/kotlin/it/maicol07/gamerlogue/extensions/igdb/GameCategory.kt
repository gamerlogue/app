package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.runtime.Composable
import at.released.igdbclient.model.GameCategoryEnum
import at.released.igdbclient.model.GameType
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

private val GAME_CATEGORY_NAMES: Map<String, StringResource> = mapOf(
    "MAIN_GAME" to Res.string.game_category__main_game,
    "DLC_ADDON" to Res.string.game_category__dlc,
    "EXPANSION" to Res.string.game_category__expansion,
    "BUNDLE" to Res.string.game_category__bundle,
    "STANDALONE_EXPANSION" to Res.string.game_category__standalone_expansion,
    "MOD" to Res.string.game_category__mod,
    "EPISODE" to Res.string.game_category__episode,
    "SEASON" to Res.string.game_category__season,
    "REMAKE" to Res.string.game_category__remake,
    "REMASTER" to Res.string.game_category__remaster,
    "EXPANDED_GAME" to Res.string.game_category__expanded_game,
    "PORT" to Res.string.game_category__port,
    "FORK" to Res.string.game_category__fork,
    "PACK" to Res.string.game_category__pack_addon,
    "UPDATE" to Res.string.game_category__update,
    "PACK_ADDON" to Res.string.game_category__pack_addon,
)

internal fun gameCategoryStringResource(name: String): StringResource? =
    GAME_CATEGORY_NAMES[name.uppercase().replace(" / ", "_").replace(' ', '_')]

val GameCategoryEnum.localizedName: String
    @Composable
    get() = gameCategoryStringResource(name)?.let { stringResource(it) }
        ?: name.ifEmpty { stringResource(Res.string.game_category__unknown) }

val GameType.localizedName: String
    @Composable
    get() = gameCategoryStringResource(type)?.let { stringResource(it) }
        ?: type.ifEmpty { stringResource(Res.string.game_category__unknown) }
