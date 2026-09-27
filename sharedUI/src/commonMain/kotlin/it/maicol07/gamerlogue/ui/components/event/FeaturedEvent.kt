package it.maicol07.gamerlogue.ui.components.event

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Event
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.events__next
import it.maicol07.gamerlogue.ui.components.pressMorphShape
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val FeaturedEventCorner = 28.dp
private val FeaturedEventPressedCorner = 44.dp
private const val FEATURED_EVENT_ASPECT_RATIO = 16f / 9f

/** The next event, full width under a "Next up" label; also the lead of the events list. */
@Composable
fun FeaturedEvent(event: Event, modifier: Modifier, onEventClick: (Event) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
        ) {
            BucketLabel(Res.string.events__next, Modifier)
            EventStatusPill(event)
        }
        EventCard(
            event = event,
            modifier = Modifier.clip(
                pressMorphShape(interactionSource, FeaturedEventCorner, FeaturedEventPressedCorner)
            ),
            sizeModifier = Modifier.fillMaxWidth().aspectRatio(FEATURED_EVENT_ASPECT_RATIO),
            interactionSource = interactionSource,
            onClick = onEventClick
        )
    }
}

/** Title of a group of events, e.g. "Next up" or "Upcoming". */
@Composable
fun BucketLabel(label: StringResource, modifier: Modifier) =
    Text(text = stringResource(label), style = MaterialTheme.typography.titleSmall, modifier = modifier)
