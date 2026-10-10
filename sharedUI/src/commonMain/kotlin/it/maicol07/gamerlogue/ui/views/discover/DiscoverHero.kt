package it.maicol07.gamerlogue.ui.views.discover

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.home__empty_section
import gamerlogue.sharedui.generated.resources.home__section_error
import it.maicol07.gamerlogue.extensions.igdb.mediaArtworks
import it.maicol07.gamerlogue.extensions.igdb.ratingLabel
import it.maicol07.gamerlogue.extensions.mouseDragScrollsHorizontally
import it.maicol07.gamerlogue.ui.components.CarouselWithArrows
import it.maicol07.gamerlogue.ui.components.SectionIcon
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import it.maicol07.gamerlogue.ui.components.game.GameBannerImage
import it.maicol07.gamerlogue.ui.components.game.bottomScrim
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

private val ImmersiveHeroHeight = 480.dp
private val ImmersiveHeroBottomCorner = 32.dp
private val ImmersiveTopScrimHeight = 160.dp
private val ImmersiveTextBottomPadding = 72.dp
private val PageSegmentHeight = 4.dp
private val PageSegmentGap = 4.dp
private const val HERO_GAME_COUNT = 10
private const val PAGE_SEGMENT_TRACK_ALPHA = 0.3f

/**
 * The page's hero: edge-to-edge artwork, one game per page, drawn under the status bar and the
 * floating search bar. The section title and "See all" move onto the artwork.
 *
 * Only the top [HERO_GAME_COUNT] games are paged, so the segmented indicator stays readable; the
 * rest of the section is behind "See all".
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ImmersiveHero(
    section: DiscoverSection,
    state: DiscoverViewModel.SectionUiState,
    onGameClick: (Game) -> Unit,
    onSeeAllClick: () -> Unit
) = Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(ImmersiveHeroHeight)
        // Rounded at the bottom so the artwork does not end on a hard edge against the page.
        .clip(RoundedCornerShape(bottomStart = ImmersiveHeroBottomCorner, bottomEnd = ImmersiveHeroBottomCorner))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    contentAlignment = Alignment.Center
) {
    when {
        state.loading -> ContainedLoadingIndicator()
        state.error -> SectionMessage(Res.string.home__section_error)
        state.games.isEmpty() -> SectionMessage(Res.string.home__empty_section)
        else -> {
            val games = state.games.take(HERO_GAME_COUNT)
            val pagerState = rememberPagerState { games.size }
            CarouselWithArrows(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                onBack = { pagerState.animateScrollToPage(pagerState.currentPage - 1) },
                onForward = { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            ) {
                HorizontalPager(pagerState, Modifier.fillMaxSize().mouseDragScrollsHorizontally(pagerState)) { page ->
                    ImmersivePage(section, games[page], rank = page + 1, onGameClick)
                }
            }
            // Keeps the status bar and the floating search bar legible over bright artwork.
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(ImmersiveTopScrimHeight)
                    .background(
                        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, Color.Transparent))
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(Dimens.ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.ScreenPadding)
            ) {
                PageSegments(
                    pageCount = games.size,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier.weight(1f)
                )
                SeeAllButton(onSeeAllClick)
            }
        }
    }
}

/** One segment per page, only the current one lit: shows both the position and the total. */
@Composable
private fun PageSegments(pageCount: Int, currentPage: Int, modifier: Modifier) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(PageSegmentGap)
) {
    repeat(pageCount) { page ->
        val color by animateColorAsState(
            targetValue = if (page == currentPage) Color.White else Color.White.copy(alpha = PAGE_SEGMENT_TRACK_ALPHA),
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
        )
        Box(Modifier.weight(1f).height(PageSegmentHeight).background(color, CircleShape))
    }
}

@Composable
private fun ImmersivePage(section: DiscoverSection, game: Game, rank: Int, onGameClick: (Game) -> Unit) = Box(
    modifier = Modifier.fillMaxSize().clickable { onGameClick(game) },
    contentAlignment = Alignment.BottomStart
) {
    GameBanner(game, Modifier.fillMaxSize().bottomScrim())
    Column(
        modifier = Modifier.padding(
            start = Dimens.ScreenPadding,
            end = Dimens.ScreenPadding,
            bottom = ImmersiveTextBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
        ) {
            SectionIcon(
                icon = section.icon,
                shape = section.iconShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(section.sectionTitle),
                color = Color.White,
                style = MaterialTheme.typography.titleMediumEmphasized
            )
        }
        Text(
            text = "#$rank",
            color = MaterialTheme.colorScheme.primaryFixed,
            style = MaterialTheme.typography.displayLargeEmphasized
        )
        Text(
            text = game.name,
            color = Color.White,
            style = MaterialTheme.typography.headlineMediumEmphasized,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        game.ratingLabel()?.let {
            Text(it, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** The game's first artwork or screenshot, falling back to the cover. */
@Composable
private fun GameBanner(game: Game, modifier: Modifier) = when (
    val bannerId = game.mediaArtworks.firstOrNull()?.image_id ?: game.screenshots.firstOrNull()?.image_id
) {
    null -> game.CoverImage(modifier, sizeModifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    else -> GameBannerImage(
        imageId = bannerId,
        modifier = modifier,
        loadingModifier = Modifier.fillMaxSize(),
        sharedKey = "banner-${game.id}",
        contentScale = ContentScale.Crop
    )
}
