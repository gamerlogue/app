package it.maicol07.gamerlogue

import androidx.compose.runtime.Composable
import com.raedghazal.kotlinx_datetime_ext.Locale
import com.raedghazal.kotlinx_datetime_ext.initPlatformLocales

@Composable
actual fun SystemBarsVisible(visible: Boolean) {
    // No-op on web
}

@JsModule("date-fns/locale/en-US")
external object DateFnsLocaleEn

@JsModule("date-fns/locale/it")
external object DateFnsLocaleIt

fun WebAppInit() {
    Locale.initPlatformLocales(DateFnsLocaleEn, DateFnsLocaleIt)
}
