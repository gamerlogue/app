package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.ReleaseDate
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__alternative_names_title
import gamerlogue.sharedui.generated.resources.game__details_collection
import gamerlogue.sharedui.generated.resources.game__details_developers
import gamerlogue.sharedui.generated.resources.game__details_engines
import gamerlogue.sharedui.generated.resources.game__details_franchise
import gamerlogue.sharedui.generated.resources.game__details_languages
import gamerlogue.sharedui.generated.resources.game__details_parent_game
import gamerlogue.sharedui.generated.resources.game__details_publishers
import gamerlogue.sharedui.generated.resources.game__details_release_date
import gamerlogue.sharedui.generated.resources.game__details_status
import gamerlogue.sharedui.generated.resources.game__details_type
import gamerlogue.sharedui.generated.resources.game__languages_count
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Book4W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CalendarMonthW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CategoryW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CodeW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.InfoW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Inventory2W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LanguageW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LayersW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PublishW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveShape
import it.maicol07.gamerlogue.extensions.igdb.displayDate
import it.maicol07.gamerlogue.extensions.igdb.localizedName
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val MAX_DETAIL_COLUMNS = 3

private val THREE_COLUMN_MIN_WIDTH = 900.dp
private val TWO_COLUMN_MIN_WIDTH = 500.dp

private enum class GameDetailSheet { RELEASE_DATES, ALTERNATIVE_NAMES, FRANCHISES, LANGUAGES }

@Composable
fun GameDetailsList(game: Game, onGameClick: (Game) -> Unit) = Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    var sheet by remember { mutableStateOf<GameDetailSheet?>(null) }
    val franchiseNames = remember(game) {
        (game.franchises.map { it.name } + listOfNotNull(game.franchise?.name)).filter { it.isNotBlank() }.distinct()
    }
    val onSheetClick: (GameDetailSheet) -> Unit = { sheet = it }
    val details = gameInfoEntries(game, onSheetClick) +
        remember(game, franchiseNames, onGameClick) {
            gameCompanyEntries(game) + gameRelatedEntries(game, franchiseNames, onGameClick, onSheetClick)
        }

    GameDetailsGrid(details)

    when (sheet) {
        GameDetailSheet.RELEASE_DATES -> ReleaseDatesBottomSheet(game) { sheet = null }
        GameDetailSheet.ALTERNATIVE_NAMES -> AlternativeNamesBottomSheet(game) { sheet = null }
        GameDetailSheet.FRANCHISES -> FranchisesBottomSheet(franchiseNames) { sheet = null }
        GameDetailSheet.LANGUAGES -> LanguagesBottomSheet(game) { sheet = null }
        null -> {}
    }
}

@Composable
private fun gameInfoEntries(game: Game, onSheetClick: (GameDetailSheet) -> Unit): List<GameDetailEntry> = listOfNotNull(
    GameDetailEntry(
        leadingIcon = Icons.CalendarMonthW500Rounded,
        headline = Res.string.game__details_release_date,
        supporting = ReleaseDate(date = game.first_release_date).displayDate(),
        trailingIcon = Icons.InfoW500Rounded,
        onClick = { onSheetClick(GameDetailSheet.RELEASE_DATES) }
    ),
    game.game_status?.localizedName?.takeIf { it.isNotBlank() }?.let {
        GameDetailEntry(Icons.InfoW500Rounded, Res.string.game__details_status, it)
    },
    game.game_type?.localizedName?.takeIf { it.isNotBlank() }?.let {
        GameDetailEntry(Icons.CategoryW500Rounded, Res.string.game__details_type, it)
    },
    game.language_supports.takeIf { it.isNotEmpty() }?.let { supports ->
        val count = supports.mapNotNull { it.language?.name }.distinct().size
        GameDetailEntry(
            leadingIcon = Icons.LanguageW500Rounded,
            headline = Res.string.game__details_languages,
            supporting = pluralStringResource(Res.plurals.game__languages_count, count, count),
            trailingIcon = Icons.InfoW500Rounded,
            onClick = { onSheetClick(GameDetailSheet.LANGUAGES) }
        )
    },
    game.alternative_names.firstOrNull()?.let { first ->
        val count = game.alternative_names.size
        GameDetailEntry(
            leadingIcon = Icons.Book4W500Rounded,
            headline = Res.string.game__alternative_names_title,
            supporting = if (count == 1) first.name else "$count (${first.name}...)",
            trailingIcon = Icons.InfoW500Rounded,
            onClick = { onSheetClick(GameDetailSheet.ALTERNATIVE_NAMES) }
        )
    }
)

