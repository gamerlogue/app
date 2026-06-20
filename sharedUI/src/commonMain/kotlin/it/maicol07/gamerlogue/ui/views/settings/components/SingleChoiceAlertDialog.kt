/**
 * @source https://github.com/alorma/Compose-Settings/blob/main/samples/shared/src/commonMain/kotlin/com/alorma/compose/settings/sample/shared/internal/SingleChoiceAlertDialog.kt
 */

package it.maicol07.gamerlogue.ui.views.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_cancel
import gamerlogue.sharedui.generated.resources.common_clear
import gamerlogue.sharedui.generated.resources.common_select
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun <T> SingleChoiceAlertDialog(
    dialogTitle: String,
    selectedItem: T?,
    onItemSelected: (T?) -> Unit,
    items: List<T>,
    itemIcon: (@Composable (T) -> Unit)? = null,
    itemTitle: (T) -> String,
    itemDescription: (T) -> String = { "" },
) {
    var userSelectedItem by remember { mutableStateOf(selectedItem) }

    AlertDialog(
        onDismissRequest = { onItemSelected(selectedItem) },
        title = { Text(text = dialogTitle) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                itemsIndexed(items) { index, sampleItem ->
                    val isSelected = sampleItem == userSelectedItem
                    LabelRadioButton(
                        item = sampleItem,
                        itemIcon = itemIcon?.let { { it(sampleItem) } },
                        isSelected = isSelected,
                        index = index,
                        count = items.size,
                        onClick = { userSelectedItem = sampleItem },
                        itemTitle = itemTitle,
                        itemDescription = itemDescription,
                    )
                }
            }
        },
        confirmButton = if (userSelectedItem == null) {
            {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { onItemSelected(null) }
                ) {
                    Text(text = stringResource(Res.string.common_cancel))
                }
            }
        } else {
            {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { onItemSelected(userSelectedItem) }
                ) {
                    Text(text = stringResource(Res.string.common_select))
                }
            }
        },
        dismissButton = if (userSelectedItem == null) {
            null
        } else {
            {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { onItemSelected(null) }
                ) {
                    Text(text = stringResource(Res.string.common_clear))
                }
            }
        },
    )
}
