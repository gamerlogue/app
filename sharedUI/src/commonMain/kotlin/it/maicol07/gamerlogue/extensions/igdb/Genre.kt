package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import at.released.igdbclient.model.Genre
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.genre__fighting
import gamerlogue.sharedui.generated.resources.genre__music
import gamerlogue.sharedui.generated.resources.genre__platform
import gamerlogue.sharedui.generated.resources.genre__point_and_click
import gamerlogue.sharedui.generated.resources.genre__puzzle
import gamerlogue.sharedui.generated.resources.genre__racing
import gamerlogue.sharedui.generated.resources.genre__rpg
import gamerlogue.sharedui.generated.resources.genre__rts
import gamerlogue.sharedui.generated.resources.genre__simulation
import gamerlogue.sharedui.generated.resources.genre__sport
import gamerlogue.sharedui.generated.resources.genre__strategy
import gamerlogue.sharedui.generated.resources.genre__tbs
import gamerlogue.sharedui.generated.resources.genre__tactical
import gamerlogue.sharedui.generated.resources.genre__hack_and_slash
import gamerlogue.sharedui.generated.resources.genre__quiz
import gamerlogue.sharedui.generated.resources.genre__pinball
import gamerlogue.sharedui.generated.resources.genre__adventure
import gamerlogue.sharedui.generated.resources.genre__indie
import gamerlogue.sharedui.generated.resources.genre__arcade
import gamerlogue.sharedui.generated.resources.genre__visual_novel
import gamerlogue.sharedui.generated.resources.genre__card_board
import gamerlogue.sharedui.generated.resources.genre__moba
import gamerlogue.sharedui.generated.resources.genre__shooter
import gamerlogue.sharedui.generated.resources.genre__unknown
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Book4W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ConversionPathW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ExploreW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FlutterDashW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LayersW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.MusicNoteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlayingCardsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.QuizW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SimulationW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SportsAndOutdoorsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SportsBaseballW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SportsMartialArtsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SportsMotorsportsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StadiumW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StrategyW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SwordRoseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SwordsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.TacticW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ToysAndGamesW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.WandStarsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.WebTrafficW500Rounded
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val Genre.localizedName: String
    @Composable
    get() {
        val s = when (id.toInt()) {
            2 -> Res.string.genre__point_and_click
            4 -> Res.string.genre__fighting
            5 -> Res.string.genre__shooter
            7 -> Res.string.genre__music
            8 -> Res.string.genre__platform
            9 -> Res.string.genre__puzzle
            10 -> Res.string.genre__racing
            11 -> Res.string.genre__rts
            12 -> Res.string.genre__rpg
            13 -> Res.string.genre__simulation
            14 -> Res.string.genre__sport
            15 -> Res.string.genre__strategy
            16 -> Res.string.genre__tbs
            24 -> Res.string.genre__tactical
            25 -> Res.string.genre__hack_and_slash
            26 -> Res.string.genre__quiz
            30 -> Res.string.genre__pinball
            31 -> Res.string.genre__adventure
            32 -> Res.string.genre__indie
            33 -> Res.string.genre__arcade
            34 -> Res.string.genre__visual_novel
            35 -> Res.string.genre__card_board
            36 -> Res.string.genre__moba
            else -> this.name.ifEmpty { Res.string.genre__unknown }
        }
        return if (s is StringResource) stringResource(s) else s.toString()
    }

val Genre.icon: ImageVector?
    get() = when (id.toInt()) {
        2 -> Icons.WebTrafficW500Rounded
        4 -> Icons.SportsMartialArtsW500Rounded
//        5 -> Icons.PistolW500Rounded
        7 -> Icons.MusicNoteW500Rounded
        8 -> Icons.LayersW500Rounded
        9 -> Icons.ToysAndGamesW500Rounded
        10 -> Icons.SportsMotorsportsW500Rounded
        11 -> Icons.ConversionPathW500Rounded
        12 -> Icons.WandStarsW500Rounded
        13 -> Icons.SimulationW500Rounded
        14 -> Icons.SportsAndOutdoorsW500Rounded
        15 -> Icons.StrategyW500Rounded
        16 -> Icons.SwordRoseW500Rounded
        24 -> Icons.TacticW500Rounded
        25 -> Icons.SwordsW500Rounded
        26 -> Icons.QuizW500Rounded
        30 -> Icons.SportsBaseballW500Rounded
        31 -> Icons.ExploreW500Rounded
        32 -> Icons.FlutterDashW500Rounded
        33 -> Icons.JoystickW500Rounded
        34 -> Icons.Book4W500Rounded
        35 -> Icons.PlayingCardsW500Rounded
        36 -> Icons.StadiumW500Rounded
        else -> null
    }

