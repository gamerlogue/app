package it.maicol07.gamerlogue

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val AuthCallbackHost = "gamerlogue.maicol07.it"
private const val AuthCallbackPath = "/auth/callback"

class AppActivity : ComponentActivity() {
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
        val isAppLink = data?.scheme == "https" && data.host == AuthCallbackHost && data.path == AuthCallbackPath
        val isLegacyLink = data?.scheme == "gamerlogue" && data.host == "auth" && data.path == AuthCallbackPath
        if (isAppLink || isLegacyLink) {
            authCallbackUri = data.toString()
        }
    }
}
