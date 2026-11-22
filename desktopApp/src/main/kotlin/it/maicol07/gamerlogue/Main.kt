package it.maicol07.gamerlogue

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import gamerlogue.desktopapp.generated.resources.Res
import gamerlogue.desktopapp.generated.resources.app_icon
import io.github.kdroidfilter.platformtools.darkmodedetector.windows.setWindowsAdaptiveTitleBar
import it.maicol07.gamerlogue.services.BackgroundSyncWindow
import it.maicol07.gamerlogue.services.SyncTray
import it.maicol07.gamerlogue.services.isTraySupported
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension

private const val WindowMinWidth = 350
private const val WindowMinHeight = 200

fun main() = application {
    var visible by remember { mutableStateOf(true) }
    val icon = painterResource(Res.drawable.app_icon)
    Window(
        title = "Gamerlogue",
        icon = icon,
        state = rememberWindowState(width = 800.dp, height = 900.dp),
        visible = visible,
        // Closing hides it in the tray, where the background syncs go on; Quit there ends the app.
        onCloseRequest = { if (isTraySupported) visible = false else exitApplication() },
    ) {
        // Window is an AWT object, not composable state: size it once instead of on every recomposition.
        LaunchedEffect(window) {
            window.minimumSize = Dimension(WindowMinWidth, WindowMinHeight)
        }
        window.setWindowsAdaptiveTitleBar()
        App()
    }

    SyncTray(icon, onOpen = { visible = true }, onQuit = ::exitApplication)
    BackgroundSyncWindow()
}
