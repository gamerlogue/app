package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.runtime.Composable
import at.released.igdbclient.model.GameStatusEnum
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game_status__alpha
import gamerlogue.sharedui.generated.resources.game_status__beta
import gamerlogue.sharedui.generated.resources.game_status__cancelled
import gamerlogue.sharedui.generated.resources.game_status__delisted
import gamerlogue.sharedui.generated.resources.game_status__early_access
import gamerlogue.sharedui.generated.resources.game_status__offline
import gamerlogue.sharedui.generated.resources.game_status__released
import gamerlogue.sharedui.generated.resources.game_status__rumored
import gamerlogue.sharedui.generated.resources.game_status__unknown
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val GameStatusEnum.localizedName: String
    @Composable
    get() {
        val s = when (this) {
            GameStatusEnum.RELEASED -> Res.string.game_status__released
            GameStatusEnum.ALPHA -> Res.string.game_status__alpha
            GameStatusEnum.BETA -> Res.string.game_status__beta
            GameStatusEnum.EARLY_ACCESS -> Res.string.game_status__early_access
            GameStatusEnum.OFFLINE -> Res.string.game_status__offline
            GameStatusEnum.CANCELLED -> Res.string.game_status__cancelled
            GameStatusEnum.RUMORED -> Res.string.game_status__rumored
            GameStatusEnum.DELISTED -> Res.string.game_status__delisted
            else -> name.ifEmpty { Res.string.game_status__unknown }
        }
        return if (s is StringResource) stringResource(s) else s.toString()
    }
