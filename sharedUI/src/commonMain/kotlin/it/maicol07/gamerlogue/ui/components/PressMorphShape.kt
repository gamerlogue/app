package it.maicol07.gamerlogue.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Rounded corners that grow while [interactionSource] is pressed. Driven by the motion scheme, so
 * it snaps instead of animating when the system animation scale is 0.
 */
@Composable
fun pressMorphShape(interactionSource: MutableInteractionSource, rest: Dp, pressed: Dp): Shape {
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) pressed else rest,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )
    return RoundedCornerShape(corner)
}
