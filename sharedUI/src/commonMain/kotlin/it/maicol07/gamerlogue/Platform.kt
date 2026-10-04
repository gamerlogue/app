package it.maicol07.gamerlogue

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry

@Composable
expect fun SystemBarsVisible(visible: Boolean)

/** Applies the system scrim drawn behind a transparent navigation bar. No-op where the platform has no such bar. */
@Composable
expect fun NavigationBarContrastEnforced(enforced: Boolean)

@Composable
expect fun appLanguageSettingsOpener(): () -> Unit

expect fun clipEntryFor(string: String): ClipEntry

/**
 * The seed color derived from the system palette (Android 12+ wallpaper colors), or null where the
 * platform has none — the app then falls back to its own brand seed.
 */
@Composable
expect fun deviceSeedColor(): Color?

/**
 * Applies the in-app language override; null restores the language the system picked.
 *
 * Compose resources resolve strings against `Locale.current`, which is platform state rather than a
 * composition local, so the override has to be pushed down to the platform. No-op on web, where the
 * browser owns the language.
 *
 * @param tag an IETF BCP 47 language tag, e.g. `it`.
 */
expect fun applyAppLanguage(tag: String?)

/** Whether the app runs on an Android emulator, which reaches the host machine through `10.0.2.2`. */
expect val isAndroidEmulator: Boolean
