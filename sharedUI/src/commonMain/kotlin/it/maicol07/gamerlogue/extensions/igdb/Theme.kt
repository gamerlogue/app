package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import at.released.igdbclient.model.Theme
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.theme__4x
import gamerlogue.sharedui.generated.resources.theme__action
import gamerlogue.sharedui.generated.resources.theme__business
import gamerlogue.sharedui.generated.resources.theme__comedy
import gamerlogue.sharedui.generated.resources.theme__drama
import gamerlogue.sharedui.generated.resources.theme__educational
import gamerlogue.sharedui.generated.resources.theme__erotic
import gamerlogue.sharedui.generated.resources.theme__fantasy
import gamerlogue.sharedui.generated.resources.theme__historical
import gamerlogue.sharedui.generated.resources.theme__horror
import gamerlogue.sharedui.generated.resources.theme__kids
import gamerlogue.sharedui.generated.resources.theme__mystery
import gamerlogue.sharedui.generated.resources.theme__non_fiction
import gamerlogue.sharedui.generated.resources.theme__open_world
import gamerlogue.sharedui.generated.resources.theme__party
import gamerlogue.sharedui.generated.resources.theme__romance
import gamerlogue.sharedui.generated.resources.theme__sandbox
import gamerlogue.sharedui.generated.resources.theme__science_fiction
import gamerlogue.sharedui.generated.resources.theme__stealth
import gamerlogue.sharedui.generated.resources.theme__survival
import gamerlogue.sharedui.generated.resources.theme__thriller
import gamerlogue.sharedui.generated.resources.theme__unknown
import gamerlogue.sharedui.generated.resources.theme__warfare
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Book4W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.BusinessCenterW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CalendarMonthW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CelebrationW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ComedyMaskW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ExploreW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HomeW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.InfoW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LightbulbW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LocalFireDepartmentW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.NewsstandW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonHeartW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SearchW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SettingsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarShineW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.UpcomingW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ExplosionW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FamilyStarW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Grid4x4W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HistoryW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LipsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.MysteryW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PartnerHeartW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlaygroundW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RocketW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SchoolW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SkeletonW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SwordsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.TheaterComedyW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.WandStarsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.mdi.icons.KnifeMdi
import io.github.kingsword09.symbolcraft.symbols.icons.mdi.icons.NinjaMdi
import io.github.kingsword09.symbolcraft.symbols.icons.mdi.icons.TankMdi
import io.github.kingsword09.symbolcraft.symbols.icons.mdi.icons.UnicornVariantMdi
import io.github.kingsword09.symbolcraft.symbols.icons.mdi.Icons as IconsMdi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val Theme.localizedName: String
    @Composable
    get() {
        @Suppress("MagicNumber")
        val s = when (id.toInt()) {
            1 -> Res.string.theme__action
            17 -> Res.string.theme__fantasy
            18 -> Res.string.theme__science_fiction
            19 -> Res.string.theme__horror
            20 -> Res.string.theme__thriller
            21 -> Res.string.theme__survival
            22 -> Res.string.theme__historical
            23 -> Res.string.theme__stealth
            27 -> Res.string.theme__comedy
            28 -> Res.string.theme__business
            31 -> Res.string.theme__drama
            32 -> Res.string.theme__non_fiction
            33 -> Res.string.theme__sandbox
            34 -> Res.string.theme__educational
            35 -> Res.string.theme__kids
            38 -> Res.string.theme__open_world
            39 -> Res.string.theme__warfare
            40 -> Res.string.theme__party
            41 -> Res.string.theme__4x
            42 -> Res.string.theme__erotic
            43 -> Res.string.theme__mystery
            44 -> Res.string.theme__romance
            else -> this.name.ifEmpty { Res.string.theme__unknown }
        }
        return if (s is StringResource) stringResource(s) else s.toString()
    }

val Theme.icon: ImageVector?
    get() = when (id.toInt()) {
        1 -> Icons.ExplosionW500Rounded // Action
        17 -> IconsMdi.UnicornVariantMdi // Fantasy
        18 -> Icons.RocketW500Rounded // Sci-fi
        19 -> Icons.SkeletonW500Rounded // Horror
        20 -> IconsMdi.KnifeMdi // Thriller
        21 -> Icons.PersonHeartW500Rounded // Survival (health/heart)
        22 -> Icons.HistoryW500Rounded // Historical
        23 -> IconsMdi.NinjaMdi // Stealth
        27 -> Icons.ComedyMaskW500Rounded // Comedy
        28 -> Icons.BusinessCenterW500Rounded // Business
        31 -> Icons.TheaterComedyW500Rounded // Drama
        32 -> Icons.InfoW500Rounded // Non-fiction
        33 -> Icons.PlaygroundW500Rounded // Sandbox
        34 -> Icons.SchoolW500Rounded // Educational
        35 -> Icons.FamilyStarW500Rounded // Kids
        38 -> Icons.ExploreW500Rounded // Open world
        39 -> IconsMdi.TankMdi // Warfare
        40 -> Icons.CelebrationW500Rounded // Party (event)
        41 -> Icons.Grid4x4W500Rounded // 4X
        42 -> Icons.LipsW500Rounded // Erotic
        43 -> Icons.MysteryW500Rounded // Mystery
        44 -> Icons.PartnerHeartW500Rounded // Romance
        else -> null
    }
