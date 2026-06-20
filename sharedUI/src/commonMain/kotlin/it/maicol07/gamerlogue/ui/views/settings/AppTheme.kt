package it.maicol07.gamerlogue.ui.views.settings

import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__theme_dark
import gamerlogue.sharedui.generated.resources.settings__theme_light
import gamerlogue.sharedui.generated.resources.settings__theme_system
import org.jetbrains.compose.resources.StringResource

enum class AppTheme(val text: StringResource) {
    SYSTEM(Res.string.settings__theme_system),
    LIGHT(Res.string.settings__theme_light),
    DARK(Res.string.settings__theme_dark)
}
