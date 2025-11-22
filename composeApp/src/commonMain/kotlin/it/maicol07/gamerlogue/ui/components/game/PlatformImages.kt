package it.maicol07.gamerlogue.ui.components.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import at.released.igdbclient.model.Platform
import at.released.igdbclient.util.igdbImageUrl
import it.maicol07.gamerlogue.ui.components.RemoteImage

@Composable
fun Platform.Image(
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier
) = RemoteImage(
    url = platform_logo?.let {
        igdbImageUrl(
            platform_logo!!.image_id,
            at.released.igdbclient.model.IgdbImageSize.LOGO_MEDIUM
        )
    } ?: "https://placehold.net/64x64.png",
    contentDescription = name,
    contentScale = ContentScale.FillBounds,
    modifier = modifier,
    loadingModifier = loadingModifier
)

// TODO: Add a Platform.Icon composable if needed with ImageVectors
