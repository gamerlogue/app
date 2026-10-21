package it.maicol07.gamerlogue.extensions

import androidx.compose.ui.text.intl.Locale
import com.raedghazal.kotlinx_datetime_ext.LocalDateTimeFormatter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * A backend timestamp (`2026-10-11T12:00:00+00:00`) as a local `dd/MM/yyyy` date, the format the
 * release dates use; null when [iso] is null or unparsable.
 */
fun displayTimestamp(iso: String?, locale: Locale = Locale.current): String? {
    val instant = iso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    val formatter = LocalDateTimeFormatter.ofPattern(
        "dd/MM/yyyy",
        com.raedghazal.kotlinx_datetime_ext.Locale.forLanguageTag(locale.toLanguageTag())
    )
    return formatter.format(instant.toLocalDateTime(TimeZone.currentSystemDefault()))
}
