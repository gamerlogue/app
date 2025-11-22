import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.raedghazal.kotlinx_datetime_ext.Locale
import com.raedghazal.kotlinx_datetime_ext.initPlatformLocales
import it.maicol07.gamerlogue.App

@JsModule("date-fns/locale/en")
external object DateFnsLocaleEn

@JsModule("date-fns/locale/it")
external object DateFnsLocaleIt

@OptIn(ExperimentalComposeUiApi::class)
fun main() = ComposeViewport {
    Locale.initPlatformLocales(DateFnsLocaleEn, DateFnsLocaleIt)
    App()
}
