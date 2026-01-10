package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@OptIn(ExperimentalMaterial3Api::class)
class TopBarState {
    var visible by mutableStateOf(true)
    var customTitle by mutableStateOf<(@Composable () -> Unit)?>(null)
    var customColors by mutableStateOf<TopAppBarColors?>(null)
}

val LocalTopBarState = staticCompositionLocalOf<TopBarState> {
    error("LocalTopBarState not provided")
}

@Composable
fun rememberTopBarState(): TopBarState = remember { TopBarState() }

