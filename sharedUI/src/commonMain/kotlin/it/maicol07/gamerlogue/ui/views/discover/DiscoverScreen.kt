package it.maicol07.gamerlogue.ui.views.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Artwork
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.ReleaseDate
import at.released.igdbclient.model.Screenshot
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.home__empty_section
import gamerlogue.sharedui.generated.resources.home__see_all
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowForwardW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.displayDate
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import it.maicol07.gamerlogue.ui.components.game.Image
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val CardWidth = 150.dp
private val CardHeight = 200.dp
private val HeroWidth = 340.dp
private val HeroHeight = 200.dp

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = koinViewModel(),
    onGameClick: (Game) -> Unit,
    onSeeAllClick: (DiscoverSection) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        DiscoverSection.entries.forEachIndexed { index, section ->
            val sectionState = uiState.sections[section] ?: DiscoverViewModel.SectionUiState()
            discoverSection(
                section = section,
                state = sectionState,
                hero = index == 0,
                onGameClick = onGameClick,
                onSeeAllClick = { onSeeAllClick(section) }
            )
        }
    }
}

private fun LazyListScope.discoverSection(
    section: DiscoverSection,
    state: DiscoverViewModel.SectionUiState,
    hero: Boolean,
    onGameClick: (Game) -> Unit,
    onSeeAllClick: () -> Unit
) {
    item(key = section.name) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(section.sectionTitle, section.icon, onSeeAllClick)
            when {
                state.loading -> CarouselLoading(if (hero) HeroWidth else CardWidth)
                state.games.isEmpty() -> EmptySection()
                hero -> HeroCarousel(state.games, onGameClick)
                else -> GameCarousel(section, state.games, onGameClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionHeader(
    title: StringResource,
    icon: ImageVector,
    onSeeAllClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        TextButton(shapes = ButtonDefaults.shapes(), onClick = onSeeAllClick) {
            Text(stringResource(Res.string.home__see_all))
            Spacer(Modifier.width(4.dp))
            Icon(Icons.ArrowForwardW500Rounded, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeroCarousel(games: List<Game>, onGameClick: (Game) -> Unit) {
    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { games.count() },
        modifier = Modifier.fillMaxWidth().height(HeroHeight),
        preferredItemWidth = HeroWidth,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { i ->
        HeroItem(games[i], onGameClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarouselItemScope.HeroItem(game: Game, onItemClick: (Game) -> Unit) {
    Box(
        modifier = Modifier
            .width(HeroWidth)
            .height(HeroHeight)
            .maskClip(MaterialTheme.shapes.extraLarge)
            .clickable { onItemClick(game) },
        contentAlignment = Alignment.BottomStart
    ) {
        val imageModifier = Modifier.fillMaxWidth().height(HeroHeight).bottomScrim()
        val loadingModifier = Modifier.fillMaxWidth().height(HeroHeight)
        val bannerKey = "banner-${game.id}"
        when (val banner = game.artworks.firstOrNull() ?: game.screenshots.firstOrNull()) {
            is Artwork -> banner.Image(imageModifier, loadingModifier, sharedKey = bannerKey)
            is Screenshot -> banner.Image(imageModifier, loadingModifier, sharedKey = bannerKey)
            else -> game.CoverImage(Modifier.bottomScrim())
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = game.name,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            game.ratingLabel()?.let {
                Text(it, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GameCarousel(section: DiscoverSection, games: List<Game>, onGameClick: (Game) -> Unit) {
    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { games.count() },
        modifier = Modifier.fillMaxWidth().height(CardHeight),
        preferredItemWidth = CardWidth,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { i ->
        val game = games[i]
        GameCard(
            game = game,
            metadata = section.cardMetadata(game),
            showTitle = carouselItemDrawInfo.size > 200,
            onItemClick = onGameClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarouselItemScope.GameCard(
    game: Game,
    metadata: String?,
    showTitle: Boolean,
    onItemClick: (Game) -> Unit
) {
    Box(contentAlignment = Alignment.BottomStart) {
        val coverModifier = Modifier
            .maskClip(MaterialTheme.shapes.large)
            .let { if (showTitle) it.bottomScrim() else it }

        game.CoverImage(coverModifier.clickable { onItemClick(game) })

        if (metadata != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        AnimatedVisibility(showTitle) {
            Text(
                text = game.name,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), blurRadius = 2f)
                ),
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CarouselLoading(itemWidth: Dp) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        repeat(4) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.width(itemWidth).height(CardHeight)
            ) {
                Box(contentAlignment = Alignment.Center) { LoadingIndicator() }
            }
        }
    }
}

@Composable
private fun EmptySection() {
    Text(
        text = stringResource(Res.string.home__empty_section),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

private fun Modifier.bottomScrim() = drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
            startY = size.height * 0.35f,
            endY = size.height
        )
    )
}

/** Star + score (0-10), or null when the game has no rating. */
private fun Game.ratingLabel(): String? = rating.takeIf { it > 0.0 }?.let { "★ %.1f".sprintf(it / 10) }

/** Per-section metadata badge shown on a cover card. */
private fun DiscoverSection.cardMetadata(game: Game): String? = when (this) {
    DiscoverSection.MOST_LOVED -> game.ratingLabel()
    DiscoverSection.RECENTLY_RELEASED, DiscoverSection.UPCOMING ->
        ReleaseDate(date = game.first_release_date).displayDate()
    else -> null
}
