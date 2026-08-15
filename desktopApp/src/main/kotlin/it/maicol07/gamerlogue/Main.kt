package it.maicol07.gamerlogue

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.kdroidfilter.platformtools.darkmodedetector.windows.setWindowsAdaptiveTitleBar
import java.awt.Dimension

private const val WindowMinWidth = 350
private const val WindowMinHeight = 200

fun main() = application {
    Window(
        title = "Gamerlogue",
        icon = painterResource("AppIcon.png"),
        state = rememberWindowState(width = 800.dp, height = 900.dp),
        onCloseRequest = ::exitApplication,
    ) {
        // Window is an AWT object, not composable state: size it once instead of on every recomposition.
        LaunchedEffect(window) {
            window.minimumSize = Dimension(WindowMinWidth, WindowMinHeight)
        }
        window.setWindowsAdaptiveTitleBar()
        App()
    }
}
