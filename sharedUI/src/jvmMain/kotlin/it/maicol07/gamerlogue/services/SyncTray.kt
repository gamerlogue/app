package it.maicol07.gamerlogue.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.sync__tray_open
import gamerlogue.sharedui.generated.resources.sync__tray_quit
import gamerlogue.sharedui.generated.resources.sync__tray_syncing
import gamerlogue.sharedui.generated.resources.sync__tray_tooltip
import it.maicol07.gamerlogue.ui.components.layout.syncingLabel
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import java.awt.SystemTray

/** Whether the desktop has a tray to keep the app in (some Linux desktops don't). */
val isTraySupported: Boolean = SystemTray.isSupported()

/** The tray icon that keeps the app, and its background syncs, running with the window closed. */
@Composable
fun ApplicationScope.SyncTray(icon: Painter, onOpen: () -> Unit, onQuit: () -> Unit) {
    if (!isTraySupported) return
    val run by DesktopBackgroundWebHost.request.collectAsState()
    // Koin starts with the main window's content, outside this composition; a background run means it did.
    val coordinator = remember(run != null) {
        if (run != null) KoinPlatform.getKoinOrNull()?.get<SyncCoordinator>() else null
    }
    val queue = coordinator?.queue?.collectAsState()?.value
    val tooltip = when {
        queue?.current != null -> stringResource(Res.string.sync__tray_tooltip, syncingLabel(emptySet(), queue))
        run != null -> stringResource(Res.string.sync__tray_syncing)
        else -> "Gamerlogue"
    }
    Tray(
        icon = icon,
        tooltip = tooltip,
        onAction = onOpen,
        menu = {
            Item(stringResource(Res.string.sync__tray_open), onClick = onOpen)
            Item(stringResource(Res.string.sync__tray_quit), onClick = onQuit)
        },
    )
}
