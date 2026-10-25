package it.maicol07.gamerlogue

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.InetAddresses
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.stoyanvuchev.systemuibarstweaker.rememberSystemUIBarsTweaker
import java.net.Inet6Address

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

// Handles both sides of API 33: above it AppCompat forwards to the system per-app language, below it
// applies and persists the override itself.
actual fun applyAppLanguage(tag: String?) {
    AppCompatDelegate.setApplicationLocales(
        if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
    )
}

@Composable
actual fun deviceSeedColor(): Color? {
    val context = LocalContext.current
    return remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context).primary
        } else {
            null
        }
    }
}

// ponytail: covers the official emulator images (ranchu, and goldfish on old ones), not third-party emulators.
actual val isAndroidEmulator = Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish"

// From API 37 Local Network Protections block LAN destinations (RFC 1918, link-local, ULA, mDNS) for apps that do
// not hold ACCESS_LOCAL_NETWORK, so a local backend times out without it. Only debug builds declare the permission.
@Composable
actual fun LocalNetworkAccess(serverUrl: String) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(serverUrl) {
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable || Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return@LaunchedEffect
        if (!isLocalNetworkHost(serverUrl)) return@LaunchedEffect
        val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(permission)
        }
    }
}

// IPv6 unique local addresses, fc00::/7: InetAddress.isSiteLocalAddress only knows the deprecated fec0::/10.
private const val UniqueLocalMask = 0xFE
private const val UniqueLocalPrefix = 0xFC

// ponytail: hostnames are not resolved (no DNS on the main thread), so a LAN host reached by a plain DNS name is
// missed; only IP literals and mDNS `.local` names count.
@RequiresApi(Build.VERSION_CODES.Q)
private fun isLocalNetworkHost(url: String): Boolean {
    val host = Uri.parse(url).host?.removeSurrounding("[", "]").orEmpty()
    val address = host.takeIf(InetAddresses::isNumericAddress)?.let(InetAddresses::parseNumericAddress)
    val uniqueLocal = address is Inet6Address && (address.address[0].toInt() and UniqueLocalMask) == UniqueLocalPrefix
    val privateAddress = address != null && (address.isSiteLocalAddress || address.isLinkLocalAddress || uniqueLocal)
    return privateAddress || host.endsWith(".local")
}
