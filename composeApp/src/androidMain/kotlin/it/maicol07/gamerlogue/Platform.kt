package it.maicol07.gamerlogue

import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.stoyanvuchev.systemuibarstweaker.rememberSystemUIBarsTweaker

@Composable
actual fun SystemBarsVisible(visible: Boolean) {
    val tweaker = rememberSystemUIBarsTweaker()
    LaunchedEffect(visible) {
        tweaker.tweakStatusBarVisibility(visible)
        tweaker.tweakNavigationBarVisibility(visible)
    }
}
