package it.maicol07.gamerlogue.ui.components.game

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.sharedGameElement(key: Any?): Modifier {
    if (key == null) return this
    val scope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(scope) { sharedElement(rememberSharedContentState(key), animatedScope) }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Game.CoverImage(modifier: Modifier = Modifier) = RemoteImage(
    cover?.let { igdbImageUrl(cover!!.image_id, IgdbImageSize.COVER_BIG) }
        ?: "https://placehold.net/default.png",
    contentDescription = name,
    modifier = Modifier
        .sharedGameElement("cover-$id")
        .then(modifier)
        .width(150.dp)
        .height(200.dp),
    loadingModifier = Modifier
        .width(150.dp)
        .height(200.dp)
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Artwork.Image(
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier,
    sharedKey: Any? = null
) = RemoteImage(
    igdbImageUrl(image_id, IgdbImageSize.SCREENSHOT_HUGE),
    contentDescription = stringResource(Res.string.game__artwork_image),
    modifier = Modifier.sharedGameElement(sharedKey).then(modifier),
    loadingModifier = loadingModifier
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Screenshot.Image(
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier,
    sharedKey: Any? = null
) = RemoteImage(
    igdbImageUrl(image_id, IgdbImageSize.SCREENSHOT_HUGE),
    contentDescription = stringResource(Res.string.game__artwork_image),
    modifier = Modifier.sharedGameElement(sharedKey).then(modifier),
    loadingModifier = loadingModifier
)
