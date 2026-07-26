package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import at.released.igdbclient.model.GameMode
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game_mode__battle_royale
import gamerlogue.sharedui.generated.resources.game_mode__cooperative
import gamerlogue.sharedui.generated.resources.game_mode__mmo
import gamerlogue.sharedui.generated.resources.game_mode__multiplayer
import gamerlogue.sharedui.generated.resources.game_mode__single_player
import gamerlogue.sharedui.generated.resources.game_mode__split_screen
import gamerlogue.sharedui.generated.resources.game_mode__unknown
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FamilyStarW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Grid4x4W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SwordsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.WebTrafficW500Rounded
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val GameMode.localizedName: String
    @Composable
    get() {
        val s = when (id.toInt()) {
            1 -> Res.string.game_mode__single_player
            2 -> Res.string.game_mode__multiplayer
            3 -> Res.string.game_mode__cooperative
            4 -> Res.string.game_mode__split_screen
            5 -> Res.string.game_mode__mmo
            6 -> Res.string.game_mode__battle_royale
            else -> this.name.ifEmpty { Res.string.game_mode__unknown }
        }
        return if (s is StringResource) stringResource(s) else s.toString()
    }

val GameMode.icon: ImageVector?
    get() = when (id.toInt()) {
        1 -> Icons.PersonW500Rounded
        2 -> Icons.JoystickW500Rounded
        3 -> Icons.FamilyStarW500Rounded
        4 -> Icons.Grid4x4W500Rounded
        5 -> Icons.WebTrafficW500Rounded
        6 -> Icons.SwordsW500Rounded
        else -> null
    }
