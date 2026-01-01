package it.maicol07.gamerlogue

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import it.maicol07.gamerlogue.auth.AndroidAuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthState
import it.maicol07.gamerlogue.auth.LocalAuthTokenProvider

class AppActivity : ComponentActivity() {
    private val authProvider by lazy { AndroidAuthTokenProvider(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set saved token
        AuthState.token = authProvider.getToken()

        handleLoginDeepLink(intent)
        setContent {
            CompositionLocalProvider(LocalAuthTokenProvider provides authProvider) {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLoginDeepLink(intent)
    }

    private fun handleLoginDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null && data.toString().startsWith("gamerlogue://auth/callback")) {
            val token = data.getQueryParameter("token")
            if (!token.isNullOrBlank()) {
                authProvider.setToken(token)
            }
            val userId = data.getQueryParameter("user_id")
            if (!userId.isNullOrBlank()) {
                AuthState.userId = userId
            }
        }
    }
}
