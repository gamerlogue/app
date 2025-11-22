package it.maicol07.gamerlogue.ui.components.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import gamerlogue.composeapp.generated.resources.Res
import gamerlogue.composeapp.generated.resources.game__details_developers
import gamerlogue.composeapp.generated.resources.game__details_publishers
import gamerlogue.composeapp.generated.resources.game__details_release_date
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CalendarMonthW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CodeW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.InfoW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PublishW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveColors
import it.maicol07.gamerlogue.extensions.expressiveShape
import it.maicol07.gamerlogue.extensions.igdb.displayDate
import it.maicol07.gamerlogue.ui.views.game.components.ReleaseDatesBottomSheet
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GameDetailsList(game: Game) = Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    var showReleaseDatesSheet by remember { mutableStateOf(false) }

    val details = remember(game) {
        val firstReleaseDate = ReleaseDate(date = game.first_release_date)
        listOf(
            GameDetailEntry(
                leadingIcon = Icons.CalendarMonthW500Rounded,
                headline = Res.string.game__details_release_date,
                supporting = firstReleaseDate.displayDate(),
                trailingIcon = Icons.InfoW500Rounded
            ) { showReleaseDatesSheet = true },
            GameDetailEntry(
                leadingIcon = Icons.CodeW500Rounded,
                headline = Res.string.game__details_developers,
                supporting = game.involved_companies
                    .filter { it.developer }
                    .joinToString { it.company?.name ?: "N/A" }
            ),
            GameDetailEntry(
                leadingIcon = Icons.PublishW500Rounded,
                headline = Res.string.game__details_publishers,
                supporting = game.involved_companies
                    .filter { it.publisher }
                    .joinToString { it.company?.name ?: "N/A" }
            ),
        )
    }

    for ((index, detail) in details.withIndex()) {
        ListItem(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(ListItemDefaults.expressiveShape(first = index == 0, last = index == details.lastIndex))
                .let { if (detail.onClick != null) it.clickable { detail.onClick.invoke() } else it },
            leadingContent = { Icon(detail.leadingIcon, contentDescription = null) },
            headlineContent = { Text(stringResource(detail.headline)) },
            supportingContent = {
                if (detail.supporting != null) {
                    Text(detail.supporting)
                }
            },
            trailingContent = detail.trailingIcon?.let {
                {
                    Icon(
                        imageVector = detail.trailingIcon,
                        contentDescription = null
                    )
                }
            },
            colors = ListItemDefaults.expressiveColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }

    if (showReleaseDatesSheet) {
        ReleaseDatesBottomSheet(game) { showReleaseDatesSheet = false }
    }
}

private data class GameDetailEntry(
    val leadingIcon: ImageVector,
    val headline: StringResource,
    val supporting: String? = null,
    val trailingIcon: ImageVector? = null,
    val onClick: (() -> Unit)? = null
)
