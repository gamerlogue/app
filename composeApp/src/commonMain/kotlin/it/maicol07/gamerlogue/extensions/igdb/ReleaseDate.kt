package it.maicol07.gamerlogue.extensions.igdb

import androidx.compose.ui.text.intl.Locale
import at.released.igdbclient.model.ReleaseDate
import com.raedghazal.kotlinx_datetime_ext.LocalDateTimeFormatter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun ReleaseDate.displayDate(locale: Locale = Locale.current) = date?.let {
    val formatter = LocalDateTimeFormatter.ofPattern(
        "dd/MM/yyyy",
        com.raedghazal.kotlinx_datetime_ext.Locale.forLanguageTag(locale.toLanguageTag())
    )
    formatter.format(Instant.fromEpochSeconds(it.getEpochSecond()).toLocalDateTime(TimeZone.UTC))
} ?: "TBA"
