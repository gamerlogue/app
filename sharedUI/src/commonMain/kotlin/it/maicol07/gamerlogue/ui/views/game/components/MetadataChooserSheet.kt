package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveShape
import it.maicol07.gamerlogue.ui.views.list.GameListPreset
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Several values of one metadata field (e.g. the developers); picking one opens the games filtered on it. */
data class MetadataChoice(val title: StringResource, val icon: ImageVector, val options: List<GameListPreset>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataChooserSheet(
    choice: MetadataChoice,
    onPick: (GameListPreset) -> Unit,
    onDismissRequest: () -> Unit
) = ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(
                stringResource(choice.title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        itemsIndexed(choice.options) { index, option ->
            ListItem(
                leadingContent = { Icon(choice.icon, contentDescription = null) },
                headlineContent = { Text(option.name) },
                trailingContent = { Icon(Icons.KeyboardArrowRightW500Rounded, contentDescription = null) },
                colors = ListItemDefaults.expressiveSegmentedColors(),
                modifier = Modifier
                    .clip(ListItemDefaults.expressiveShape(index == 0, index == choice.options.lastIndex))
                    .clickable { onPick(option) }
            )
        }
    }
}
