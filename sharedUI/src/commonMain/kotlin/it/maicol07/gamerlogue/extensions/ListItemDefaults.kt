package it.maicol07.gamerlogue.extensions

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListItemDefaults.expressiveSegmentedColors(
    // default
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = Color.Unspecified,
    leadingContentColor: Color = Color.Unspecified,
    trailingContentColor: Color = Color.Unspecified,
    overlineContentColor: Color = Color.Unspecified,
    supportingContentColor: Color = Color.Unspecified,
    // disabled
    disabledContainerColor: Color = Color.Unspecified,
    disabledContentColor: Color = Color.Unspecified,
    disabledLeadingContentColor: Color = Color.Unspecified,
    disabledTrailingContentColor: Color = Color.Unspecified,
    disabledOverlineContentColor: Color = Color.Unspecified,
    disabledSupportingContentColor: Color = Color.Unspecified,
    // selected
    selectedContainerColor: Color = Color.Unspecified,
    selectedContentColor: Color = Color.Unspecified,
    selectedLeadingContentColor: Color = Color.Unspecified,
    selectedTrailingContentColor: Color = Color.Unspecified,
    selectedOverlineContentColor: Color = Color.Unspecified,
    selectedSupportingContentColor: Color = Color.Unspecified,
    // dragged
    draggedContainerColor: Color = Color.Unspecified,
    draggedContentColor: Color = Color.Unspecified,
    draggedLeadingContentColor: Color = Color.Unspecified,
    draggedTrailingContentColor: Color = Color.Unspecified,
    draggedOverlineContentColor: Color = Color.Unspecified,
    draggedSupportingContentColor: Color = Color.Unspecified,
) = segmentedColors(
    containerColor = containerColor,
    contentColor = contentColor,
    leadingContentColor = leadingContentColor,
    trailingContentColor = trailingContentColor,
    overlineContentColor = overlineContentColor,
    supportingContentColor = supportingContentColor,
    disabledContainerColor = disabledContainerColor,
    disabledContentColor = disabledContentColor,
    disabledLeadingContentColor = disabledLeadingContentColor,
    disabledTrailingContentColor = disabledTrailingContentColor,
    disabledOverlineContentColor = disabledOverlineContentColor,
    disabledSupportingContentColor = disabledSupportingContentColor,
    selectedContainerColor = selectedContainerColor,
    selectedContentColor = selectedContentColor,
    selectedLeadingContentColor = selectedLeadingContentColor,
    selectedTrailingContentColor = selectedTrailingContentColor,
    selectedOverlineContentColor = selectedOverlineContentColor,
    selectedSupportingContentColor = selectedSupportingContentColor,
    draggedContainerColor = draggedContainerColor,
    draggedContentColor = draggedContentColor,
    draggedLeadingContentColor = draggedLeadingContentColor,
    draggedTrailingContentColor = draggedTrailingContentColor,
    draggedOverlineContentColor = draggedOverlineContentColor,
    draggedSupportingContentColor = draggedSupportingContentColor
)

@Suppress("UnusedReceiverParameter")
@Composable
fun ListItemDefaults.expressiveShape(
    first: Boolean = false,
    last: Boolean = false
): RoundedCornerShape {
    // 16.dp and 4.dp are hardcoded since we have no way of getting them dynamically from Material3
    val topStartCornerDp by animateDpAsState(
        if (first) 16.dp else 4.dp
    )
    val topEndCornerDp by animateDpAsState(
        if (first) 16.dp else 4.dp
    )
    val bottomStartCornerDp by animateDpAsState(
        if (last) 16.dp else 4.dp
    )
    val bottomEndCornerDp by animateDpAsState(
        if (last) 16.dp else 4.dp
    )

    return RoundedCornerShape(topStartCornerDp, topEndCornerDp, bottomStartCornerDp, bottomEndCornerDp)
}
