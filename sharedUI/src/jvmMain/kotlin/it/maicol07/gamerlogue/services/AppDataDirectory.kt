package it.maicol07.gamerlogue.services

import okio.Path
import okio.Path.Companion.toPath

/**
 * The per-user data directory of the desktop app, by OS convention: `%APPDATA%\Gamerlogue` on Windows,
 * `~/Library/Application Support/Gamerlogue` on macOS, `$XDG_DATA_HOME/gamerlogue` (`~/.local/share`) elsewhere.
 */
internal fun appDataDirectory(): Path {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home").toPath()
    return when {
        os.startsWith("windows") -> (env("APPDATA") ?: (home / "AppData" / "Roaming")) / "Gamerlogue"
        os.startsWith("mac") -> home / "Library" / "Application Support" / "Gamerlogue"
        else -> (env("XDG_DATA_HOME") ?: (home / ".local" / "share")) / "gamerlogue"
    }
}

private fun env(name: String): Path? = System.getenv(name)?.takeIf { it.isNotBlank() }?.toPath()
