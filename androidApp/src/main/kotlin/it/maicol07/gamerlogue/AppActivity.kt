package it.maicol07.gamerlogue

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import co.touchlab.kermit.Logger
import it.maicol07.gamerlogue.auth.AndroidAuthTokenProvider
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleLoginDeepLink(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLoginDeepLink(intent)
    }

    private fun handleLoginDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null && data.toString().startsWith("gamerlogue://auth/callback")) {
            val authProvider = AndroidAuthTokenProvider(applicationContext)
            val authHandler = it.maicol07.gamerlogue.auth.AndroidAuthenticationHandler(applicationContext, authProvider)
            authHandler.handleCallback(data.toString()) {
                try {
                    URLDecoder.decode(it, "UTF-8")
                } catch (e: UnsupportedEncodingException) {
                    Logger.e(e) { "Error decoding callback query parameter" }
                    null
                }
            }
        }
    }
}
