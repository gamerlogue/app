package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.runtime.Composable
import at.released.igdbclient.model.GameStatus
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

private val GAME_STATUS_NAMES: Map<String, StringResource> = mapOf(
    "RELEASED" to Res.string.game_status__released,
    "ALPHA" to Res.string.game_status__alpha,
    "BETA" to Res.string.game_status__beta,
    "EARLY_ACCESS" to Res.string.game_status__early_access,
    "OFFLINE" to Res.string.game_status__offline,
    "CANCELLED" to Res.string.game_status__cancelled,
    "RUMORED" to Res.string.game_status__rumored,
    "DELISTED" to Res.string.game_status__delisted,
)

internal fun gameStatusStringResource(name: String): StringResource? =
    GAME_STATUS_NAMES[name.uppercase().replace(" / ", "_").replace(' ', '_')]

val GameStatusEnum.localizedName: String
    @Composable
    get() = gameStatusStringResource(name)?.let { stringResource(it) }
        ?: name.ifEmpty { stringResource(Res.string.game_status__unknown) }

val GameStatus.localizedName: String
    @Composable
    get() = gameStatusStringResource(status)?.let { stringResource(it) }
        ?: status.ifEmpty { stringResource(Res.string.game_status__unknown) }
