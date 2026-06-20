package it.maicol07.gamerlogue.extensions

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.intl.Locale
import flagpack.icons.FlagIcons
import flagpack.icons.LargeFlagList

expect fun Locale.getDisplayLanguage(inLocale: Locale): String?

// Common helper to build a simpler language tag (language[-REGION])
internal fun Locale.asLanguageTag(): String = buildString {
    append(language)
    region.takeIf { it.isNotBlank() }?.let {
        append("-")
        append(it)
    }
}

fun Locale.getFlag(flagsList: List<ImageVector> = FlagIcons.LargeFlagList): ImageVector? = flagsList.find {
    val localeLanguage = when (this.language) {
        "en" -> "us"
        else -> this.language
    }
    it.name.split(":")[1].equals(localeLanguage, ignoreCase = true)
}
