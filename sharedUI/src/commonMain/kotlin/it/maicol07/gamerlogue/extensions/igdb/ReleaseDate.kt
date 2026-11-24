package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.ui.text.intl.Locale
import at.released.igdbclient.model.ReleaseDate
import com.raedghazal.kotlinx_datetime_ext.LocalDateTimeFormatter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun ReleaseDate.displayDate(locale: Locale = Locale.current): String =
    date?.let { releaseDayLabel(it.getEpochSecond(), locale) } ?: "TBA"

/** An IGDB release day (a UTC midnight, in epoch seconds) as `dd/MM/yyyy`. */
fun releaseDayLabel(epochSeconds: Long, locale: Locale = Locale.current): String {
    val formatter = LocalDateTimeFormatter.ofPattern(
        "dd/MM/yyyy",
        com.raedghazal.kotlinx_datetime_ext.Locale.forLanguageTag(locale.toLanguageTag())
    )
    return formatter.format(Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.UTC))
}
