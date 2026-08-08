package it.maicol07.gamerlogue.extensions

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.launch

/**
 * Pixels scrolled per wheel notch; Compose reports wheel deltas in notches, not pixels.
 * Roughly one and a half cover cards, so a notch visibly advances the carousel.
 */
private const val WheelScrollPixels = 240f

/**
 * Makes a *horizontal* [state] reachable with a mouse: vertical wheel and left-button drag.
 *
 * Compose assigns a wheel delta to a single axis by its angle
 * (`ScrollingLogic.toSingleAxisDeltaFromAngle`), so a vertical wheel resolves to 0 on a horizontal
 * scrollable, and a horizontal scrollable only accepts drags from touch. The wheel is only consumed
 * while the carousel can still move that way, so scrolling past its end falls through to the page
 * below; the drag is restricted to [PointerType.Mouse] so touch keeps using the built-in gesture.
 *
 * ponytail: the drag dispatches raw deltas, so releasing stops dead instead of flinging. Wire it to
 * a `scrollable`/velocity tracker if the lack of momentum starts to matter.
 */
fun Modifier.mouseScrollsHorizontally(state: ScrollableState) = composed {
    val scope = rememberCoroutineScope()
    Modifier
        .pointerInput(state) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.type != PointerEventType.Scroll) continue

                    val notches = event.changes.sumOf { it.scrollDelta.y.toDouble() }.toFloat()
                    val canScroll = if (notches > 0f) state.canScrollForward else state.canScrollBackward
                    if (notches == 0f || !canScroll) continue

                    event.changes.forEach { it.consume() }
                    scope.launch { state.animateScrollBy(notches * WheelScrollPixels) }
                }
            }
        }
        .pointerInput(state) {
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
