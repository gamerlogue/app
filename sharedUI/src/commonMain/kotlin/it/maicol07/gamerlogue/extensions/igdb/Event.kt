package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.ui.text.intl.Locale
import at.released.igdbclient.model.Event
import com.raedghazal.kotlinx_datetime_ext.LocalDateTimeFormatter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The event's span as `dd/MM/yyyy - dd/MM/yyyy`, collapsed to a single date for same-day events.
 *
 * IGDB leaves `end_time` unset on a good share of events, so the end is optional.
 */
@OptIn(ExperimentalTime::class)
fun Event.dateRangeLabel(locale: Locale = Locale.current): String {
    val start = start_time?.let { formatDay(it.getEpochSecond(), locale) } ?: return "TBA"
    val end = end_time?.let { formatDay(it.getEpochSecond(), locale) }
    return if (end == null || end == start) start else "$start - $end"
}

/**
 * Like [dateRangeLabel] but with the time of day, in the device's time zone — the header shows the
 * event's own [Event.time_zone] separately.
 */
@OptIn(ExperimentalTime::class)
fun Event.dateTimeRangeLabel(locale: Locale = Locale.current): String {
    val start = start_time?.let { format(it.getEpochSecond(), DateTimePattern, TimeZone.currentSystemDefault(), locale) }
        ?: return "TBA"
    val end = end_time?.let { format(it.getEpochSecond(), DateTimePattern, TimeZone.currentSystemDefault(), locale) }
    return if (end == null) start else "$start - $end"
}

private const val DatePattern = "dd/MM/yyyy"
private const val DateTimePattern = "dd/MM/yyyy HH:mm"

@OptIn(ExperimentalTime::class)
private fun formatDay(epochSeconds: Long, locale: Locale) =
    format(epochSeconds, DatePattern, TimeZone.UTC, locale)

@OptIn(ExperimentalTime::class)
private fun format(epochSeconds: Long, pattern: String, timeZone: TimeZone, locale: Locale): String {
    val formatter = LocalDateTimeFormatter.ofPattern(
        pattern,
        com.raedghazal.kotlinx_datetime_ext.Locale.forLanguageTag(locale.toLanguageTag())
    )
    return formatter.format(Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(timeZone))
}
