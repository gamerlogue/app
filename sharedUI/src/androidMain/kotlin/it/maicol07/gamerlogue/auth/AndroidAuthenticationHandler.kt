package it.maicol07.gamerlogue.auth

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import it.maicol07.gamerlogue.BuildConfig
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import java.security.MessageDigest
import java.security.SecureRandom

class AndroidAuthenticationHandler(
    private val context: Context,
    authProvider: AuthTokenProvider,
    authClient: HttpClient,
) : AuthenticationHandler(authProvider, authClient) {
    private val flowState = context.getSharedPreferences("native_auth_flow", Context.MODE_PRIVATE)

    private var pendingProof: LoginProof?
        get() {
            val verifier = flowState.getString("verifier", null) ?: return null
            val state = flowState.getString("state", null) ?: return null
            return LoginProof(verifier, challenge = "", state)
        }
        set(value) {
            val saved = flowState.edit().apply {
                if (value == null) {
                    remove("verifier")
                    remove("state")
                } else {
                    putString("verifier", value.verifier)
                    putString("state", value.state)
                }
            }.commit()
            check(saved) { "Could not persist the pending authentication flow" }
        }

    override fun login() {
        val proof = generateLoginProof()
        pendingProof = proof
        val redirectUri = "${BuildConfig.GAMERLOGUE_URL}/auth/callback"
        // A Custom Tab keeps the flow in a browser the user can inspect (URL bar, real cert state) and
        // shares the browser's cookie jar, instead of handing the auth URL to whatever app happens to
        // claim ACTION_VIEW. It falls back to the default browser when no provider supports it.
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .apply { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            .launchUrl(context, getAuthUrl(redirectUri, proof).toUri())
    }

    override suspend fun handleCallback(query: String): Boolean {
        val proof = pendingProof ?: return false
        if (!callbackMatchesState(query, proof.state)) return false
        pendingProof = null
        return exchangeCallback(query, proof)
    }
}

private fun generateLoginProof(): LoginProof {
    val random = SecureRandom()
    return createLoginProof(
        verifierEntropy = ByteArray(32).also(random::nextBytes),
        stateEntropy = ByteArray(32).also(random::nextBytes),
        sha256 = { MessageDigest.getInstance("SHA-256").digest(it) },
    )
}

@Composable
actual fun rememberAuthenticationHandler(): AuthenticationHandler {
    val context = LocalContext.current
    val authProvider = koinInject<AuthTokenProvider>()
    val authClient = koinInject<HttpClient>(qualifier = named("AuthHttpClient"))
    return remember(context, authProvider, authClient) {
        AndroidAuthenticationHandler(context, authProvider, authClient)
    }
}
