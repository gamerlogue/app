package it.maicol07.gamerlogue

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() = ComposeViewport {
    webAppInit()
    App()
}
