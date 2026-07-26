package it.maicol07.gamerlogue.ui.components.game

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.model.Platform
import at.released.igdbclient.util.igdbImageUrl
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.AndroidSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.AppleSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.AtariSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.CommodoreSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.EpicgamesSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.GogdotcomSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.IosSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.LinuxSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.MacosSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.MetaSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.OculusSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.Playstation2SimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.Playstation3SimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.Playstation4SimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.Playstation5SimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.PlaystationSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.PlaystationvitaSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.SegaSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.SteamSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.SteamdeckSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.UbisoftSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.icons.WindowsSvgl
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.icons.XboxSvgl
import it.maicol07.gamerlogue.ui.components.RemoteImage
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons as MaterialSymbols
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.Icons as SimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.Icons as SvglIcons

/**
 * Returns the [ImageVector] for the [Platform] if available from SimpleIcons / SVGL vector icon sets.
 */
fun Platform.vectorIcon(): ImageVector? {
    val platformName = name
    val pSlug = slug.orEmpty()

    return when {
        // PlayStation family
        platformName.contains("PlayStation 5", ignoreCase = true) || pSlug.contains("ps5") -> SimpleIcons.Playstation5SimpleIcons
        platformName.contains("PlayStation 4", ignoreCase = true) || pSlug.contains("ps4") -> SimpleIcons.Playstation4SimpleIcons
        platformName.contains("PlayStation 3", ignoreCase = true) || pSlug.contains("ps3") -> SimpleIcons.Playstation3SimpleIcons
        platformName.contains("PlayStation 2", ignoreCase = true) || pSlug.contains("ps2") -> SimpleIcons.Playstation2SimpleIcons
        platformName.contains("Vita", ignoreCase = true) || pSlug.contains("psvita") -> SimpleIcons.PlaystationvitaSimpleIcons
        platformName.contains("PlayStation", ignoreCase = true) || platformName.contains("PSP", ignoreCase = true) ||
            pSlug.contains("ps") || pSlug.contains("psp") -> SimpleIcons.PlaystationSimpleIcons

        // Xbox family
        platformName.contains("Xbox", ignoreCase = true) || pSlug.contains("xbox") -> SvglIcons.XboxSvgl

        // PC / Windows
        platformName.contains("PC", ignoreCase = true) || platformName.contains("Windows", ignoreCase = true) ||
            pSlug.contains("win") -> SvglIcons.WindowsSvgl

        // Apple / Mac / iOS
        platformName.contains("Mac", ignoreCase = true) || pSlug.contains("mac") -> SimpleIcons.MacosSimpleIcons
        platformName.contains("iOS", ignoreCase = true) || pSlug.contains("ios") -> SimpleIcons.IosSimpleIcons
        platformName.contains("Apple", ignoreCase = true) -> SimpleIcons.AppleSimpleIcons

        // Linux
        platformName.contains("Linux", ignoreCase = true) || pSlug.contains("linux") -> SimpleIcons.LinuxSimpleIcons

        // Android
        platformName.contains("Android", ignoreCase = true) || pSlug.contains("android") -> SimpleIcons.AndroidSimpleIcons

        // Steam / Steam Deck
        platformName.contains("Steam Deck", ignoreCase = true) || pSlug.contains("steamdeck") -> SimpleIcons.SteamdeckSimpleIcons
        platformName.contains("Steam", ignoreCase = true) -> SimpleIcons.SteamSimpleIcons

        // Epic Games / GOG / Ubisoft
        platformName.contains("Epic Games", ignoreCase = true) || pSlug.contains("epic") -> SimpleIcons.EpicgamesSimpleIcons
        platformName.contains("GOG", ignoreCase = true) || pSlug.contains("gog") -> SimpleIcons.GogdotcomSimpleIcons
        platformName.contains("Ubisoft", ignoreCase = true) || pSlug.contains("ubisoft") -> SimpleIcons.UbisoftSimpleIcons

        // Sega family
        platformName.contains("Sega", ignoreCase = true) || platformName.contains("Genesis", ignoreCase = true) ||
            platformName.contains("Dreamcast", ignoreCase = true) || platformName.contains("Saturn", ignoreCase = true) ||
            platformName.contains("Mega Drive", ignoreCase = true) || platformName.contains("Game Gear", ignoreCase = true) ||
            pSlug.contains("sega") -> SimpleIcons.SegaSimpleIcons

        // Atari family
        platformName.contains("Atari", ignoreCase = true) || pSlug.contains("atari") -> SimpleIcons.AtariSimpleIcons

        // Commodore / Amiga
        platformName.contains("Commodore", ignoreCase = true) || platformName.contains("Amiga", ignoreCase = true) ||
            pSlug.contains("commodore") || pSlug.contains("amiga") -> SimpleIcons.CommodoreSimpleIcons

        // VR / Meta / Oculus
        platformName.contains("Oculus", ignoreCase = true) -> SimpleIcons.OculusSimpleIcons
        platformName.contains("Meta", ignoreCase = true) || platformName.contains("Quest", ignoreCase = true) -> SimpleIcons.MetaSimpleIcons

        else -> null
    }
}

/**
 * Renders the logo for a platform using vector icons from SimpleIcons / SVGL when available,
 * falling back to the IGDB logo image, or a neutral joystick icon.
 */
@Composable
fun Platform.Image(
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier
) {
    val vector = vectorIcon()
    if (vector != null) {
        Icon(
            imageVector = vector,
            contentDescription = name,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        val logoUrl = platform_logo?.let {
            igdbImageUrl(
                it.image_id,
                IgdbImageSize.LOGO_MEDIUM
            )
        }
        if (logoUrl != null) {
            RemoteImage(
                url = logoUrl,
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = modifier,
                loadingModifier = loadingModifier
            )
        } else {
            Icon(
                imageVector = MaterialSymbols.JoystickW500Rounded,
                contentDescription = name,
                modifier = modifier,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
