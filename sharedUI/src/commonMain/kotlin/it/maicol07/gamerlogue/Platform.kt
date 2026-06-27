package it.maicol07.gamerlogue

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry

@Composable
expect fun SystemBarsVisible(visible: Boolean)

@Composable
expect fun appLanguageSettingsOpener(): () -> Unit

expect fun clipEntryFor(string: String): ClipEntry
