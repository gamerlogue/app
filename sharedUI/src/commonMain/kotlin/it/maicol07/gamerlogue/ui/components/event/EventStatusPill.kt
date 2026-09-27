package it.maicol07.gamerlogue.ui.components.event

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Event
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.events__live_now
import gamerlogue.sharedui.generated.resources.home__release_in_days
import gamerlogue.sharedui.generated.resources.home__release_today
import gamerlogue.sharedui.generated.resources.home__release_tomorrow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/** Beyond this the countdown is dropped: the event's date range says enough. */
private const val CountdownMaxDays = 30

/** "Live now" while the event runs, a countdown in its last month before; nothing otherwise. */
@Composable
fun EventStatusPill(event: Event) {
    val start = event.start_time?.getEpochSecond() ?: return
    val end = event.end_time?.getEpochSecond() ?: start
    val now = Clock.System.now()
    if (now.epochSeconds in start..end) {
        StatusPill(
            stringResource(Res.string.events__live_now),
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary
        )
        return
    }
    if (start < now.epochSeconds) return

    val timeZone = TimeZone.currentSystemDefault()
    val days = now.toLocalDateTime(timeZone).date
        .daysUntil(Instant.fromEpochSeconds(start).toLocalDateTime(timeZone).date)
    val label = when (days) {
        0 -> stringResource(Res.string.home__release_today)
        1 -> stringResource(Res.string.home__release_tomorrow)
        in 2..CountdownMaxDays -> pluralStringResource(Res.plurals.home__release_in_days, days, days)
        else -> return
    }
    StatusPill(label, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
}

@Composable
private fun StatusPill(text: String, containerColor: Color, contentColor: Color) =
    Surface(color = containerColor, contentColor = contentColor, shape = CircleShape) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