private fun gameCompanyEntries(game: Game): List<GameDetailEntry> = listOfNotNull(
    game.involved_companies.filter { it.developer }.takeIf { it.isNotEmpty() }?.let { companies ->
        GameDetailEntry(
            leadingIcon = Icons.CodeW500Rounded,
            headline = Res.string.game__details_developers,
            supporting = companies.joinToString { it.company?.name ?: "N/A" }
        )
    },
    game.involved_companies.filter { it.publisher }.takeIf { it.isNotEmpty() }?.let { companies ->
        GameDetailEntry(
            leadingIcon = Icons.PublishW500Rounded,
            headline = Res.string.game__details_publishers,
            supporting = companies.joinToString { it.company?.name ?: "N/A" }
        )
    }
)

private fun gameRelatedEntries(
    game: Game,
    franchiseNames: List<String>,
    onGameClick: (Game) -> Unit,
    onSheetClick: (GameDetailSheet) -> Unit
): List<GameDetailEntry> = listOfNotNull(
    listOfNotNull(game.parent_game, game.version_parent).distinctBy { it.id }.singleOrNull()?.let { parent ->
        GameDetailEntry(
            leadingIcon = Icons.JoystickW500Rounded,
            headline = Res.string.game__details_parent_game,
            supporting = parent.name,
            trailingIcon = Icons.KeyboardArrowRightW500Rounded,
            onClick = { onGameClick(parent) }
        )
    },
    game.game_engines.map { it.name }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let {
        GameDetailEntry(Icons.LayersW500Rounded, Res.string.game__details_engines, it.joinToString())
    },
    franchiseNames.firstOrNull()?.let { first ->
        val multiple = franchiseNames.size > 1
        GameDetailEntry(
            leadingIcon = Icons.CategoryW500Rounded,
            headline = Res.string.game__details_franchise,
            supporting = if (multiple) "${franchiseNames.size} franchises ($first...)" else first,
            trailingIcon = if (multiple) Icons.InfoW500Rounded else null,
            onClick = if (multiple) {
                { onSheetClick(GameDetailSheet.FRANCHISES) }
            } else {
                null
            }
        )
    },
    game.collections.map { it.name }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let {
        GameDetailEntry(Icons.Inventory2W500Rounded, Res.string.game__details_collection, it.joinToString())
    }
)

@Composable
private fun GameDetailsGrid(details: List<GameDetailEntry>) = BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val columnsCount = when {
        maxWidth >= THREE_COLUMN_MIN_WIDTH -> MAX_DETAIL_COLUMNS
        maxWidth >= TWO_COLUMN_MIN_WIDTH -> 2
        else -> 1
    }
    val columnItems = remember(details, columnsCount) {
        val columns = List(columnsCount) { mutableListOf<GameDetailEntry>() }
        details.forEachIndexed { index, detail -> columns[index % columnsCount].add(detail) }
        columns
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        for (items in columnItems) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items.forEachIndexed { index, detail -> GameDetailRow(detail, index == 0, index == items.lastIndex) }
            }
        }
    }
}

@Composable
private fun GameDetailRow(detail: GameDetailEntry, first: Boolean, last: Boolean) = ListItem(
    modifier = Modifier.clip(ListItemDefaults.expressiveShape(first, last))
        .let { modifier -> detail.onClick?.let { modifier.clickable(onClick = it) } ?: modifier },
    leadingContent = { Icon(detail.leadingIcon, contentDescription = null) },
    headlineContent = { Text(stringResource(detail.headline)) },
    supportingContent = detail.supporting?.let { { Text(it) } },
    trailingContent = detail.trailingIcon?.let { { Icon(it, contentDescription = null) } },
    colors = ListItemDefaults.expressiveSegmentedColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
)

private data class GameDetailEntry(
    val leadingIcon: ImageVector,
    val headline: StringResource,
    val supporting: String? = null,
    val trailingIcon: ImageVector? = null,
    val onClick: (() -> Unit)? = null
)
