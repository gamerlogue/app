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

class AppActivity : ComponentActivity() {
    // Observed by setContent: updated by onCreate/onNewIntent so the callback reaches App().
    private var authCallbackUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        captureLoginDeepLink(intent)
        setContent {
            App(authCallbackUri = authCallbackUri)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        captureLoginDeepLink(intent)
    }

    private fun captureLoginDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null && data.toString().startsWith("gamerlogue://auth/callback")) {
            authCallbackUri = data.toString()
        }
    }
}
