package it.maicol07.gamerlogue.ui.views.discover

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Artwork
import at.released.igdbclient.model.Event
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.Screenshot
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.events__empty
import gamerlogue.sharedui.generated.resources.events__past
import gamerlogue.sharedui.generated.resources.events__upcoming
import gamerlogue.sharedui.generated.resources.home__empty_section
import gamerlogue.sharedui.generated.resources.home__events
import gamerlogue.sharedui.generated.resources.home__see_all
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowForwardW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CelebrationW500Rounded
import it.maicol07.gamerlogue.ui.components.GameCoverCarousel
import it.maicol07.gamerlogue.ui.components.event.EventCard
import it.maicol07.gamerlogue.ui.components.event.EventCardHeight
import it.maicol07.gamerlogue.ui.components.event.EventCardWidth
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import it.maicol07.gamerlogue.ui.components.game.GameCoverCard
import it.maicol07.gamerlogue.ui.components.game.Image
import it.maicol07.gamerlogue.ui.components.game.bottomScrim
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.views.events.EventsViewModel
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
    eventsViewModel: EventsViewModel = koinViewModel(),
    onGameClick: (Game) -> Unit,
    onSeeAllClick: (DiscoverSection) -> Unit = {},
    onEventClick: (Event) -> Unit = {},
    onSeeAllEventsClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val eventsState by eventsViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    Box {
        LazyColumn(
            state = listState,
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
            eventsSection(eventsState, onEventClick, onSeeAllEventsClick)
        }
        AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
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

/**
 * The events section: one carousel for the upcoming events and one for the previous ones.
 *
 * Events are not games, so they live outside [DiscoverSection] (whose queries and nav key are typed
 * against games) and are appended as their own item.
 */
private fun LazyListScope.eventsSection(
    state: EventsViewModel.UiState,
    onEventClick: (Event) -> Unit,
    onSeeAllClick: () -> Unit
) {
    item(key = "EVENTS") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Res.string.home__events, Icons.CelebrationW500Rounded, onSeeAllClick)
            when {
                state.loading -> CarouselLoading(EventCardWidth, EventCardHeight)
                state.upcoming.isEmpty() && state.past.isEmpty() -> EmptySection(Res.string.events__empty)
                else -> {
                    EventBucket(Res.string.events__upcoming, state.upcoming, onEventClick)
                    EventBucket(Res.string.events__past, state.past, onEventClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventBucket(label: StringResource, events: List<Event>, onEventClick: (Event) -> Unit) {
    if (events.isEmpty()) return
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    GameCoverCarousel(
        itemCount = events.count(),
        preferredItemWidth = EventCardWidth,
        modifier = Modifier.height(EventCardHeight)
    ) { i ->
        EventCard(
            event = events[i],
            modifier = Modifier.maskClip(MaterialTheme.shapes.large),
            onClick = onEventClick
        )
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
    GameCoverCarousel(
        itemCount = games.count(),
        preferredItemWidth = HeroWidth,
        modifier = Modifier.height(HeroHeight)
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
    GameCoverCarousel(
        itemCount = games.count(),
        preferredItemWidth = CardWidth,
        modifier = Modifier.height(CardHeight)
    ) { i ->
        val game = games[i]
        GameCoverCard(
            game = game,
            metadata = listOfNotNull(section.cardMetadata(game)),
            showTitle = carouselItemDrawInfo.size > CardTitleThreshold,
            modifier = Modifier.maskClip(MaterialTheme.shapes.large),
            onClick = onGameClick
        )
    }
}

private const val CardTitleThreshold = 200

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CarouselLoading(itemWidth: Dp, itemHeight: Dp = CardHeight) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        repeat(4) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.width(itemWidth).height(itemHeight)
            ) {
                Box(contentAlignment = Alignment.Center) { LoadingIndicator() }
            }
        }
    }
}

@Composable
private fun EmptySection(text: StringResource = Res.string.home__empty_section) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

