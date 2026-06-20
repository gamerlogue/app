package it.maicol07.gamerlogue.extensions

import androidx.compose.ui.text.intl.Locale
import java.util.Locale as JavaLocale

actual fun Locale.getDisplayLanguage(inLocale: Locale): String? {
    val target = JavaLocale.forLanguageTag(asLanguageTag())
    val display = JavaLocale.forLanguageTag(inLocale.asLanguageTag())
    val name = target.getDisplayLanguage(display)
    return name.takeIf { it.isNotBlank() }
}
