package it.maicol07.gamerlogue.ui.views.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Event
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.events__empty
import gamerlogue.sharedui.generated.resources.events__logo
import gamerlogue.sharedui.generated.resources.events__past
import gamerlogue.sharedui.generated.resources.events__upcoming
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CelebrationW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.igdb.dateRangeLabel
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Thumbnail of an event logo in the list; 16:9 like the logo itself. */
private val ThumbWidth = 64.dp
private val ThumbHeight = 36.dp

/**
 * The full events list: upcoming events first, then the previous ones. Tapping an event opens the
 * game list scoped to that event's games.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EventListScreen(
    viewModel: EventsViewModel = koinViewModel(),
    onEventClick: (Event) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(listState.firstVisibleItemIndex, uiState.past.size) {
        val lastVisible = listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size
        viewModel.onEndReached(lastVisible)
    }

    when {
        uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        uiState.upcoming.isEmpty() && uiState.past.isEmpty() -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.events__empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> Box {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                eventGroup(Res.string.events__upcoming, uiState.upcoming, onEventClick)
                eventGroup(Res.string.events__past, uiState.past, onEventClick)
                if (uiState.loadingMorePast) {
                    item(key = "loading-more") {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    }
                }
            }
            AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}

/** A titled segmented list for one bucket; no-op when empty. */
private fun LazyListScope.eventGroup(
    titleRes: StringResource,
    events: List<Event>,
    onEventClick: (Event) -> Unit,
) {
    if (events.isEmpty()) return
    item(key = "header-$titleRes") {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
    }
    itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
        EventRow(event, index, events.size) { onEventClick(event) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EventRow(event: Event, indexInGroup: Int, groupCount: Int, onClick: () -> Unit) = SegmentedListItem(
    onClick = onClick,
    shapes = ListItemDefaults.segmentedShapes(index = indexInGroup, count = groupCount),
    colors = ListItemDefaults.expressiveSegmentedColors(),
    leadingContent = { EventThumb(event) },
    supportingContent = { Text(event.dateRangeLabel()) },
) { Text(event.name) }

@Composable
private fun EventThumb(event: Event) {
    val shape = MaterialTheme.shapes.extraSmall
    val sizeModifier = Modifier.size(width = ThumbWidth, height = ThumbHeight).clip(shape)
    val logo = event.event_logo
    if (logo == null) {
        Box(sizeModifier, contentAlignment = Alignment.Center) {
            Icon(Icons.CelebrationW500Rounded, contentDescription = null)
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
