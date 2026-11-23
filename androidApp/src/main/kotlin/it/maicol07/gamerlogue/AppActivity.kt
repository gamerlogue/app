package it.maicol07.gamerlogue

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.PayloadData
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import it.maicol07.gamerlogue.core.notificationTarget
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree

// One pair per intent-filter in the manifest: the private-use scheme carries the callback in
// host + path (`gamerlogue://auth/callback`), so it does not share the App Link's path.
private const val AppLinkHost = "gamerlogue.maicol07.it"
private const val AppLinkPath = "/auth/callback"
private const val LegacyCallbackHost = "auth"
private const val LegacyCallbackPath = "/callback"

// AppCompatActivity, not ComponentActivity: AppCompatDelegate applies the per-app language below
// API 33, where the system has no app language setting of its own.
class AppActivity : AppCompatActivity() {
    // Observed by setContent: updated by onCreate/onNewIntent so the callback reaches App().
    private var authCallbackUri by mutableStateOf<String?>(null)

    // Listened to here, not in App(): the tap that launches the activity is parsed before the first composition.
    private var openDestination by mutableStateOf<NavKey?>(null)
    private val notificationListener = object : KMPNotifier.Listener {
        // A push shown by the system, the app in the background: it carries no destination of the app's.
        override fun onNotificationClicked(data: PayloadData) {
            openDestination = RootNavTree.Notifications
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FileKit.init(this)

        KMPNotifier.addListener(notificationListener)
        // A recreated activity gets the launching intent again: only a fresh start may open the list.
        if (savedInstanceState == null) KMPNotifier.onCreateOrOnNewIntent(intent)
        captureLoginDeepLink(intent)
        if (savedInstanceState == null) captureNotificationTarget(intent)
        setContent {
            App(
                authCallbackUri = authCallbackUri,
                // A login callback is a one-shot event. Without clearing it, recreating the composition
                // replays the same URI through AuthHandler and re-applies the same token.
                onAuthCallbackHandled = { authCallbackUri = null },
                openDestination = openDestination,
                onDestinationOpened = { openDestination = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        KMPNotifier.onCreateOrOnNewIntent(intent)
        captureLoginDeepLink(intent)
        captureNotificationTarget(intent)
    }

    override fun onDestroy() {
        KMPNotifier.removeListener(notificationListener)
        super.onDestroy()
    }

    // Set by the notifications the app shows itself.
    private fun captureNotificationTarget(intent: Intent) {
        intent.notificationTarget()?.let { openDestination = it }
    }

    private fun captureLoginDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        val isAppLink = data?.scheme == "https" && data.host == AppLinkHost && data.path == AppLinkPath
        val isLegacyLink = data?.scheme == "gamerlogue" &&
            data.host == LegacyCallbackHost &&
            data.path == LegacyCallbackPath
        if (isAppLink || isLegacyLink) {
            authCallbackUri = data.toString()
        }
    }
}
