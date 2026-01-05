package it.maicol07.gamerlogue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.stoyanvuchev.systemuibarstweaker.rememberSystemUIBarsTweaker

@Composable
actual fun SystemBarsVisible(visible: Boolean) {
    val tweaker = rememberSystemUIBarsTweaker()
    LaunchedEffect(visible) {
        tweaker.tweakStatusBarVisibility(visible)
        tweaker.tweakNavigationBarVisibility(visible)
    }
}
