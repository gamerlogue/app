package it.maicol07.gamerlogue.auth

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.edit
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import it.maicol07.gamerlogue.BuildConfig

class AndroidAuthenticationHandler(
    private val context: Context,
    authProvider: AuthTokenProvider,
    authClient: HttpClient,
) : NativeAuthenticationHandler(authProvider, authClient) {
    private val flowState = context.getSharedPreferences("native_auth_flow", Context.MODE_PRIVATE)

    override fun launchLogin(attempt: PkceLoginAttempt) {
        savePendingLogin(attempt.pending)
        // The App Link is bound to the host hardcoded in the manifest, so any other backend — a local
        // one included — can only come back through the private-use scheme filter, which matches
        // regardless of host. See docs/auth-app-links.md.
        val redirectUri = if (BuildConfig.GAMERLOGUE_URL == OFFICIAL_INSTANCE_URL) {
            "$OFFICIAL_INSTANCE_URL/auth/callback"
        } else {
            "gamerlogue://auth/callback"
        }
        // A Custom Tab keeps the flow in a browser the user can inspect (URL bar, real cert state) and
        // shares the browser's cookie jar, instead of handing the auth URL to whatever app happens to
        // claim ACTION_VIEW. It falls back to the default browser when no provider supports it.
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .apply { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            .launchUrl(context, buildAuthUrl(redirectUri, attempt).toUri())
    }

    override suspend fun handleCallback(query: String): Boolean {
        val pending = loadPendingLogin() ?: return false
        return if (callbackMatchesState(query, pending.state)) {
            clearPendingLogin()
            exchangeCallback(query, pending)
        } else {
            false
        }
    }

    private fun loadPendingLogin(): PendingLogin? {
        val verifier = flowState.getString("verifier", null)
        val state = flowState.getString("state", null)
        return if (verifier == null || state == null) null else PendingLogin(verifier, state)
    }

    private fun savePendingLogin(pending: PendingLogin) {
        flowState.edit(commit = true) {
            putString("verifier", pending.verifier)
            putString("state", pending.state)
        }
    }

    private fun clearPendingLogin() {
        flowState.edit(commit = true) { clear() }
    }
}

/** Kept in sync with the `autoVerify` intent-filter host in the Android manifest. */
private const val OFFICIAL_INSTANCE_URL = "https://gamerlogue.maicol07.it"
