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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import co.touchlab.kermit.Logger
import com.github.panpf.zoomimage.compose.zoom.rememberZoomableState
import com.github.panpf.zoomimage.compose.zoom.zoom
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common__more_options
import gamerlogue.sharedui.generated.resources.common_close
import gamerlogue.sharedui.generated.resources.viewer__copy_link
import gamerlogue.sharedui.generated.resources.viewer__link_copied
import gamerlogue.sharedui.generated.resources.viewer__open_external
import gamerlogue.sharedui.generated.resources.viewer__page
import gamerlogue.sharedui.generated.resources.viewer__save
import gamerlogue.sharedui.generated.resources.viewer__save_failed
import gamerlogue.sharedui.generated.resources.viewer__saved
import gamerlogue.sharedui.generated.resources.viewer__share
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DownloadW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LinkW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.MoreVertW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ShareW500Rounded
import it.maicol07.gamerlogue.SystemBarsVisible
import it.maicol07.gamerlogue.clipEntryFor
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.ui.views.game.components.Ratio169
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Fullscreen image viewer, reusable and content-agnostic.
 *
 * Each page zooms on its own (pinch, double tap); a tap toggles the chrome. The top bar shows the
 * position and a menu to open, copy, share (where supported) or save the current image. On a
 * keyboard, arrows change page and Escape closes.
 *
 * Contract:
 * - imagesCount: total number of pages
 * - imageUrl: full-size URL of a page, for the menu actions
 * - initialPage: page to open on
 * - onDismissRequest: called when user taps the close action
 * - imageContent: slot to render the large image for the given page (apply the incoming `modifier`)
 * - thumbnailContent: slot to render the thumbnail; highlight is provided via `selected`
 */
private const val TopScrimAlpha = 0.5f

/** Height of the thumbnail strip (16:9 thumbnails at 125dp wide plus their bottom padding), with a margin. */
private val ThumbnailStripClearance = 104.dp

@Composable
fun FullscreenImageViewer(
    imagesCount: Int,
    imageUrl: (page: Int) -> String,
    initialPage: Int = 0,
    onDismissRequest: () -> Unit,
    imageContent: @Composable BoxScope.(page: Int, modifier: Modifier) -> Unit,
    thumbnailContent: @Composable (page: Int, modifier: Modifier) -> Unit = { _, modifier ->
        // Default: simple dot
        Box(modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
    },
    showThumbnails: Boolean = imagesCount > 1,
    topOverlayHeight: Dp = 88.dp, // top gradient area height (status bar + actions)
    bottomScrimMaxAlpha: Float = 0.5f, // max bottom scrim opacity
) {
    if (imagesCount <= 0) return

    val safeInitial = initialPage.coerceIn(0, imagesCount - 1)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(color = Color.Black) {
            val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { imagesCount })
            val pagerScope = rememberCoroutineScope()
            val focusRequester = remember { FocusRequester() }
            val snackbarHostState = remember { SnackbarHostState() }
            var chromeVisible by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            Box(
                Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val target = when (event.key) {
                            Key.DirectionLeft -> pagerState.currentPage - 1
                            Key.DirectionRight -> pagerState.currentPage + 1
                            Key.Escape -> {
                                onDismissRequest()
                                return@onPreviewKeyEvent true
                            }
                            else -> return@onPreviewKeyEvent false
                        }
                        pagerScope.launch { pagerState.animateScrollToPage(target.coerceIn(0, imagesCount - 1)) }
                        true
                    }
            ) {
                // Hide system bars when chrome is hidden
                SystemBarsVisible(chromeVisible)

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    // One zoom state per page, so a zoomed image does not leave the next one zoomed.
                    Box(
                        Modifier.fillMaxSize().zoom(rememberZoomableState(), onTap = { chromeVisible = !chromeVisible }),
                        contentAlignment = Alignment.Center
                    ) {
                        imageContent(page, Modifier)
                    }
                }

                AnimatedVisibility(
                    visible = chromeVisible,
                    enter = slideInVertically(tween(200)),
                    exit = slideOutVertically(tween(200)),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    TopOverlay(
                        page = pagerState.currentPage,
                        imagesCount = imagesCount,
                        imageUrl = imageUrl(pagerState.currentPage),
                        snackbarHostState = snackbarHostState,
                        onClose = onDismissRequest,
                        height = topOverlayHeight,
                    )
                }

                // Above the thumbnail strip while it shows, at the bottom edge otherwise.
                SnackbarHost(
                    snackbarHostState,
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = if (showThumbnails && chromeVisible) ThumbnailStripClearance else 16.dp)
                )

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

@Composable
private fun TopOverlay(
    page: Int,
    imagesCount: Int,
    imageUrl: String,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    height: Dp,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = TopScrimAlpha), Color.Transparent)))
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        FilledTonalIconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.CloseW500Rounded, contentDescription = stringResource(Res.string.common_close))
        }
        if (imagesCount > 1) {
            val position = stringResource(Res.string.viewer__page, page + 1, imagesCount)
            Text(
                "${page + 1} / $imagesCount",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clearAndSetSemantics { contentDescription = position }
            )
        }
        ImageMenu(imageUrl, snackbarHostState)
    }
}

/** Open, copy, share (where the platform has a share sheet) and save the current image. */
@Composable
private fun ImageMenu(imageUrl: String, snackbarHostState: SnackbarHostState) = Box {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val share = rememberShareUrl()
    val saveImage = rememberSaveImage()
    val scope = rememberCoroutineScope()
    val linkCopied = stringResource(Res.string.viewer__link_copied)
    val saved = stringResource(Res.string.viewer__saved)
    val saveFailed = stringResource(Res.string.viewer__save_failed)
    var expanded by remember { mutableStateOf(false) }

    FilledTonalIconButton(onClick = { expanded = true }, shapes = IconButtonDefaults.shapes()) {
        Icon(Icons.MoreVertW500Rounded, contentDescription = stringResource(Res.string.common__more_options))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        MenuItem(Res.string.viewer__open_external, Icons.OpenInNewW500Rounded) {
            expanded = false
            uriHandler.openURL(imageUrl)
        }
        MenuItem(Res.string.viewer__copy_link, Icons.LinkW500Rounded) {
            expanded = false
            scope.launch {
                clipboard.setClipEntry(clipEntryFor(imageUrl))
                snackbarHostState.showSnackbar(linkCopied)
            }
        }
        if (isShareSupported) {
            MenuItem(Res.string.viewer__share, Icons.ShareW500Rounded) {
                expanded = false
                share(imageUrl)
            }
        }
        MenuItem(Res.string.viewer__save, Icons.DownloadW500Rounded) {
            expanded = false
            scope.launch {
                // UI boundary: any failure (network, storage, browser) ends in the same message.
                val message = try {
                    if (saveImage(downloadImageBytes(imageUrl), imageFileName(imageUrl))) saved else null
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Logger.e(e) { "Saving image $imageUrl failed" }
                    saveFailed
                }
                message?.let { snackbarHostState.showSnackbar(it) }
            }
        }
    }
}

@Composable
private fun MenuItem(text: StringResource, icon: ImageVector, onClick: () -> Unit) = DropdownMenuItem(
    text = { Text(stringResource(text)) },
    leadingIcon = { Icon(icon, contentDescription = null) },
    onClick = onClick
)
