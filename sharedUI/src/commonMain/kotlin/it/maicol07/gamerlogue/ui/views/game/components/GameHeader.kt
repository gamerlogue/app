package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Artwork
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.model.ReleaseDate
import at.released.igdbclient.model.Screenshot
import at.released.igdbclient.util.igdbImageUrl
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.displayDate
import it.maicol07.gamerlogue.extensions.igdb.mediaArtworks
import it.maicol07.gamerlogue.extensions.isVisible
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.game.CoverAspectRatio
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import it.maicol07.gamerlogue.ui.components.game.GameBannerImage
import it.maicol07.gamerlogue.ui.components.game.GameCoverImage
import it.maicol07.gamerlogue.ui.components.game.Image
import it.maicol07.gamerlogue.ui.components.imageviewer.FullscreenImageViewer
import it.maicol07.gamerlogue.ui.views.list.GameListPreset

private const val TITLE_VISIBILITY_THRESHOLD = 40

private val BannerHeight = 280.dp
private val DetailCoverSize = Modifier.width(168.dp).height(224.dp)

/** How far the cover hangs below the banner: half its height, so it straddles the edge. */
private val CoverOverlap = 112.dp
private val CoverDropShadow = androidx.compose.ui.graphics.shadow.Shadow(
    16.dp,
    spread = 2.dp,
    color = Color(0x3a000000),
    offset = DpOffset(0.dp, 6.dp)
)

/**
 * Hero of the screen: banner, a large centered cover across its edge, then title, meta and platforms.
 *
 * Banner and cover open the fullscreen viewer, on a gallery of the cover followed by the media images
 * (the banner is the first of those).
 */
@Composable
fun LazyItemScope.GameHeader(
    game: Game,
    onTitleVisibilityChange: (Boolean) -> Unit,
    onPresetClick: (GameListPreset) -> Unit
) = Column(Modifier.animateItem().fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    val coverId = game.cover?.image_id
    val galleryImages = remember(game) { listOfNotNull(coverId) + gameMediaImageIds(game) }
    val firstMediaPage = if (coverId != null) 1 else 0
    var viewerPage by remember { mutableStateOf<Int?>(null) }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        GameBanner(game, onClick = { viewerPage = firstMediaPage })
        game.CoverImage(
            Modifier.detailCover().clickable(enabled = coverId != null) { viewerPage = 0 },
            sizeModifier = DetailCoverSize
        )
    }
    Spacer(Modifier.height(CoverOverlap + 20.dp))

    Text(
        text = game.name,
        style = MaterialTheme.typography.headlineLargeEmphasized,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .isVisible(TITLE_VISIBILITY_THRESHOLD, onTitleVisibilityChange)
    )
    val meta = remember(game) {
        listOfNotNull(
            ReleaseDate(date = game.first_release_date).displayDate(),
            game.involved_companies.firstOrNull { it.developer }?.company?.name?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
    }
    if (meta.isNotEmpty()) {
        Text(
            meta,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp)
        )
    }
    GamePlatforms(game, onPresetClick, Modifier.padding(top = 16.dp))

    viewerPage?.let { page ->
        HeaderGallery(game.name, coverId, galleryImages, page) { viewerPage = null }
    }
}

/** Fullscreen viewer over the cover (page 0, when [coverId] is set) followed by the media [images]. */
@Composable
private fun HeaderGallery(
    gameName: String,
    coverId: String?,
    images: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
    val viewerImage: @Composable (Int, Modifier) -> Unit = { index, modifier ->
        if (coverId != null && index == 0) {
            RemoteImage(
                igdbImageUrl(coverId, IgdbImageSize.H1080P),
                contentDescription = gameName,
                modifier = modifier.aspectRatio(CoverAspectRatio)
            )
        } else {
            GameBannerImage(images[index], modifier.aspectRatio(Ratio169))
        }
    }
    FullscreenImageViewer(
        imagesCount = images.size,
        imageUrl = { page -> igdbImageUrl(images[page], IgdbImageSize.H1080P) },
        initialPage = initialPage,
        onDismissRequest = onDismiss,
        imageContent = { index, modifier -> viewerImage(index, modifier) },
        thumbnailContent = viewerImage
    )
}

/** Same geometry as [GameHeader]'s cover, so the loading → loaded swap does not jump. */
@Composable
fun GameDetailLoadingCover(gameId: Int, coverImageId: String?, contentDescription: String) {
    Box(Modifier.fillMaxWidth().height(BannerHeight), contentAlignment = Alignment.BottomCenter) {
        GameCoverImage(
            gameId = gameId,
            coverImageId = coverImageId,
            contentDescription = contentDescription,
            modifier = Modifier.detailCover(),
            sizeModifier = DetailCoverSize,
        )
    }
}

@Composable
private fun Modifier.detailCover() = offset(y = CoverOverlap)
    .dropShadow(MaterialTheme.shapes.extraLarge, CoverDropShadow)
    .clip(MaterialTheme.shapes.extraLarge)

@Composable
private fun GameBanner(game: Game, onClick: () -> Unit) {
    val banner = game.mediaArtworks.firstOrNull() ?: game.screenshots.firstOrNull()
    val backgroundColor = MaterialTheme.colorScheme.background
    val bannerModifier = Modifier
        .fillMaxWidth()
        .height(BannerHeight)
        .clickable(onClick = onClick)
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

    val bannerKey = "banner-${game.id}"
    when (banner) {
        is Artwork -> banner.Image(bannerModifier, bannerLoadingModifier, sharedKey = bannerKey)
        is Screenshot -> banner.Image(bannerModifier, bannerLoadingModifier, sharedKey = bannerKey)
        else -> Box(bannerLoadingModifier.background(Color.Black))
    }
}

private const val PlatformsToShow = 4
private val PlatformIconSize = Modifier.size(24.dp)

/** Platform logos in an outlined pill with a chevron, so it reads as a button; opens the release dates. */
@Composable
private fun GamePlatforms(game: Game, onPresetClick: (GameListPreset) -> Unit, modifier: Modifier) {
    if (game.platforms.isEmpty()) return
    val shown = game.platforms.take(PlatformsToShow)
    val extraCount = game.platforms.size - shown.size
    var showPlatformsSheet by remember { mutableStateOf(false) }

    Surface(
        onClick = { showPlatformsSheet = true },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (platform in shown) {
                platform.Image(PlatformIconSize, PlatformIconSize)
            }
            if (extraCount > 0) {
                Text("+$extraCount", style = MaterialTheme.typography.labelLarge)
            }
            Icon(
                Icons.KeyboardArrowRightW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showPlatformsSheet) {
        ReleaseDatesBottomSheet(
            game,
            onPlatformClick = {
                showPlatformsSheet = false
                onPresetClick(it)
            },
            onDismissRequest = { showPlatformsSheet = false }
        )
    }
}
