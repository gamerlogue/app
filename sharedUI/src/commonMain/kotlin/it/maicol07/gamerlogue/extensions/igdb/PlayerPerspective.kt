package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import at.released.igdbclient.model.PlayerPerspective
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.player_perspective__auditory
import gamerlogue.sharedui.generated.resources.player_perspective__first_person
import gamerlogue.sharedui.generated.resources.player_perspective__isometric
import gamerlogue.sharedui.generated.resources.player_perspective__side_view
import gamerlogue.sharedui.generated.resources.player_perspective__text
import gamerlogue.sharedui.generated.resources.player_perspective__third_person
import gamerlogue.sharedui.generated.resources.player_perspective__unknown
import gamerlogue.sharedui.generated.resources.player_perspective__virtual_reality
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Book4W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ConversionPathW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DevicesW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ExploreW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LayersW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.MusicNoteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonW500Rounded
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val PlayerPerspective.localizedName: String
    @Composable
    get() {
        val s = when (id.toInt()) {
            1 -> Res.string.player_perspective__first_person
            2 -> Res.string.player_perspective__third_person
            3 -> Res.string.player_perspective__isometric
            4 -> Res.string.player_perspective__side_view
            5 -> Res.string.player_perspective__text
            6 -> Res.string.player_perspective__auditory
            7 -> Res.string.player_perspective__virtual_reality
            else -> this.name.ifEmpty { Res.string.player_perspective__unknown }
        }
        return if (s is StringResource) stringResource(s) else s.toString()
    }

val PlayerPerspective.icon: ImageVector?
    get() = when (id.toInt()) {
        1 -> Icons.PersonW500Rounded
        2 -> Icons.ExploreW500Rounded
        3 -> Icons.LayersW500Rounded
        4 -> Icons.ConversionPathW500Rounded
        5 -> Icons.Book4W500Rounded
        6 -> Icons.MusicNoteW500Rounded
        7 -> Icons.DevicesW500Rounded
        else -> null
    }
