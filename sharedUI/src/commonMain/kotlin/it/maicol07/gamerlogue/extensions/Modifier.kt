package it.maicol07.gamerlogue.extensions

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * Makes a *horizontal* [state] draggable with the left mouse button: a horizontal scrollable only
 * accepts drags from touch. Restricted to [PointerType.Mouse] so touch keeps the built-in gesture.
 *
 * The vertical wheel is deliberately left alone so it always scrolls the page; horizontal wheel and
 * trackpad deltas (Shift+wheel included) already reach the scrollable natively.
 *
 * ponytail: the drag dispatches raw deltas, so releasing stops dead instead of flinging. Wire it to
 * a `scrollable`/velocity tracker if the lack of momentum starts to matter.
 */
fun Modifier.mouseDragScrollsHorizontally(state: ScrollableState) =
    pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (down.type != PointerType.Mouse) return@awaitEachGesture

            val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                change.consume()
                state.dispatchRawDelta(-overSlop)
            } ?: return@awaitEachGesture

            horizontalDrag(dragStart.id) { change ->
                change.consume()
                state.dispatchRawDelta(-change.positionChange().x)
            }
        }
    }

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
