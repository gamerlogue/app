package it.maicol07.gamerlogue

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection
import java.util.Locale

@Suppress("EmptyMethod")
@Composable
actual fun SystemBarsVisible(visible: Boolean) {
    // No-op on JVM
}

@Suppress("EmptyMethod")
@Composable
actual fun NavigationBarContrastEnforced(enforced: Boolean) {
    // No-op on JVM
}

@Composable
actual fun appLanguageSettingsOpener(): () -> Unit = {}

@OptIn(ExperimentalComposeUiApi::class)
actual fun clipEntryFor(string: String) = ClipEntry(StringSelection(string))

@Suppress("SameReturnValue")
@Composable
actual fun deviceSeedColor(): Color? = null // No system palette on JVM

/** Captured before any override, so passing null can restore what the OS reported. */
private val systemLocale: Locale = Locale.getDefault()

actual fun applyAppLanguage(tag: String?) {
    Locale.setDefault(if (tag == null) systemLocale else Locale.forLanguageTag(tag))
}
