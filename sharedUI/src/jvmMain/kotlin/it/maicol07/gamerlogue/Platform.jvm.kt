package it.maicol07.gamerlogue

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

@Composable
actual fun SystemBarsVisible(visible: Boolean) {
    // No-op on JVM
}

@Composable
actual fun appLanguageSettingsOpener(): () -> Unit = {}

@OptIn(ExperimentalComposeUiApi::class)
actual fun clipEntryFor(string: String) = ClipEntry(StringSelection(string))
