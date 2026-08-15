package it.maicol07.gamerlogue

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Outside ComposeViewport: its content lambda is composable and would re-run the init on every recomposition.
    webAppInit()
    ComposeViewport { App() }
}
