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

fun Locale.getFlag(flagsList: List<ImageVector> = FlagIcons.LargeFlagList): ImageVector? {
    val targetCode = if (region.isNotBlank()) {
        region.lowercase()
    } else {
        when (language.lowercase()) {
            "en" -> "us"
            "ja" -> "jp"
            "ko" -> "kr"
            "ar" -> "sa"
            "zh" -> "cn"
            "cs" -> "cz"
            "da" -> "dk"
            "el" -> "gr"
            "he" -> "il"
            "hi" -> "in"
            "sv" -> "se"
            "uk" -> "ua"
            "vi" -> "vn"
            else -> language.lowercase()
        }
    }
    return flagsList.find {
        val flagCode = it.name.split(":").getOrNull(1) ?: return@find false
        flagCode.equals(targetCode, ignoreCase = true)
    }
}
