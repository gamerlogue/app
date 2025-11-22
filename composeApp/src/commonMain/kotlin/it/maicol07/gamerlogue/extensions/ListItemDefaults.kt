package it.maicol07.gamerlogue.extensions

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ListItemDefaults.expressiveColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    headlineColor: Color = Color.Unspecified,
    leadingIconColor: Color = Color.Unspecified,
    overlineColor: Color = Color.Unspecified,
    supportingColor: Color = Color.Unspecified,
    trailingIconColor: Color = Color.Unspecified,
    disabledHeadlineColor: Color = Color.Unspecified,
    disabledLeadingIconColor: Color = Color.Unspecified,
    disabledTrailingIconColor: Color = Color.Unspecified,
) = colors(
    containerColor,
    headlineColor,
    leadingIconColor,
    overlineColor,
    supportingColor,
    trailingIconColor,
    disabledHeadlineColor,
    disabledLeadingIconColor,
    disabledTrailingIconColor
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
