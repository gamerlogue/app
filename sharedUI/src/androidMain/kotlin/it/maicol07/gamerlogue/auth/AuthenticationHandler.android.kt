package it.maicol07.gamerlogue.auth

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import it.maicol07.gamerlogue.BuildConfig

class AndroidAuthenticationHandler(private val context: Context) : AuthenticationHandler {
    override fun login() {
        val redirectUri = "gamerlogue://auth/callback"
        val intent = Intent(
            Intent.ACTION_VIEW,
            "${BuildConfig.GAMERLOGUE_URL}/sanctum/token?token_name=Gamerlogue&redirect_uri=$redirectUri".toUri()
        )
        context.startActivity(intent)
    }
}

@Composable
actual fun rememberAuthenticationHandler(): AuthenticationHandler {
    val context = LocalContext.current
    return remember(context) { AndroidAuthenticationHandler(context) }
}
