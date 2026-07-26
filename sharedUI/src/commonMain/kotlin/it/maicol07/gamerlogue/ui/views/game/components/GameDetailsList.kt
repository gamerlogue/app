package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import gamerlogue.sharedui.generated.resources.game__languages_count
import gamerlogue.sharedui.generated.resources.game__details_parent_game
import gamerlogue.sharedui.generated.resources.game__details_publishers
import gamerlogue.sharedui.generated.resources.game__details_release_date
import gamerlogue.sharedui.generated.resources.game__details_status
import gamerlogue.sharedui.generated.resources.game__details_type
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
import org.jetbrains.compose.resources.stringResource

@Composable
fun GameDetailsList(
    game: Game,
    onGameClick: ((Game) -> Unit)? = null
) = Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    var showReleaseDatesSheet by remember { mutableStateOf(false) }
    var showAlternativeNamesSheet by remember { mutableStateOf(false) }
    var showFranchisesSheet by remember { mutableStateOf(false) }
    var showLanguagesSheet by remember { mutableStateOf(false) }

    val franchiseNames = remember(game) {
        (game.franchises.mapNotNull { it.name } + listOfNotNull(game.franchise?.name))
            .filter { it.isNotBlank() }
            .distinct()
    }

    val statusName = game.status?.localizedName
    val categoryName = game.category?.localizedName

    val languagesCount = remember(game.language_supports) {
        game.language_supports.mapNotNull { it.language?.name }.distinct().size
    }
    val languagesText = stringResource(Res.string.game__languages_count, languagesCount)

    val details = remember(game, onGameClick, franchiseNames, statusName, categoryName, languagesText) {
        val firstReleaseDate = ReleaseDate(date = game.first_release_date)
        buildList {
            add(
                GameDetailEntry(
                    leadingIcon = Icons.CalendarMonthW500Rounded,
                    headline = Res.string.game__details_release_date,
                    supporting = firstReleaseDate.displayDate(),
                    trailingIcon = Icons.InfoW500Rounded
                ) { showReleaseDatesSheet = true }
            )
            if (!statusName.isNullOrBlank()) {
                add(
                    GameDetailEntry(
                        leadingIcon = Icons.InfoW500Rounded,
                        headline = Res.string.game__details_status,
                        supporting = statusName
                    )
                )
            }
            if (!categoryName.isNullOrBlank()) {
                add(
                    GameDetailEntry(
                        leadingIcon = Icons.CategoryW500Rounded,
                        headline = Res.string.game__details_type,
                        supporting = categoryName
                    )
                )
            }
            if (game.language_supports.isNotEmpty()) {
                add(
                    GameDetailEntry(
                        leadingIcon = Icons.LanguageW500Rounded,
                        headline = Res.string.game__details_languages,
                        supporting = languagesText,
                        trailingIcon = Icons.InfoW500Rounded
                    ) { showLanguagesSheet = true }
                )
            }
            if (game.alternative_names.isNotEmpty()) {
                val count = game.alternative_names.size
                val firstAlt = game.alternative_names.firstOrNull()?.name ?: ""
                val supportingText = if (count == 1) firstAlt else "$count ($firstAlt...)"
                add(
                    GameDetailEntry(
                        leadingIcon = Icons.Book4W500Rounded,
                        headline = Res.string.game__alternative_names_title,
                        supporting = supportingText,
                        trailingIcon = Icons.InfoW500Rounded
                    ) { showAlternativeNamesSheet = true }
                )
            }
            val developers = game.involved_companies.filter { it.developer }
            if (developers.isNotEmpty()) {
                add(
                    GameDetailEntry(
                        leadingIcon = Icons.CodeW500Rounded,
                        headline = Res.string.game__details_developers,
                        supporting = developers.joinToString { it.company?.name ?: "N/A" }
                    )
                )
            }
            val publishers = game.involved_companies.filter { it.publisher }
            if (publishers.isNotEmpty()) {
                add(
                    GameDetailEntry(
                        leadingIcon = Icons.PublishW500Rounded,
                        headline = Res.string.game__details_publishers,
                        supporting = publishers.joinToString { it.company?.name ?: "N/A" }
                    )
                )
            }
            val parentGames = listOfNotNull(game.parent_game, game.version_parent).distinctBy { it.id }
            if (parentGames.size == 1) {
                val parent = parentGames.first()
                add(
                    GameDetailEntry(
                        leadingIcon = Icons.JoystickW500Rounded,
                        headline = Res.string.game__details_parent_game,
                        supporting = parent.name,
                        trailingIcon = if (onGameClick != null) Icons.KeyboardArrowRightW500Rounded else null,
                        onClick = if (onGameClick != null) { { onGameClick(parent) } } else null
                    )
                )
            }
            if (game.game_engines.isNotEmpty()) {
                val engines = game.game_engines.mapNotNull { it.name }.filter { it.isNotBlank() }
                if (engines.isNotEmpty()) {
                    add(
                        GameDetailEntry(
                            leadingIcon = Icons.LayersW500Rounded,
                            headline = Res.string.game__details_engines,
                            supporting = engines.joinToString()
                        )
                    )
                }
            }
            if (franchiseNames.isNotEmpty()) {
                if (franchiseNames.size > 1) {
                    add(
                        GameDetailEntry(
                            leadingIcon = Icons.CategoryW500Rounded,
                            headline = Res.string.game__details_franchise,
                            supporting = "${franchiseNames.size} franchises (${franchiseNames.first()}...)",
                            trailingIcon = Icons.InfoW500Rounded
                        ) { showFranchisesSheet = true }
                    )
                } else {
                    add(
                        GameDetailEntry(
                            leadingIcon = Icons.CategoryW500Rounded,
                            headline = Res.string.game__details_franchise,
                            supporting = franchiseNames.first()
                        )
                    )
                }
            }
            if (game.collections.isNotEmpty()) {
                val collections = game.collections.mapNotNull { it.name }.filter { it.isNotBlank() }
                if (collections.isNotEmpty()) {
                    add(
                        GameDetailEntry(
                            leadingIcon = Icons.Inventory2W500Rounded,
                            headline = Res.string.game__details_collection,
                            supporting = collections.joinToString()
                        )
                    )
                }
            }
        }
    }

    if (details.isEmpty()) return@Column

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnsCount = when {
            maxWidth >= 900.dp -> 3
            maxWidth >= 500.dp -> 2
            else -> 1
        }

        val columnItems = remember(details, columnsCount) {
            val list = List(columnsCount) { mutableListOf<GameDetailEntry>() }
            for ((index, detail) in details.withIndex()) {
                list[index % columnsCount].add(detail)
            }
            list
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (colItems in columnItems) {
                if (colItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for ((index, detail) in colItems.withIndex()) {
                            ListItem(
                                modifier = Modifier
                                    .clip(
                                        ListItemDefaults.expressiveShape(
                                            first = index == 0,
                                            last = index == colItems.lastIndex
                                        )
                                    )
                                    .let { if (detail.onClick != null) it.clickable { detail.onClick.invoke() } else it },
                                leadingContent = { Icon(detail.leadingIcon, contentDescription = null) },
                                headlineContent = { Text(stringResource(detail.headline)) },
                                supportingContent = detail.supporting?.let { { Text(it) } },
                                trailingContent = detail.trailingIcon?.let {
                                    { Icon(imageVector = it, contentDescription = null) }
                                },
                                colors = ListItemDefaults.expressiveSegmentedColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    if (showReleaseDatesSheet) {
        ReleaseDatesBottomSheet(game) { showReleaseDatesSheet = false }
    }
    if (showAlternativeNamesSheet) {
        AlternativeNamesBottomSheet(game) { showAlternativeNamesSheet = false }
    }
    if (showFranchisesSheet) {
        FranchisesBottomSheet(franchiseNames) { showFranchisesSheet = false }
    }
    if (showLanguagesSheet) {
        LanguagesBottomSheet(game) { showLanguagesSheet = false }
    }
}

private data class GameDetailEntry(
    val leadingIcon: ImageVector,
    val headline: StringResource,
    val supporting: String? = null,
    val trailingIcon: ImageVector? = null,
    val onClick: (() -> Unit)? = null
)
