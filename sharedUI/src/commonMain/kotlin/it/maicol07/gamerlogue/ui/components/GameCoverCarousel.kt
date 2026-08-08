package it.maicol07.gamerlogue.ui.components

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import it.maicol07.gamerlogue.extensions.mouseScrollsHorizontally
import it.maicol07.gamerlogue.ui.theme.Dimens
import kotlinx.coroutines.launch

/** Gap between an arrow button and the carousel it drives. */
private val ArrowGap = 4.dp

/**
 * The app's horizontal carousel: a [HorizontalMultiBrowseCarousel] with the side padding, the mouse
 * affordances and the previous/next buttons every carousel in the app wants.
 *
 * The padding is on the row rather than the carousel's `contentPadding`, which MultiBrowse only
 * honours on the leading edge. The arrows are pointer chrome: they are left out on Android, and
 * elsewhere they flank the carousel instead of overlaying the cards at the edges.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameCoverCarousel(
    itemCount: Int,
    preferredItemWidth: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = Dimens.CardGap,
    content: @Composable CarouselItemScope.(Int) -> Unit
) {
    val state = rememberCarouselState { itemCount }
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { (preferredItemWidth + itemSpacing).toPx() }

    val showArrows = getPlatform() != Platform.ANDROID

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(ArrowGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showArrows) {
            CarouselArrow(Icons.ArrowBackW500Rounded, state.canScrollBackward) {
                scope.launch { state.animateScrollBy(-step) }
            }
        }
        HorizontalMultiBrowseCarousel(
            state = state,
            modifier = Modifier
                .weight(1f)
                .then(modifier)
                .mouseScrollsHorizontally(state),
            preferredItemWidth = preferredItemWidth,
            itemSpacing = itemSpacing,
            content = content
        )
        if (showArrows) {
            CarouselArrow(Icons.ArrowForwardW500Rounded, state.canScrollForward) {
                scope.launch { state.animateScrollBy(step) }
            }
        }
    }
}

/**
 * Disabled rather than hidden at the ends of the carousel: removing it would resize the carousel
 * mid-scroll and make the cards jump.
 */
@Composable
private fun CarouselArrow(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) = FilledTonalIconButton(onClick = onClick, enabled = enabled) {
    Icon(icon, contentDescription = null)
}
