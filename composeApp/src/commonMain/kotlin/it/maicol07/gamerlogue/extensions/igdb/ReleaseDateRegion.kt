package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.runtime.Composable
import at.released.igdbclient.model.ReleaseDateRegion
import gamerlogue.composeapp.generated.resources.Res
import gamerlogue.composeapp.generated.resources.region__asia
import gamerlogue.composeapp.generated.resources.region__australia
import gamerlogue.composeapp.generated.resources.region__brazil
import gamerlogue.composeapp.generated.resources.region__china
import gamerlogue.composeapp.generated.resources.region__europe
import gamerlogue.composeapp.generated.resources.region__japan
import gamerlogue.composeapp.generated.resources.region__korea
import gamerlogue.composeapp.generated.resources.region__new_zealand
import gamerlogue.composeapp.generated.resources.region__north_america
import gamerlogue.composeapp.generated.resources.region__unknown
import gamerlogue.composeapp.generated.resources.region__worldwide
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val ReleaseDateRegion.localizedName: String
    @Composable
    get() {
        val s = when (id.toInt()) {
            1 -> Res.string.region__europe
            2 -> Res.string.region__north_america
            3 -> Res.string.region__australia
            4 -> Res.string.region__new_zealand
            5 -> Res.string.region__japan
            6 -> Res.string.region__china
            7 -> Res.string.region__asia
            8 -> Res.string.region__worldwide
            9 -> Res.string.region__korea
            10 -> Res.string.region__brazil
            else -> region.ifEmpty { Res.string.region__unknown }
        }
        return if (s is StringResource) stringResource(s) else s.toString()
    }
