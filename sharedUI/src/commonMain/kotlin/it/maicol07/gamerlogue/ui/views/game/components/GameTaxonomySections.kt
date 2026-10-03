package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__game_modes_title
import gamerlogue.sharedui.generated.resources.game__genres_title
import gamerlogue.sharedui.generated.resources.game__keywords_title
import gamerlogue.sharedui.generated.resources.game__multiplayer_title
import gamerlogue.sharedui.generated.resources.game__player_perspectives_title
import gamerlogue.sharedui.generated.resources.game__themes_title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CategoryW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.GroupW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StyleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.TagW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.VisibilityW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.icon
import it.maicol07.gamerlogue.extensions.igdb.localizedName
import org.jetbrains.compose.resources.stringResource

/** Keywords are a long tail; only the most relevant ones are worth the vertical space. */
private const val MaxKeywords = 15

@Composable
internal fun GameGenresAndThemes(game: Game) {
    if (game.genres.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__genres_title), Icons.CategoryW500Rounded) {
            for (genre in game.genres) {
                InfoChip(genre.localizedName, genre.icon)
            }
        }
    }
    if (game.themes.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__themes_title), Icons.StyleW500Rounded) {
            for (theme in game.themes) {
                InfoChip(theme.localizedName, theme.icon)
            }
        }
    }
    if (game.game_modes.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__game_modes_title), Icons.JoystickW500Rounded) {
            for (mode in game.game_modes) {
                InfoChip(mode.localizedName, mode.icon)
            }
        }
    }
    if (game.player_perspectives.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__player_perspectives_title), Icons.VisibilityW500Rounded) {
            for (perspective in game.player_perspectives) {
                InfoChip(perspective.localizedName, perspective.icon)
            }
        }
    }
}

@Composable
internal fun GameMultiplayerDetails(game: Game) {
    if (game.multiplayer_modes.isEmpty()) return

    // ponytail: the labels below are hardcoded Italian; they need string resources with a player
    // count argument before this section reads correctly in any other language.
    val details = remember(game) {
        buildList {
            for (mode in game.multiplayer_modes) {
                if (mode.onlinecoop) add("Co-Op Online" + playerLimit(mode.onlinecoopmax))
                if (mode.offlinecoop) add("Co-Op Locale" + playerLimit(mode.offlinecoopmax))
                if (mode.campaigncoop) add("Co-Op Campagna")
                if (mode.splitscreen) add("Schermo Condiviso (Split Screen)")
                if (mode.lancoop) add("Co-Op LAN")
                if (mode.dropin) add("Co-Op Drop-in/Drop-out")
                if (mode.onlinemax > 1) add("Multiplayer Online" + playerLimit(mode.onlinemax))
                if (mode.offlinemax > 1) add("Multiplayer Locale" + playerLimit(mode.offlinemax))
            }
        }.distinct()
    }
    if (details.isEmpty()) return

    ChipSection(stringResource(Res.string.game__multiplayer_title), Icons.GroupW500Rounded) {
        for (detail in details) {
            InfoChip(detail, icon = null)
        }
    }
}

/** IGDB uses 0 for "unknown": no limit suffix then. */
private fun playerLimit(count: Int) = if (count > 0) " (fino a $count giocatori)" else ""

@Composable
internal fun GameKeywords(game: Game) {
    if (game.keywords.isEmpty()) return

    val items = remember(game) {
        game.keywords.mapNotNull { keyword -> keyword.name.takeIf { it.isNotBlank() } }.distinct()
    }
    if (items.isEmpty()) return

    ChipSection(stringResource(Res.string.game__keywords_title), Icons.TagW500Rounded) {
        for (keyword in items.take(MaxKeywords)) {
            Text(
                "#$keyword",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
