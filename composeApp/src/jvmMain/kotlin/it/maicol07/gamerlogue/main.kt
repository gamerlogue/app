package it.maicol07.gamerlogue

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.CompositionLocalProvider
import io.github.kdroidfilter.platformtools.darkmodedetector.windows.setWindowsAdaptiveTitleBar
import it.maicol07.gamerlogue.auth.AuthState
import it.maicol07.gamerlogue.auth.JvmAuthTokenProvider
import it.maicol07.gamerlogue.auth.LocalAuthTokenProvider
import java.awt.Dimension

fun main() {
    val authProvider = JvmAuthTokenProvider()
    AuthState.token = authProvider.getToken()
    application {
        Window(
            title = "Gamerlogue App",
            state = rememberWindowState(width = 800.dp, height = 1200.dp),
            onCloseRequest = ::exitApplication,
        ) {
            window.minimumSize = Dimension(350, 1200)
            window.setWindowsAdaptiveTitleBar()
            CompositionLocalProvider(LocalAuthTokenProvider provides authProvider) {
                App()
            }
        }
    }
}
