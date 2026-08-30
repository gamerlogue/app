package it.maicol07.gamerlogue.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context

class AndroidAuthTokenProvider(context: Context) : AuthTokenProvider() {
    private val accountManager: AccountManager = AccountManager.get(context)
    // Mirrors res/xml/authenticator.xml, whose account type is the applicationId (debug builds carry a .dev suffix).
    private val accountType = context.packageName
    private val authTokenType = "Bearer"
    private val accountName = "Gamerlogue"
    private val userIdKey = "user_id"
    private val expiresAtKey = "expires_at_epoch_millis"

    init {
        restore()
    }

    // getAccountsByType only returns accounts owned by this app, so it needs no GET_ACCOUNTS permission.
    private fun getOrCreateAccount(): Account {
        val existing = accountManager.getAccountsByType(accountType).firstOrNull()
        if (existing != null) return existing

        val account = Account(accountName, accountType)
        // Add account without password (we use token-based auth)
        accountManager.addAccountExplicitly(account, null, null)
        return account
    }

    override fun loadToken(): String? {
        val account = accountManager.getAccountsByType(accountType).firstOrNull() ?: return null
        return accountManager.peekAuthToken(account, authTokenType)
    }

    override fun saveToken(token: String?) {
        if (token == null) {
            // invalidateAuthToken only drops the token from the cache — it means "this token is stale",
            // not "forget it", and no-ops when peekAuthToken returns null. Removing the account is the
            // unambiguous way to end a session; the next login recreates it through getOrCreateAccount.
            accountManager.getAccountsByType(accountType).forEach(accountManager::removeAccountExplicitly)
            return
        }
        accountManager.setAuthToken(getOrCreateAccount(), authTokenType, token)
    }

    override fun loadUserId(): String? {
        val account = accountManager.getAccountsByType(accountType).firstOrNull() ?: return null
        return accountManager.getUserData(account, userIdKey)
    }

    override fun saveUserId(userId: String?) {
        // No getOrCreateAccount on the null path: logout clears the token first, which removes the
        // account, and recreating an empty one here would leave a stray account behind.
        val account = if (userId == null) {
            accountManager.getAccountsByType(accountType).firstOrNull() ?: return
        } else {
            getOrCreateAccount()
        }
        accountManager.setUserData(account, userIdKey, userId)
    }

    override fun loadExpiresAtEpochMillis(): Long? {
        val account = accountManager.getAccountsByType(accountType).firstOrNull() ?: return null
        return accountManager.getUserData(account, expiresAtKey)?.toLongOrNull()
    }

    override fun saveExpiresAtEpochMillis(value: Long?) {
        val account = if (value == null) {
            accountManager.getAccountsByType(accountType).firstOrNull() ?: return
        } else {
            getOrCreateAccount()
        }
        accountManager.setUserData(account, expiresAtKey, value?.toString())
    }
}
