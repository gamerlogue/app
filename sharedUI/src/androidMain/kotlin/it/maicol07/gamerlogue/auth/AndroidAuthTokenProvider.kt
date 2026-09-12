package it.maicol07.gamerlogue.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context

class AndroidAuthTokenProvider(context: Context) : AuthTokenProvider() {
    private val accountManager: AccountManager = AccountManager.get(context)

    // Mirrors res/xml/authenticator.xml, whose account type is the applicationId (debug builds carry a .dev suffix).
    private val accountType = context.packageName
    private val accessTokenType = "Bearer"
    private val refreshTokenType = "Refresh"
    private val accountName = "Gamerlogue"
    private val userIdKey = "user_id"
    private val accessExpiresAtKey = "expires_at_epoch_millis"
    private val refreshExpiresAtKey = "refresh_expires_at_epoch_millis"

    init {
        restore()
    }

    // getAccountsByType only returns accounts owned by this app, so it needs no GET_ACCOUNTS permission.
    private val account: Account?
        get() = accountManager.getAccountsByType(accountType).firstOrNull()

    private fun getOrCreateAccount(): Account = account ?: Account(accountName, accountType).also {
        // Add account without password (we use token-based auth)
        accountManager.addAccountExplicitly(it, null, null)
    }

    override fun loadPersistedSession(): PersistedSession {
        val account = account ?: return PersistedSession()
        return PersistedSession(
            accessToken = accountManager.peekAuthToken(account, accessTokenType),
            refreshToken = accountManager.peekAuthToken(account, refreshTokenType),
            userId = accountManager.getUserData(account, userIdKey),
            accessExpiresAtEpochMillis = accountManager.getUserData(account, accessExpiresAtKey)?.toLongOrNull(),
            refreshExpiresAtEpochMillis = accountManager.getUserData(account, refreshExpiresAtKey)?.toLongOrNull(),
        )
    }

    override fun savePersistedSession(persisted: PersistedSession) {
        if (persisted == PersistedSession()) {
            // invalidateAuthToken only drops the token from the cache — it means "this token is stale",
            // not "forget it", and no-ops when peekAuthToken returns null. Removing the account is the
            // unambiguous way to end a session; the next login recreates it through getOrCreateAccount.
            accountManager.getAccountsByType(accountType).forEach(accountManager::removeAccountExplicitly)
            return
        }
        val account = getOrCreateAccount()

        accountManager.setAuthToken(account, refreshTokenType, persisted.refreshToken)
        accountManager.setAuthToken(account, accessTokenType, persisted.accessToken)
        accountManager.setUserData(account, userIdKey, persisted.userId)
        accountManager.setUserData(account, accessExpiresAtKey, persisted.accessExpiresAtEpochMillis?.toString())
        accountManager.setUserData(account, refreshExpiresAtKey, persisted.refreshExpiresAtEpochMillis?.toString())
    }
}
