package it.maicol07.gamerlogue.ui.components.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Event
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.events__logo
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CelebrationW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.dateRangeLabel
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.game.bottomScrim
import org.jetbrains.compose.resources.stringResource

/** Event logos are 16:9, so the card follows that ratio. */
val EventCardWidth = 260.dp
val EventCardHeight = 146.dp

/**
 * An event logo with its name and date range overlaid. The caller supplies the clip via [modifier]
 * (carousel: `maskClip`, list: `clip`), like [it.maicol07.gamerlogue.ui.components.game.GameCoverCard].
 */
@Composable
fun EventCard(
    event: Event,
    modifier: Modifier = Modifier,
    sizeModifier: Modifier = Modifier.width(EventCardWidth).height(EventCardHeight),
    onClick: (Event) -> Unit
) {
    Box(
        modifier = modifier
            .then(sizeModifier)
            .clickable { onClick(event) },
        contentAlignment = Alignment.BottomStart
    ) {
        EventLogo(event, Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = event.name,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = event.dateRangeLabel(),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** The event's logo, or a tonal placeholder for the events IGDB has no logo for. */
@Composable
private fun EventLogo(event: Event, modifier: Modifier) {
    val logo = event.event_logo
    if (logo == null) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = modifier
        ) {
            Box(Modifier.bottomScrim(), contentAlignment = Alignment.Center) {
                Icon(Icons.CelebrationW500Rounded, contentDescription = null, modifier = Modifier.size(48.dp))
            }
        }
    } else {
        RemoteImage(
            igdbImageUrl(logo.image_id, IgdbImageSize.LOGO_MEDIUM),
            contentDescription = stringResource(Res.string.events__logo, event.name),
            modifier = modifier.bottomScrim(),
            loadingModifier = modifier
        )
    }
}
