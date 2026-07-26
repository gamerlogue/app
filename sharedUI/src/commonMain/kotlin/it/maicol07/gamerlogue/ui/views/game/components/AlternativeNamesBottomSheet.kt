package it.maicol07.gamerlogue.ui.views.game.components

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
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__alternative_names_title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Book4W500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveShape
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlternativeNamesBottomSheet(
    game: Game,
    onDismissRequest: () -> Unit = { }
) = ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(
                stringResource(Res.string.game__alternative_names_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        itemsIndexed(game.alternative_names, { _, alt -> alt.id }) { index, alt ->
            ListItem(
                leadingContent = {
                    Icon(Icons.Book4W500Rounded, contentDescription = null)
                },
                headlineContent = { Text(alt.name) },
                supportingContent = alt.comment.takeIf { !it.isNullOrBlank() }?.let { { Text(it) } },
                colors = ListItemDefaults.expressiveSegmentedColors(),
                modifier = Modifier.clip(
                    ListItemDefaults.expressiveShape(index == 0, index == game.alternative_names.lastIndex)
                )
            )
        }
    }
}
