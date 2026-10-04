package it.maicol07.gamerlogue.ui.components.game

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import at.released.igdbclient.model.Artwork
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.model.Screenshot
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__artwork_image
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.navigation.LocalSharedTransitionScope
import org.jetbrains.compose.resources.stringResource

/**
 * Tags this image as a shared element keyed by [key], so it morphs across the list -> detail
 * transition. No-op when [key] is null or when rendered outside the nav host (previews, galleries).
 *
 * ponytail: key is a plain string. On tablet list-detail both panes can show the same image at once
 * (same key) and Compose logs a duplicate-key warning — gate on a compact window, or scope the key
 * per pane, if that ever matters.
 */
@Composable
private fun Modifier.sharedGameElement(key: Any?): Modifier {
    if (key == null) return this
    val scope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(scope) { sharedElement(rememberSharedContentState(key), animatedScope) }
}

/** Intrinsic cover size, used when the caller does not size the image itself. */
val CoverWidth = 150.dp
val CoverHeight = 200.dp

/** Aspect ratio of a cover, for grid cells that size themselves off their column width. */
val CoverAspectRatio = CoverWidth / CoverHeight

/**
 * A game cover. Pass [sizeModifier] to let the parent drive the size (e.g. a grid cell that must
 * fill its column); the default keeps the intrinsic [CoverWidth] x [CoverHeight].
 */
@Composable
fun Game.CoverImage(
    modifier: Modifier = Modifier,
    sizeModifier: Modifier = Modifier.width(CoverWidth).height(CoverHeight),
    contentScale: ContentScale = ContentScale.FillBounds
) = GameCoverImage(id.toInt(), cover?.image_id, name, modifier, sizeModifier, contentScale)

/** A cover preview that only needs the small values carried by the detail route. */
@Composable
fun GameCoverImage(
    gameId: Int,
    coverImageId: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    sizeModifier: Modifier = Modifier.width(CoverWidth).height(CoverHeight),
    contentScale: ContentScale = ContentScale.FillBounds,
) = RemoteImage(
    coverImageId?.let { igdbImageUrl(it, IgdbImageSize.COVER_BIG) } ?: "https://placehold.net/default.png",
    contentDescription = contentDescription,
    contentScale = contentScale,
    modifier = Modifier
        .sharedGameElement("cover-$gameId")
        .then(modifier)
        .then(sizeModifier),
    loadingModifier = sizeModifier
)

/** A wide game image (artwork or screenshot) from its IGDB [imageId]. */
@Composable
fun GameBannerImage(
    imageId: String,
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier,
    sharedKey: Any? = null,
    contentScale: ContentScale = ContentScale.FillBounds
) = RemoteImage(
    igdbImageUrl(imageId, IgdbImageSize.SCREENSHOT_HUGE),
    contentDescription = stringResource(Res.string.game__artwork_image),
    contentScale = contentScale,
    modifier = Modifier.sharedGameElement(sharedKey).then(modifier),
    loadingModifier = loadingModifier
)

@Composable
fun Artwork.Image(
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier,
    sharedKey: Any? = null
) = GameBannerImage(image_id, modifier, loadingModifier, sharedKey)

@Composable
fun Screenshot.Image(
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier,
    sharedKey: Any? = null
) = GameBannerImage(image_id, modifier, loadingModifier, sharedKey)
