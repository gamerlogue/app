package it.maicol07.gamerlogue

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
        title = "Gamerlogue App",
        state = rememberWindowState(width = 800.dp, height = 1200.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(WindowMinWidth, WindowMinHeight)
        window.setWindowsAdaptiveTitleBar()
        App()
    }
}
