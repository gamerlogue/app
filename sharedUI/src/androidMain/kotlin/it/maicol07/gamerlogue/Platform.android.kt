package it.maicol07.gamerlogue

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext
import com.stoyanvuchev.systemuibarstweaker.rememberSystemUIBarsTweaker

@Composable
actual fun SystemBarsVisible(visible: Boolean) {
    val tweaker = rememberSystemUIBarsTweaker()
    LaunchedEffect(visible) {
        tweaker.tweakStatusBarVisibility(visible)
        tweaker.tweakNavigationBarVisibility(visible)
    }
}

@Composable
actual fun NavigationBarContrastEnforced(enforced: Boolean) {
    val window = LocalActivity.current?.window
    LaunchedEffect(window, enforced) {
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = enforced
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun openAppLanguageSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
    intent.data = Uri.fromParts("package", context.packageName, null)
    context.startActivity(intent)
}

@Composable
actual fun appLanguageSettingsOpener(): () -> Unit {
    val context = LocalContext.current
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openAppLanguageSettings(context)
        }
    }
}

actual fun clipEntryFor(string: String) = ClipEntry(ClipData.newPlainText("Copied Text", string))
