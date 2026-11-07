package it.maicol07.gamerlogue.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.github.panpf.sketch.SubcomposeAsyncImage
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.request.ComposableImageRequest

private val SectionIconSize = 36.dp
private val SectionIconGlyphSize = 20.dp

/** [logoUrl], when set, is a remote logo shown in place of [icon]; [icon] stays the fallback if it fails. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SectionIcon(
    icon: ImageVector,
    shape: RoundedPolygon,
    containerColor: Color,
    contentColor: Color,
    logoUrl: String? = null,
) = Box(
    modifier = Modifier.size(SectionIconSize).background(containerColor, shape.toShape()),
    contentAlignment = Alignment.Center
) {
    val glyph = @Composable {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(SectionIconGlyphSize))
    }
    if (logoUrl == null) {
        glyph()
    } else {
        // Not RemoteImage: its loading slot is a square placeholder, here the glyph stands in while loading too.
        SubcomposeAsyncImage(
            ComposableImageRequest(logoUrl) {
                crossfade()
                resultCachePolicy(CachePolicy.DISABLED) // Same workaround as RemoteImage (sketch issue 295)
            },
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(SectionIconGlyphSize).clip(RoundedCornerShape(4.dp)),
            loading = { glyph() },
            error = { glyph() },
        )
    }
}
