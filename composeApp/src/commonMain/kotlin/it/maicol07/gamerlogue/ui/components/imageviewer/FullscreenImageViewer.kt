package it.maicol07.gamerlogue.ui.components.imageviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth // aggiunto
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.panpf.zoomimage.compose.zoom.rememberZoomableState
import com.github.panpf.zoomimage.compose.zoom.zoom
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import it.maicol07.gamerlogue.SystemBarsVisible
import it.maicol07.gamerlogue.ui.views.game.Ratio169
import kotlinx.coroutines.launch

/**
 * Fullscreen image viewer, reusable and content-agnostic.
 *
 * Contract:
 * - imagesCount: total number of pages
 * - initialPage: page to open on
 * - onDismissRequest: called when user taps the close action
 * - imageContent: slot to render the large image for the given page (apply the incoming `modifier`)
 * - thumbnailContent: slot to render the thumbnail; highlight is provided via `selected`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenImageViewer(
    imagesCount: Int,
    initialPage: Int = 0,
    onDismissRequest: () -> Unit,
    imageContent: @Composable BoxScope.(page: Int, modifier: Modifier) -> Unit,
    thumbnailContent: @Composable (page: Int, modifier: Modifier) -> Unit = { _, modifier ->
        // Default: simple dot
        Box(modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
    },
    showThumbnails: Boolean = imagesCount > 1,
    bottomBarHeight: Dp = 84.dp,
    topOverlayHeight: Dp = 88.dp, // altezza area gradient top (statusbar + azioni)
    bottomOverlayHeight: Dp = 140.dp, // altezza area gradient bottom (thumbnails)
    topScrimAlpha: Float = 0.45f, // intensità massima scrim top
    bottomScrimMaxAlpha: Float = 0.5f, // intensità massima scrim bottom
) {
    if (imagesCount <= 0) return

    val safeInitial = initialPage.coerceIn(0, imagesCount - 1)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(color = Color.Black) {
            Box(Modifier.fillMaxSize()) {
                val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { imagesCount })
                var chromeVisible by remember { mutableStateOf(true) }

                // Hide system bars when chrome is hidden
                SystemBarsVisible(chromeVisible)

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                        .zoom(
                            rememberZoomableState(),
                            onTap = { chromeVisible = !chromeVisible }
                        )
                ) { page ->
                    imageContent(
                        page,
                        Modifier
                    )
                }

                AnimatedVisibility(
                    visible = chromeVisible,
                    enter = slideInVertically(tween(200)),
                    exit = slideOutVertically(tween(200)),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    TopOverlay(
                        onClose = onDismissRequest,
                        height = topOverlayHeight,
                        maxScrimAlpha = topScrimAlpha,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                // Bottom carousel thumbnails
                AnimatedVisibility(
                    visible = showThumbnails && chromeVisible,
                    enter = slideInVertically(tween(200)),
                    exit = slideOutVertically(tween(200)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val carouselState = rememberCarouselState { imagesCount }

                    // Sync when main pager changes
                    LaunchedEffect(pagerState.currentPage) {
                        if (carouselState.currentItem != pagerState.currentPage) {
                            carouselState.animateScrollToItem(pagerState.currentPage)
                        }
                    }
                    // Optional: when carousel tapped -> main pager scroll
                    LaunchedEffect(carouselState.currentItem) {
                        if (pagerState.currentPage != carouselState.currentItem) {
                            pagerState.animateScrollToPage(carouselState.currentItem)
                        }
                    }

                    val scope = rememberCoroutineScope()

                    HorizontalMultiBrowseCarousel(
                        state = carouselState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp, start = 8.dp, end = 8.dp)
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.5f to Color.Black.copy(alpha = bottomScrimMaxAlpha * 0.4f),
                                        1f to Color.Black.copy(alpha = bottomScrimMaxAlpha)
                                    )
                                )
                            },
                        preferredItemWidth = 125.dp,
                        itemSpacing = 6.dp,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) { i ->
                        val selected = carouselState.currentItem == i
                        val targetScale = if (selected) 1f else 0.85f
                        val scale by animateFloatAsState(
                            targetValue = targetScale,
                            animationSpec = tween(250, easing = FastOutSlowInEasing)
                        )
                        val alpha by animateFloatAsState(
                            targetValue = if (selected) 1f else 0.6f,
                            animationSpec = tween(250)
                        )
                        thumbnailContent(
                            i,
                            Modifier
                                .aspectRatio(Ratio169)
                                .graphicsLayer {
                                    this.scaleX = scale
                                    this.scaleY = scale
                                    this.alpha = alpha
                                }
                                .clickable {
                                    scope.launch { pagerState.animateScrollToPage(i) }
                                }
                                .fillMaxSize()
                                .maskClip(MaterialTheme.shapes.large)
                                .maskBorder(
                                    BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Transparent
                                        }
                                    ),
                                    shape = MaterialTheme.shapes.large
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopOverlay(
    onClose: () -> Unit,
    height: Dp,
    maxScrimAlpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(
                onClick = onClose,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    Icons.CloseW500Rounded,
                    contentDescription = null // TODO: Add res string
                )
            }

            Spacer(Modifier.size(8.dp))
        }
    }
}
