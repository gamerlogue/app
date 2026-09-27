package it.maicol07.gamerlogue.ui.views.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Event
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.events__empty
import gamerlogue.sharedui.generated.resources.events__error
import gamerlogue.sharedui.generated.resources.events__logo
import gamerlogue.sharedui.generated.resources.events__past
import gamerlogue.sharedui.generated.resources.events__retry
import gamerlogue.sharedui.generated.resources.events__upcoming
import gamerlogue.sharedui.generated.resources.nav__events
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CelebrationW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.igdb.dateRangeLabel
import it.maicol07.gamerlogue.extensions.igdb.gamesNavKey
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.event.EventStatusPill
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.ListPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.discover.FeaturedEvent
import it.maicol07.gamerlogue.ui.views.discover.SectionIcon
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

/** Thumbnail of an event logo in the list; 16:9 like the logo itself. */
private val ThumbWidth = 96.dp
private val ThumbHeight = 54.dp

private val StatusIconSize = 96.dp
private val StatusGlyphSize = 48.dp

/**
 * The full events list: the next event featured, the other upcoming ones, then the previous ones
 * grouped by year under sticky headers. Tapping an event opens the game list scoped to its games.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Branch(RootTree::class, metadata = ListPaneMetadata::class)
@Composable
fun EventListView(
    viewModel: EventsViewModel = koinViewModel(parameters = { parametersOf(EventsViewModel.LIST_PAGE_SIZE) }),
) {
    val navigationState = LocalNavigationState.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val onEventClick: (Event) -> Unit = { navigationState.backStack.add(it.gamesNavKey) }

    LaunchedEffect(listState.firstVisibleItemIndex, uiState.past.size) {
        viewModel.onEndReached(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0)
    }

    ScreenScaffold(title = Res.string.nav__events) {
        when {
            uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ContainedLoadingIndicator()
            }
            uiState.error -> EventsStatus(Res.string.events__error, Modifier.fillMaxSize()) { viewModel.load() }
            uiState.upcoming.isEmpty() && uiState.past.isEmpty() ->
                EventsStatus(Res.string.events__empty, Modifier.fillMaxSize(), onRetry = null)
            else -> Box {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    upcomingSection(uiState.upcoming, onEventClick)
                    pastSection(uiState.past, onEventClick)
                    if (uiState.loadingMorePast) {
                        item(key = "loading-more") {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                LoadingIndicator()
                            }
                        }
                    }
                    if (uiState.pastPageError) {
                        item(key = "page-error") {
                            PageError(Modifier.fillMaxWidth().padding(vertical = 16.dp).animateItem()) {
                                viewModel.loadMorePast()
                            }
                        }
                    }
                }
                AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }
}

/** The next event featured, then the other upcoming ones; no-op when there are none. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.upcomingSection(events: List<Event>, onEventClick: (Event) -> Unit) {
    val featured = events.firstOrNull() ?: return
    item(key = "header-upcoming") {
        EventSectionHeader(
            title = Res.string.events__upcoming,
            iconShape = MaterialShapes.Flower,
            iconContainerColor = MaterialTheme.colorScheme.tertiary,
            iconColor = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier.animateItem(),
        )
    }
    item(key = featured.id) {
        FeaturedEvent(featured, Modifier.padding(bottom = Dimens.ItemGap).animateItem(), onEventClick)
    }
    eventRows(events.drop(1), onEventClick)
}

/** Previous events grouped by the year they started in; no-op when there are none. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.pastSection(events: List<Event>, onEventClick: (Event) -> Unit) {
    if (events.isEmpty()) return
    item(key = "header-past") {
        EventSectionHeader(
            title = Res.string.events__past,
            iconShape = MaterialShapes.Clover4Leaf,
            iconContainerColor = MaterialTheme.colorScheme.secondary,
            iconColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.animateItem(),
        )
    }
    events.groupBy { it.startYear() }.forEach { (year, group) ->
        stickyHeader(key = "year-$year") { YearHeader(year) }
        eventRows(group, onEventClick)
    }
}

/** One segmented list: the first and last rows get the rounded outer corners. */
private fun LazyListScope.eventRows(events: List<Event>, onEventClick: (Event) -> Unit) =
    itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
        EventRow(event, index, events.size, Modifier.animateItem()) { onEventClick(event) }
    }

@Composable
private fun EventSectionHeader(
    title: StringResource,
    iconShape: RoundedPolygon,
    iconContainerColor: Color,
    iconColor: Color,
    modifier: Modifier,
) = Row(
    modifier = modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
) {
    SectionIcon(Icons.CelebrationW500Rounded, iconShape, iconContainerColor, iconColor)
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleLargeEmphasized,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Opaque, so the rows scrolling under it stay hidden. */
@Composable
private fun YearHeader(year: Int?) = Surface(color = MaterialTheme.colorScheme.background) {
    Text(
        // Same placeholder as the date range of an undated event.
        text = year?.toString() ?: "TBA",
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EventRow(event: Event, indexInGroup: Int, groupCount: Int, modifier: Modifier, onClick: () -> Unit) =
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = indexInGroup, count = groupCount),
        colors = ListItemDefaults.expressiveSegmentedColors(),
        modifier = modifier,
        leadingContent = { EventThumb(event) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.dateRangeLabel())
                EventStatusPill(event)
            }
        },
    ) { Text(event.name, maxLines = 2, overflow = TextOverflow.Ellipsis) }

/** Full-screen state for an empty list or a failed first load, the latter with its retry. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EventsStatus(text: StringResource, modifier: Modifier, onRetry: (() -> Unit)?) = Column(
    modifier = modifier.padding(Dimens.ScreenPadding),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Box(
        modifier = Modifier
            .size(StatusIconSize)
            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.Cookie9Sided.toShape()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.CelebrationW500Rounded,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(StatusGlyphSize),
        )
    }
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (onRetry != null) RetryButton(onRetry)
}

/** A failed later page, as the last row of the list. */
@Composable
private fun PageError(modifier: Modifier, onRetry: () -> Unit) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
) {
    Text(
        text = stringResource(Res.string.events__error),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
    )
    RetryButton(onRetry)
}

@Composable
private fun RetryButton(onRetry: () -> Unit) = FilledTonalButton(onClick = onRetry, shapes = ButtonDefaults.shapes()) {
    Text(stringResource(Res.string.events__retry))
}

@Composable
private fun EventThumb(event: Event) {
    val shape = MaterialTheme.shapes.medium
    val sizeModifier = Modifier.size(width = ThumbWidth, height = ThumbHeight).clip(shape)
    val logo = event.event_logo
    if (logo == null) {
        // Tonal placeholder, like the one of the event cards.
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = shape, modifier = sizeModifier) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.CelebrationW500Rounded, contentDescription = null)
            }
        }
    } else {
        RemoteImage(
            url = igdbImageUrl(logo.image_id, IgdbImageSize.LOGO_MEDIUM),
            contentDescription = stringResource(Res.string.events__logo, event.name),
            modifier = sizeModifier,
            loadingModifier = sizeModifier,
        )
    }
}

/** UTC, like [dateRangeLabel], so an event sits under the year its dates show. */
private fun Event.startYear(): Int? =
    start_time?.let { Instant.fromEpochSeconds(it.getEpochSecond()).toLocalDateTime(TimeZone.UTC).year }
