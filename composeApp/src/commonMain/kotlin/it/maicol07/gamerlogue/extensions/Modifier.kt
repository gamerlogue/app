package it.maicol07.gamerlogue.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

// Source - https://stackoverflow.com/a/77222327
// Posted by Thracian, modified by community. See post 'Timeline' for change history
// Retrieved 2025-11-15, License - CC BY-SA 4.0
fun Modifier.isVisible(
    threshold: Int,
    onVisibilityChange: (Boolean) -> Unit
) = composed {
    Modifier.onGloballyPositioned { layoutCoordinates: LayoutCoordinates ->
        val layoutHeight = layoutCoordinates.size.height
        val thresholdHeight = layoutHeight * threshold / 100
        val layoutTop = layoutCoordinates.positionInRoot().y
        val layoutBottom = layoutTop + layoutHeight

        // This should be parentLayoutCoordinates not parentCoordinates
        val parent =
            layoutCoordinates.parentLayoutCoordinates

        parent?.boundsInRoot()?.let { rect: Rect ->
            val parentTop = rect.top
            val parentBottom = rect.bottom

            if (
                parentBottom - layoutTop > thresholdHeight &&
                (parentTop < layoutBottom - thresholdHeight)
            ) {
                onVisibilityChange(true)
            } else {
                onVisibilityChange(false)
            }
        }
    }
}
