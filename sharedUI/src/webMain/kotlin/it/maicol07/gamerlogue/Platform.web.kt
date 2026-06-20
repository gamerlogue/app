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

fun webAppInit() {
    Locale.initPlatformLocales(DateFnsLocaleEn, DateFnsLocaleIt)
}

@Composable
actual fun appLanguageSettingsOpener(): () -> Unit = {
    // No-op on web, as the language is determined by the browser settings
}
