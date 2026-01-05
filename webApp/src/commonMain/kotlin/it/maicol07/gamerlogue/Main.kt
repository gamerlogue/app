package it.maicol07.gamerlogue

import WasmAuthTokenProvider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.raedghazal.kotlinx_datetime_ext.Locale
import com.raedghazal.kotlinx_datetime_ext.initPlatformLocales
import it.maicol07.gamerlogue.auth.LocalAuthTokenProvider

@JsModule("date-fns/locale/en")
external object DateFnsLocaleEn

@JsModule("date-fns/locale/it")
external object DateFnsLocaleIt

@OptIn(ExperimentalComposeUiApi::class)
fun main() = ComposeViewport {
    val authProvider = WasmAuthTokenProvider()
    Locale.initPlatformLocales(DateFnsLocaleEn, DateFnsLocaleIt)
    CompositionLocalProvider(LocalAuthTokenProvider provides authProvider) {
        App()
    }
}
