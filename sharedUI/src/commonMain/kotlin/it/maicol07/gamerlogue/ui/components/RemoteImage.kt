package it.maicol07.gamerlogue.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.sketch.SubcomposeAsyncImage
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.request.ImageRequest

@Composable
fun RemoteImage(
    url: String,
    contentDescription: String,
    contentScale: ContentScale = ContentScale.FillBounds,
    alignment: Alignment = Alignment.Center,
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier
) {
    RemoteImage(
        ComposableImageRequest(url) {
            crossfade()
        },
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = alignment,
        modifier = modifier,
        loadingModifier = loadingModifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RemoteImage(
    imageRequest: ImageRequest,
    contentDescription: String,
    contentScale: ContentScale = ContentScale.FillBounds,
    alignment: Alignment = Alignment.Center,
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        imageRequest,
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = alignment,
        loading = {
            Box(
                modifier = loadingModifier.background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        },
        modifier = modifier
    )
}
