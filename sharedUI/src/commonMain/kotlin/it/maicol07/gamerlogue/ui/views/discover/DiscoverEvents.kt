package it.maicol07.gamerlogue.ui.views.discover

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import at.released.igdbclient.model.Event
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.events__empty
import gamerlogue.sharedui.generated.resources.events__error
import gamerlogue.sharedui.generated.resources.events__past
import gamerlogue.sharedui.generated.resources.events__upcoming
import gamerlogue.sharedui.generated.resources.home__events
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CelebrationW500Rounded
import it.maicol07.gamerlogue.ui.components.GameCoverCarousel
import it.maicol07.gamerlogue.ui.components.event.BucketLabel
import it.maicol07.gamerlogue.ui.components.event.EventCard
import it.maicol07.gamerlogue.ui.components.event.EventCardHeight
import it.maicol07.gamerlogue.ui.components.event.EventCardWidth
import it.maicol07.gamerlogue.ui.components.event.FeaturedEvent
import it.maicol07.gamerlogue.ui.components.pressMorphShape
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.events.EventsViewModel
import org.jetbrains.compose.resources.StringResource

/**
 * The events block, the page's second highlight: a tonal container with the next event featured
 * full width, then the rest of the upcoming events, and the previous ones.
 *
 * Events are not games, so they live outside [DiscoverSection] (whose queries and nav key are typed
 * against games) and are appended as their own item.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun LazyListScope.eventsSection(
    state: EventsViewModel.UiState,
    onEventClick: (Event) -> Unit,
    onSeeAllClick: () -> Unit
) = item(key = "EVENTS") {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ItemGap)
    ) {
        Column(
            modifier = Modifier.padding(vertical = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap)
        ) {
            SectionHeader(
                title = Res.string.home__events,
                icon = Icons.CelebrationW500Rounded,
                iconShape = MaterialShapes.Flower,
                iconContainerColor = MaterialTheme.colorScheme.tertiary,
                iconColor = MaterialTheme.colorScheme.onTertiary,
                onSeeAllClick = onSeeAllClick
            )
            when {
                state.loading -> SectionLoading(EventCardWidth, EventCardHeight)
                state.error -> SectionMessage(Res.string.events__error)
                state.upcoming.isEmpty() && state.past.isEmpty() -> SectionMessage(Res.string.events__empty)
                else -> {
                    state.upcoming.firstOrNull()?.let {
                        FeaturedEvent(it, Modifier.padding(horizontal = Dimens.ScreenPadding), onEventClick)
                    }
                    EventBucket(Res.string.events__upcoming, state.upcoming.drop(1), onEventClick)
                    EventBucket(Res.string.events__past, state.past, onEventClick)
                }
            }
        }
    }
}

@Composable
private fun EventBucket(label: StringResource, events: List<Event>, onEventClick: (Event) -> Unit) {
    if (events.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
        BucketLabel(label, Modifier.padding(horizontal = Dimens.ScreenPadding))
        GameCoverCarousel(
            itemCount = events.count(),
            preferredItemWidth = EventCardWidth,
            modifier = Modifier.height(EventCardHeight)
        ) { i ->
            val interactionSource = remember { MutableInteractionSource() }
            EventCard(
                event = events[i],
                modifier = Modifier.maskClip(pressMorphShape(interactionSource, CardCorner, CardPressedCorner)),
                // The carousel sizes its items, and a large item can be wider than the preferred width.
                sizeModifier = Modifier.fillMaxSize(),
                interactionSource = interactionSource,
                onClick = onEventClick
            )
        }
    }
}
