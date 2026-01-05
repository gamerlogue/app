package it.maicol07.gamerlogue.ui.components.game

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Artwork
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.model.Screenshot
import at.released.igdbclient.util.igdbImageUrl
import it.maicol07.gamerlogue.ui.components.RemoteImage

@Composable
fun Game.CoverImage(modifier: Modifier = Modifier) = RemoteImage(
    cover?.let {
        igdbImageUrl(
            cover!!.image_id,
            IgdbImageSize.COVER_BIG
        )
    } ?: "https://placehold.net/default.png",
    contentDescription = name,
    modifier = modifier
        .width(150.dp)
        .height(200.dp),
    loadingModifier = Modifier
        .width(150.dp)
        .height(200.dp)
)

@Composable
fun Artwork.Image(
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier
) = RemoteImage(
    igdbImageUrl(image_id, IgdbImageSize.SCREENSHOT_HUGE),
    contentDescription = "Artwork Image",
    modifier = modifier,
    loadingModifier = loadingModifier
)

@Composable
fun Screenshot.Image(
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier
) = RemoteImage(
    igdbImageUrl(image_id, IgdbImageSize.SCREENSHOT_HUGE),
    contentDescription = "Artwork Image",
    modifier = modifier,
    loadingModifier = loadingModifier
)
