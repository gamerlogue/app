package it.maicol07.gamerlogue.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.platformtools.Platform
import io.github.kdroidfilter.platformtools.getPlatform
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowBackW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowForwardW500Rounded
import it.maicol07.gamerlogue.extensions.mouseDragScrollsHorizontally
import it.maicol07.gamerlogue.ui.theme.Dimens
import kotlinx.coroutines.launch

/** Inset of an arrow button from the carousel edge it overlays. */
private val ArrowInset = 8.dp

/**
 * The app's horizontal carousel: a [HorizontalMultiBrowseCarousel] with the side padding, the mouse
 * affordances and the previous/next buttons ([CarouselWithArrows]) every carousel in the app wants.
 *
 * The padding is on the box rather than the carousel's `contentPadding`, which MultiBrowse only
 * honours on the leading edge.
 */
@Composable
fun GameCoverCarousel(
    itemCount: Int,
    preferredItemWidth: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = Dimens.CardGap,
    content: @Composable CarouselItemScope.(Int) -> Unit
) {
    val state = rememberCarouselState { itemCount }
    val step = with(LocalDensity.current) { (preferredItemWidth + itemSpacing).toPx() }
    CarouselWithArrows(
        state = state,
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        onBack = { state.animateScrollBy(-step) },
        onForward = { state.animateScrollBy(step) }
    ) {
        HorizontalMultiBrowseCarousel(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .then(modifier)
                .mouseDragScrollsHorizontally(state),
            preferredItemWidth = preferredItemWidth,
            itemSpacing = itemSpacing,
            content = content
        )
    }
}

/**
 * Overlays previous/next buttons on [carousel], each shown while [state] can still scroll its way.
 * They are pointer chrome: left out on Android, and elsewhere shown only while the pointer hovers.
 */
@Composable
fun CarouselWithArrows(
    state: ScrollableState,
    modifier: Modifier,
    onBack: suspend () -> Unit,
    onForward: suspend () -> Unit,
    carousel: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()

    Box(modifier.fillMaxWidth().hoverable(hoverSource)) {
        carousel()
        if (getPlatform() != Platform.ANDROID) {
            CarouselArrow(
                icon = Icons.ArrowBackW500Rounded,
                visible = hovered && state.canScrollBackward,
                modifier = Modifier.align(Alignment.CenterStart)
            ) { scope.launch { onBack() } }
            CarouselArrow(
                icon = Icons.ArrowForwardW500Rounded,
                visible = hovered && state.canScrollForward,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) { scope.launch { onForward() } }
        }
    }
}

/** Hidden rather than disabled at the ends: as an overlay it no longer affects the layout. */
@Composable
private fun CarouselArrow(
    icon: ImageVector,
    visible: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) = AnimatedVisibility(visible, modifier.padding(horizontal = ArrowInset), enter = fadeIn(), exit = fadeOut()) {
    FilledTonalIconButton(onClick = onClick) {
        Icon(icon, contentDescription = null)
    }
}
