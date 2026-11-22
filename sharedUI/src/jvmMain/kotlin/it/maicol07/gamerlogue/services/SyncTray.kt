package it.maicol07.gamerlogue.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.sync__tray_open
import gamerlogue.sharedui.generated.resources.sync__tray_quit
import gamerlogue.sharedui.generated.resources.sync__tray_syncing
import org.jetbrains.compose.resources.stringResource
import java.awt.SystemTray

/** Whether the desktop has a tray to keep the app in (some Linux desktops don't). */
val isTraySupported: Boolean = SystemTray.isSupported()

/** The tray icon that keeps the app, and its background syncs, running with the window closed. */
@Composable
fun ApplicationScope.SyncTray(icon: Painter, onOpen: () -> Unit, onQuit: () -> Unit) {
    if (!isTraySupported) return
    val run by DesktopBackgroundWebHost.request.collectAsState()
    Tray(
        icon = icon,
        tooltip = if (run != null) stringResource(Res.string.sync__tray_syncing) else "Gamerlogue",
        onAction = onOpen,
        menu = {
            Item(stringResource(Res.string.sync__tray_open), onClick = onOpen)
            Item(stringResource(Res.string.sync__tray_quit), onClick = onQuit)
        },
    )
}
