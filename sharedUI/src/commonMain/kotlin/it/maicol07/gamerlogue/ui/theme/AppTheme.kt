package it.maicol07.gamerlogue.ui.theme

import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__theme_dark
import gamerlogue.sharedui.generated.resources.settings__theme_light
import gamerlogue.sharedui.generated.resources.settings__theme_system
import org.jetbrains.compose.resources.StringResource

/**
 * @param isDark the stored `IS_DARK_THEME` value this mode maps to; null means the preference is
 *   absent and the system setting decides.
 */
enum class AppTheme(val label: StringResource, val isDark: Boolean?) {
    SYSTEM(Res.string.settings__theme_system, null),
    LIGHT(Res.string.settings__theme_light, false),
    DARK(Res.string.settings__theme_dark, true);

    companion object {
        fun of(isDark: Boolean?) = entries.first { it.isDark == isDark }
    }
}
