package it.maicol07.gamerlogue.auth

import co.touchlab.kermit.Logger
import com.github.javakeyring.BackendNotSupportedException
import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import java.util.prefs.Preferences

private const val KeyringDomain = "it.maicol07.gamerlogue"
private const val TokenAccount = "auth_token"
private const val LegacyTokenKey = "auth_token"
private const val UserIdKey = "auth_user_id"
private const val ExpiresAtKey = "auth_expires_at_epoch_millis"

/**
 * Desktop session storage.
 *
 * The bearer token goes to the OS credential store (Windows Credential Manager, macOS Keychain, Linux
 * Secret Service) rather than to [Preferences], which is a plaintext file or registry key readable by
 * any process running as the user. The user id stays in [Preferences]: it is not a secret, and keeping
 * it there avoids a second keyring round-trip on every start.
 *
 * If the platform has no usable credential store the token is kept in memory only — the failure is
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
        val legacy = prefs.get(LegacyTokenKey, null) ?: return
        if (keyring == null) return
        try {
            keyring.setPassword(KeyringDomain, TokenAccount, legacy)
            prefs.remove(LegacyTokenKey)
            Logger.i { "Migrated the desktop session token to the OS credential store" }
        } catch (e: PasswordAccessException) {
            Logger.e(e) { "Could not migrate the session token to the OS credential store" }
        }
    }

    override fun loadToken(): String? {
        val keyring = keyring ?: return null
        return try {
            keyring.getPassword(KeyringDomain, TokenAccount)
        } catch (e: PasswordAccessException) {
            // Thrown when the entry is simply absent, which is the normal signed-out case.
            Logger.v(e) { "No session token in the OS credential store" }
            null
        }
    }

    override fun saveToken(token: String?) {
        val keyring = keyring ?: return
        try {
            if (token == null) keyring.deletePassword(KeyringDomain, TokenAccount)
            else keyring.setPassword(KeyringDomain, TokenAccount, token)
        } catch (e: PasswordAccessException) {
            Logger.e(e) { "Could not ${if (token == null) "clear" else "store"} the session token" }
        }
    }

    override fun loadUserId(): String? = prefs.get(UserIdKey, null)

    override fun saveUserId(userId: String?) {
        if (userId == null) prefs.remove(UserIdKey) else prefs.put(UserIdKey, userId)
    }

    override fun loadExpiresAtEpochMillis(): Long? = prefs.getLong(ExpiresAtKey, 0).takeIf { it > 0 }

    override fun saveExpiresAtEpochMillis(value: Long?) {
        if (value == null) prefs.remove(ExpiresAtKey) else prefs.putLong(ExpiresAtKey, value)
    }
}
