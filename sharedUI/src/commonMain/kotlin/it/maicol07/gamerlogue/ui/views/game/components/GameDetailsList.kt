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
import gamerlogue.sharedui.generated.resources.game__franchises_title
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
import it.maicol07.gamerlogue.ui.views.list.CompanyRole
import it.maicol07.gamerlogue.ui.views.list.GameListPreset
import it.maicol07.gamerlogue.ui.views.list.GameListPresetType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val MAX_DETAIL_COLUMNS = 3

private val THREE_COLUMN_MIN_WIDTH = 900.dp
private val TWO_COLUMN_MIN_WIDTH = 500.dp

private enum class GameDetailSheet { RELEASE_DATES, ALTERNATIVE_NAMES, LANGUAGES }

@Composable
fun GameDetailsList(
    game: Game,
    onGameClick: (Game) -> Unit,
    onPresetClick: (GameListPreset) -> Unit
) = Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = SectionSpacing)) {
    var sheet by remember { mutableStateOf<GameDetailSheet?>(null) }
    var choice by remember { mutableStateOf<MetadataChoice?>(null) }
    val onSheetClick: (GameDetailSheet) -> Unit = { sheet = it }
    val actions = PresetActions(onPresetClick) { choice = it }
    val details = gameInfoEntries(game, onSheetClick) +
        remember(game, onGameClick, onPresetClick) {
            gameCompanyEntries(game, actions) + gameRelatedEntries(game, onGameClick, actions)
        }

    GameDetailsGrid(details)

    when (sheet) {
        GameDetailSheet.RELEASE_DATES -> ReleaseDatesBottomSheet(
            game,
            onPlatformClick = {
                sheet = null
                onPresetClick(it)
            },
            onDismissRequest = { sheet = null }
        )
        GameDetailSheet.ALTERNATIVE_NAMES -> AlternativeNamesBottomSheet(game) { sheet = null }
        GameDetailSheet.LANGUAGES -> LanguagesBottomSheet(game) { sheet = null }
        null -> {}
    }
    choice?.let { current ->
        MetadataChooserSheet(
            current,
            onPick = {
                choice = null
                onPresetClick(it)
            },
            onDismissRequest = { choice = null }
        )
    }
}

/**
 * Click handling for a metadata row: a single value opens the games filtered on it directly,
 * several values open [MetadataChooserSheet] to pick one.
 */
private class PresetActions(val onPresetClick: (GameListPreset) -> Unit, val onChoose: (MetadataChoice) -> Unit) {
    fun forChoice(choice: MetadataChoice): () -> Unit {
        val single = choice.options.singleOrNull()
        return if (single != null) {
            { onPresetClick(single) }
        } else {
            { onChoose(choice) }
        }
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

private fun gameCompanyEntries(game: Game, actions: PresetActions): List<GameDetailEntry> = listOfNotNull(
    companyEntry(game, CompanyRole.DEVELOPER, Icons.CodeW500Rounded, Res.string.game__details_developers, actions),
    companyEntry(game, CompanyRole.PUBLISHER, Icons.PublishW500Rounded, Res.string.game__details_publishers, actions),
)

private fun companyEntry(
    game: Game,
    role: CompanyRole,
    icon: ImageVector,
    headline: StringResource,
    actions: PresetActions
): GameDetailEntry? {
    val type = if (role == CompanyRole.DEVELOPER) GameListPresetType.DEVELOPER else GameListPresetType.PUBLISHER
    val companies = game.involved_companies
        .filter { if (role == CompanyRole.DEVELOPER) it.developer else it.publisher }
        .mapNotNull { it.company }
        .filter { it.name.isNotBlank() }
        .distinctBy { it.id }
    if (companies.isEmpty()) return null
    return presetEntry(
        icon,
        headline,
        companies.joinToString { it.name },
        MetadataChoice(headline, icon, companies.map { GameListPreset(type, it.id.toInt(), it.name) }),
        actions
    )
}

/** A row listing [supporting], tappable to open the games filtered on one of the [choice] values. */
private fun presetEntry(
    icon: ImageVector,
    headline: StringResource,
    supporting: String,
    choice: MetadataChoice,
    actions: PresetActions
) = GameDetailEntry(
    leadingIcon = icon,
    headline = headline,
    supporting = supporting,
    trailingIcon = Icons.KeyboardArrowRightW500Rounded,
    onClick = actions.forChoice(choice)
)

private fun gameRelatedEntries(
    game: Game,
    onGameClick: (Game) -> Unit,
    actions: PresetActions
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
    game.game_engines.filter { it.name.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { engines ->
        presetEntry(
            Icons.LayersW500Rounded,
            Res.string.game__details_engines,
            engines.joinToString { it.name },
            MetadataChoice(
                Res.string.game__details_engines,
                Icons.LayersW500Rounded,
                engines.map { GameListPreset(GameListPresetType.ENGINE, it.id.toInt(), it.name) }
            ),
            actions
        )
    },
    (game.franchises + listOfNotNull(game.franchise)).filter { it.name.isNotBlank() }.distinctBy { it.id }
        .takeIf { it.isNotEmpty() }?.let { franchises ->
            val first = franchises.first().name
            presetEntry(
                Icons.CategoryW500Rounded,
                Res.string.game__details_franchise,
                if (franchises.size > 1) "${franchises.size} franchises ($first...)" else first,
                MetadataChoice(
                    Res.string.game__franchises_title,
                    Icons.CategoryW500Rounded,
                    franchises.map { GameListPreset(GameListPresetType.FRANCHISE, it.id.toInt(), it.name) }
                ),
                actions
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
private fun GameDetailRow(detail: GameDetailEntry, first: Boolean, last: Boolean) {
    val shape = ListItemDefaults.expressiveShape(first, last)
    ListItem(
        modifier = Modifier.clip(shape)
            .let { modifier -> detail.onClick?.let { modifier.clickable(onClick = it) } ?: modifier },
        leadingContent = { Icon(detail.leadingIcon, contentDescription = null) },
        supportingContent = detail.supporting?.let { { Text(it) } },
        trailingContent = detail.trailingIcon?.let { { Icon(it, contentDescription = null) } },
        shapes = ListItemDefaults.shapes(shape = shape),
        colors = ListItemDefaults.expressiveSegmentedColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) { Text(stringResource(detail.headline)) }
}

private data class GameDetailEntry(
    val leadingIcon: ImageVector,
    val headline: StringResource,
    val supporting: String? = null,
    val trailingIcon: ImageVector? = null,
    val onClick: (() -> Unit)? = null
)
