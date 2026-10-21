package it.maicol07.gamerlogue.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import co.touchlab.kermit.Logger
import com.github.panpf.sketch.SubcomposeAsyncImage
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.LoadState

/** [url]'s image; a failed load shows [failure], or nothing without one. */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String,
    contentScale: ContentScale = ContentScale.FillBounds,
    alignment: Alignment = Alignment.Center,
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier,
    failure: (@Composable () -> Unit)? = null,
) {
    RemoteImage(
        ComposableImageRequest(url) {
            crossfade()
            resultCachePolicy(CachePolicy.DISABLED) // Disable to avoid nosuchmetoderror. @see https://github.com/panpf/sketch/issues/295
        },
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = alignment,
        modifier = modifier,
        loadingModifier = loadingModifier,
        failure = failure,
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
    loadingModifier: Modifier = Modifier,
    failure: (@Composable () -> Unit)? = null,
) {
    // Compose requests take no listener: failures are read from the state, and logged with their URL.
    val state = rememberAsyncImageState()
    val error = state.loadState as? LoadState.Error
    LaunchedEffect(error) {
        if (error != null) Logger.w(error.result.throwable) { "Remote image failed: ${error.request.uri}" }
    }
    SubcomposeAsyncImage(
        imageRequest,
        state = state,
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
        error = failure?.let { content -> { content() } },
        modifier = modifier
    )
}
