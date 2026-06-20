/**
 * @source https://github.com/alorma/Compose-Settings/blob/main/samples/shared/src/commonMain/kotlin/com/alorma/compose/settings/sample/shared/internal/LabelRadioButton.kt
 */
package it.maicol07.gamerlogue.ui.views.settings.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun <T> LabelRadioButton(
    item: T,
    itemIcon: (@Composable () -> Unit)? = null,
    itemTitle: (T) -> String,
    itemDescription: (T) -> String = { "" },
    isSelected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) = SegmentedListItem(
    onClick = onClick,
    selected = isSelected,
    shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
    leadingContent = itemIcon,
    supportingContent = itemDescription(item).ifBlank { null }?.let { { Text(it) } },
    trailingContent = { RadioButton(selected = isSelected, onClick = null) },
) { Text(text = itemTitle(item)) }
