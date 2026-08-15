package it.maicol07.gamerlogue

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry

@Composable
expect fun SystemBarsVisible(visible: Boolean)

/** Applies the system scrim drawn behind a transparent navigation bar. No-op where the platform has no such bar. */
@Composable
expect fun NavigationBarContrastEnforced(enforced: Boolean)

@Composable
expect fun appLanguageSettingsOpener(): () -> Unit

expect fun clipEntryFor(string: String): ClipEntry
