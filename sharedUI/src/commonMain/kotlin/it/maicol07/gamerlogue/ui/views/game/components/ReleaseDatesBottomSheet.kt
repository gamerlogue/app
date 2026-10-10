package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__release_dates_title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes
import it.maicol07.gamerlogue.extensions.igdb.displayDate
import it.maicol07.gamerlogue.extensions.igdb.localizedName
import it.maicol07.gamerlogue.ui.components.game.Image
import it.maicol07.gamerlogue.ui.views.list.GameListPreset
import it.maicol07.gamerlogue.ui.views.list.GameListPresetType
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseDatesBottomSheet(
    game: Game,
    onPlatformClick: (GameListPreset) -> Unit,
    onDismissRequest: () -> Unit
) = ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = rememberBottomSheetState(SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(
                stringResource(Res.string.game__release_dates_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        itemsIndexed(game.platforms, { _, platform -> platform.id }) { index, platform ->
            val dateInstant = game.release_dates
                .filter { it.platform?.id == platform.id }
            ListItem(
                onClick = {
                    onPlatformClick(GameListPreset(GameListPresetType.PLATFORM, platform.id.toInt(), platform.name))
                },
                leadingContent = {
                    platform.Image(
                        modifier = Modifier.width(24.dp).height(24.dp),
                        loadingModifier = Modifier.width(24.dp).height(24.dp),
                    )
                },
                supportingContent = {
                    Column {
                        for (date in dateInstant) {
                            val prepend = date.status?.name ?: "•"
                            val append = date.release_region?.let { " (${it.localizedName})" } ?: ""
                            Text(
                                text = "$prepend ${date.displayDate()}$append",
                                modifier = Modifier.padding(top = if (date == dateInstant.first()) 0.dp else 4.dp)
                            )
                        }
                    }
                },
                trailingContent = { Icon(Icons.KeyboardArrowRightW500Rounded, contentDescription = null) },
                colors = ListItemDefaults.expressiveSegmentedColors(),
                shapes = ListItemDefaults.expressiveSegmentedShapes(index, game.platforms.size),
            ) { Text(platform.name) }
        }
    }
}
