package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__game_modes_title
import gamerlogue.sharedui.generated.resources.game__genres_title
import gamerlogue.sharedui.generated.resources.game__keywords_title
import gamerlogue.sharedui.generated.resources.game__multiplayer_title
import gamerlogue.sharedui.generated.resources.game__player_perspectives_title
import gamerlogue.sharedui.generated.resources.game__themes_title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.icon
import it.maicol07.gamerlogue.extensions.igdb.localizedName
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/** Keywords are a long tail; only the most relevant ones are worth the vertical space. */
private const val MaxKeywords = 15

@Composable
internal fun GameGenresAndThemes(game: Game) {
    if (game.genres.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__genres_title)) {
            for (genre in game.genres) {
                IconChip(genre.localizedName, genre.icon)
            }
        }
    }
    if (game.themes.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__themes_title)) {
            for (theme in game.themes) {
                IconChip(theme.localizedName, theme.icon)
            }
        }
    }
    if (game.game_modes.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__game_modes_title)) {
            for (mode in game.game_modes) {
                IconChip(mode.localizedName, mode.icon)
            }
        }
    }
    if (game.player_perspectives.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__player_perspectives_title)) {
            for (perspective in game.player_perspectives) {
                IconChip(perspective.localizedName, perspective.icon)
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
                if (mode.onlinecoop == true) {
                    val count = mode.onlinecoopmax?.takeIf { it > 0 }
                    add("Co-Op Online" + if (count != null) " (fino a $count giocatori)" else "")
                }
                if (mode.offlinecoop == true) {
                    val count = mode.offlinecoopmax?.takeIf { it > 0 }
                    add("Co-Op Locale" + if (count != null) " (fino a $count giocatori)" else "")
                }
                if (mode.campaigncoop == true) {
                    add("Co-Op Campagna")
                }
                if (mode.splitscreen == true) {
                    add("Schermo Condiviso (Split Screen)")
                }
                if (mode.lancoop == true) {
                    add("Co-Op LAN")
                }
                if (mode.dropin == true) {
                    add("Co-Op Drop-in/Drop-out")
                }
                if (mode.onlinemax != null && mode.onlinemax > 1) {
                    add("Multiplayer Online (fino a ${mode.onlinemax} giocatori)")
                }
                if (mode.offlinemax != null && mode.offlinemax > 1) {
                    add("Multiplayer Locale (fino a ${mode.offlinemax} giocatori)")
                }
            }
        }.distinct()
    }
    if (details.isEmpty()) return

    ChipSection(stringResource(Res.string.game__multiplayer_title)) {
        for (detail in details) {
            IconChip(detail, Icons.JoystickW500Rounded)
        }
    }
}

@Composable
internal fun GameKeywords(game: Game) {
    if (game.keywords.isEmpty()) return

    val items = remember(game) {
        game.keywords.mapNotNull { keyword -> keyword.name.takeIf { !it.isNullOrBlank() } }.distinct()
    }
    if (items.isEmpty()) return

    ChipSection(stringResource(Res.string.game__keywords_title)) {
        for (keyword in items.take(MaxKeywords)) {
            AssistChip(onClick = {}, label = { Text("#$keyword") })
        }
    }
}

@Composable
private fun IconChip(label: String, icon: ImageVector?) {
    AssistChip(
        onClick = {},
        leadingIcon = icon?.let { { Icon(it, contentDescription = null) } },
        label = { Text(label) }
    )
}

/** Title plus a wrapping row of chips, the shape every taxonomy section uses. */
@Composable
internal fun ChipSection(title: String, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}
