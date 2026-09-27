package it.maicol07.gamerlogue.auth

import co.touchlab.kermit.Logger
import com.github.javakeyring.BackendNotSupportedException
import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import java.util.prefs.Preferences

private const val KEYRING_DOMAIN = "it.maicol07.gamerlogue"
private const val ACCESS_TOKEN_ACCOUNT = "auth_token"
private const val REFRESH_TOKEN_ACCOUNT = "auth_refresh_token"
private const val LEGACY_TOKEN_KEY = "auth_token"
private const val USER_ID_KEY = "auth_user_id"
private const val ACCESS_EXPIRES_AT_KEY = "auth_expires_at_epoch_millis"
private const val REFRESH_EXPIRES_AT_KEY = "auth_refresh_expires_at_epoch_millis"

/**
 * Desktop session storage.
 *
 * The access and refresh tokens go to the OS credential store (Windows Credential Manager, macOS Keychain, Linux
 * Secret Service) rather than to [Preferences], which is a plaintext file or registry key readable by
 * any process running as the user. The user id stays in [Preferences]: it is not a secret, and keeping
 * it there avoids a second keyring round-trip on every start.
 *
 * If the platform has no usable credential store, the token is kept in memory only — the failure is
 * logged and the session simply does not survive a restart. It is deliberately **not** written back to
 * [Preferences] as a fallback: silently downgrading to plaintext would defeat the point.
 */
class JvmAuthTokenProvider : AuthTokenProvider() {
    private val prefs: Preferences = Preferences.userNodeForPackage(JvmAuthTokenProvider::class.java)

    /** Null when the platform has no usable credential store; every access degrades to in-memory. */
    private val keyring: Keyring? = try {
        Keyring.create()
    } catch (e: BackendNotSupportedException) {
        Logger.e(e) { "No OS credential store available; the session will not survive a restart" }
        null
    }

    init {
        migrateLegacyToken()
        restore()
    }

    /**
     * Moves a token written by the previous `Preferences`-based implementation into the keyring and
     * deletes the plaintext copy. Without this, upgrading users would be signed out *and* leave the old
     * token behind in the clear.
     */
    private fun migrateLegacyToken() {
        val legacy = prefs.get(LEGACY_TOKEN_KEY, null) ?: return
        if (keyring == null) return
        try {
            keyring.setPassword(KEYRING_DOMAIN, ACCESS_TOKEN_ACCOUNT, legacy)
            prefs.remove(LEGACY_TOKEN_KEY)
            Logger.i { "Migrated the desktop session token to the OS credential store" }
        } catch (e: PasswordAccessException) {
            Logger.e(e) { "Could not migrate the session token to the OS credential store" }
        }
    }

    override fun loadPersistedSession() = PersistedSession(
        accessToken = loadToken(ACCESS_TOKEN_ACCOUNT),
        refreshToken = loadToken(REFRESH_TOKEN_ACCOUNT),
        userId = prefs.get(USER_ID_KEY, null),
        accessExpiresAtEpochMillis = prefs.getLong(ACCESS_EXPIRES_AT_KEY, 0).takeIf { it > 0 },
        refreshExpiresAtEpochMillis = prefs.getLong(REFRESH_EXPIRES_AT_KEY, 0).takeIf { it > 0 },
    )

    override fun savePersistedSession(persisted: PersistedSession) {
        saveToken(REFRESH_TOKEN_ACCOUNT, persisted.refreshToken)
        saveToken(ACCESS_TOKEN_ACCOUNT, persisted.accessToken)
        prefs.putOrRemove(USER_ID_KEY, persisted.userId)
        prefs.putLongOrRemove(ACCESS_EXPIRES_AT_KEY, persisted.accessExpiresAtEpochMillis)
        prefs.putLongOrRemove(REFRESH_EXPIRES_AT_KEY, persisted.refreshExpiresAtEpochMillis)
    }

    private fun loadToken(account: String): String? {
        val keyring = keyring ?: return null
        return try {
            keyring.getPassword(KEYRING_DOMAIN, account)
        } catch (e: PasswordAccessException) {
            // Thrown when the entry is simply absent, which is the normal signed-out case.
            Logger.v(e) { "No session token in the OS credential store" }
            null
        }
    }

    private fun saveToken(account: String, token: String?) {
        val keyring = keyring ?: return
        try {
            if (token == null) {
                keyring.deletePassword(KEYRING_DOMAIN, account)
            } else {
                keyring.setPassword(KEYRING_DOMAIN, account, token)
            }
        } catch (e: PasswordAccessException) {
            Logger.e(e) { "Could not ${if (token == null) "clear" else "store"} the session token" }
        }
    }
}

@Suppress("SameParameterValue")
private fun Preferences.putOrRemove(key: String, value: String?) =
    if (value == null) remove(key) else put(key, value)

private fun Preferences.putLongOrRemove(key: String, value: Long?) =
    if (value == null) remove(key) else putLong(key, value)
