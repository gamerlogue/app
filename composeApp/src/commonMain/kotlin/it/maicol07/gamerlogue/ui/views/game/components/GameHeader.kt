package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Artwork
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.Screenshot
import it.maicol07.gamerlogue.extensions.isVisible
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import it.maicol07.gamerlogue.ui.components.game.LocalGameTopBarOverlayMode
import it.maicol07.gamerlogue.ui.components.game.Image

private val BannerHeight = 260.dp
private val CoverWidth = 140.dp
private val CoverOverlap = 36.dp
private val CoverDropShadow = androidx.compose.ui.graphics.shadow.Shadow(
    10.dp,
    spread = 4.dp,
    color = Color(0x2a000000),
    offset = DpOffset(2.dp, 2.dp)
)

@Composable
fun LazyItemScope.GameHeader(
    game: Game
) {
    Box(modifier = Modifier.animateItem().fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
        GameBanner(game)
        game.CoverImage(
            Modifier
                .padding(start = 16.dp)
                .offset(y = CoverOverlap)
                .dropShadow(MaterialTheme.shapes.large, CoverDropShadow)
                .clip(MaterialTheme.shapes.large)
                .clickable {
//                    showViewer = true
                }
        )

        // Title and metadata on the banner, moved to the right of the cover
        Column(
            Modifier.padding(start = 16.dp + CoverWidth + 16.dp, end = 16.dp, bottom = 36.dp)
        ) {
            val topBarState = LocalGameTopBarOverlayMode.current

            Text(
                text = game.name,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 2f)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.isVisible(40) { topBarState.value = it }
            )

            GamePlatforms(game)
        }
    }

    // Add spacer to compensate for the overlap of the cover image
    Spacer(Modifier.height(CoverOverlap))
}

@Composable
private fun GameBanner(game: Game) {
    val banner = game.artworks.firstOrNull() ?: game.screenshots.firstOrNull()
    val backgroundColor = MaterialTheme.colorScheme.background
    val bannerModifier = Modifier
        .fillMaxWidth()
        .height(BannerHeight)
        .clickable {
//            showViewer = true
        }
        .drawWithContent {
            drawContent()
            // Top scrim for readability of the top bar and icons
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.38f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.25f
                )
            )
            // Bottom scrim for title readability on banner
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, backgroundColor),
                    startY = size.height * 0.4f,
                    endY = size.height
                )
            )
        }
    val bannerLoadingModifier = Modifier
        .fillMaxWidth()
        .height(BannerHeight)

    when (banner) {
        is Artwork -> banner.Image(bannerModifier, bannerLoadingModifier)
        is Screenshot -> banner.Image(bannerModifier, bannerLoadingModifier)
        else -> {}
    }
}

const val GamePlatformsBadgeToShow = 3
const val GamePlatformsBadgeTransparency = 0.7f

@Composable
private fun GamePlatforms(game: Game) {
    if (game.platforms.isNotEmpty()) {
        val first3 = game.platforms.take(GamePlatformsBadgeToShow)
        val extraCount = (game.platforms.size - first3.size).coerceAtLeast(0)
        var showPlatformsSheet by remember { mutableStateOf(false) }

        Badge(
            Modifier.clickable { showPlatformsSheet = true },
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(GamePlatformsBadgeTransparency)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(4.dp)
            ) {
                for (platform in first3) {
                    platform.Image(
                        Modifier.width(24.dp).height(24.dp),
                        Modifier.width(24.dp).height(24.dp)
                    )
                }
                if (extraCount > 0) {
                    Text(
                        "+$extraCount",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (showPlatformsSheet) {
            ReleaseDatesBottomSheet(game) { showPlatformsSheet = false }
        }
    }
}
