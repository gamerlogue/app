package it.maicol07.gamerlogue.ui.components.event

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import at.released.igdbclient.model.Event
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.events__live_stream
import gamerlogue.sharedui.generated.resources.events__logo
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LanguageW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlayCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.TwitchSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.XSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.YoutubeSimpleIcons
import it.maicol07.gamerlogue.extensions.igdb.dateTimeRangeLabel
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.ui.components.ConnectedButtonGroup
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.Icons as SimpleIcons

/** Logo aspect ratio, so the header scales with the pane instead of a fixed height. */
private const val LogoAspectRatio = 16f / 9f

/** Collapsed description length; IGDB descriptions run to several paragraphs. */
private const val DescriptionCollapsedLines = 4

/**
 * Everything IGDB exposes about an event: logo, name, span, description and its links (live stream,
 * socials). Used as the [it.maicol07.gamerlogue.ui.views.list.GameListResults] header when the list
 * is scoped to an event.
 */
@Composable
fun EventHeader(event: Event, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    var descriptionExpanded by remember(event.id) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
    ) {
        event.event_logo?.let { logo ->
            val imageModifier = Modifier
                .fillMaxWidth()
                .aspectRatio(LogoAspectRatio)
                .clip(MaterialTheme.shapes.large)
            RemoteImage(
                url = igdbImageUrl(logo.image_id, IgdbImageSize.LOGO_MEDIUM),
                contentDescription = stringResource(Res.string.events__logo, event.name),
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
                loadingModifier = imageModifier
            )
        }
        Text(event.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            // IGDB's own time zone label, next to the span rendered in the device's zone.
            text = listOfNotNull(
                event.dateTimeRangeLabel(),
                event.time_zone.takeIf(String::isNotBlank)
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        event.description.takeIf(String::isNotBlank)?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (descriptionExpanded) Int.MAX_VALUE else DescriptionCollapsedLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { descriptionExpanded = !descriptionExpanded }.animateContentSize()
            )
        }
        EventLinks(event, uriHandler::openURL)
    }
}

/** One tappable link of the event: its live stream or one of its networks. */
private data class EventLink(val url: String, val label: String, val icon: ImageVector)

/** The event's live stream and social links, as the app's connected button group. */
@Composable
private fun EventLinks(event: Event, onOpen: (String) -> Unit) {
    val liveStreamLabel = stringResource(Res.string.events__live_stream)
    val links = buildList {
        event.live_stream_url.takeIf(String::isNotBlank)?.let { url ->
            add(EventLink(url, liveStreamLabel, Icons.PlayCircleW500Rounded))
        }
        for (network in event.event_networks) {
            val url = network.url.ifBlank { continue }
            val type = network.network_type?.name
            add(EventLink(url, type ?: url, networkIcon(type)))
        }
    }
    if (links.isEmpty()) return

    ConnectedButtonGroup(
        options = links,
        // Links are actions, not a selection, so no button ever stays checked.
        checked = { false },
        onCheckedChange = { link, _ -> onOpen(link.url) },
        toggleButtonText = { it.label },
        toggleButtonIcon = { it.icon },
        rowModifier = Modifier.fillMaxWidth()
    )
}

/** IGDB only defines four network types; anything new falls back to a generic link glyph. */
private fun networkIcon(networkType: String?): ImageVector = when (networkType) {
    "Twitch" -> SimpleIcons.TwitchSimpleIcons
    "YouTube" -> SimpleIcons.YoutubeSimpleIcons
    "Twitter" -> SimpleIcons.XSimpleIcons
    "Official website" -> Icons.LanguageW500Rounded
    else -> Icons.OpenInNewW500Rounded
}
