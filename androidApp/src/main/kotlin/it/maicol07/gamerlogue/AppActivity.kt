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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        captureLoginDeepLink(intent)
        setContent {
            App(
                authCallbackUri = authCallbackUri,
                // A login callback is a one-shot event. Without clearing it, recreating the composition
                // replays the same URI through AuthHandler and re-applies the same token.
                onAuthCallbackHandled = { authCallbackUri = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        captureLoginDeepLink(intent)
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
