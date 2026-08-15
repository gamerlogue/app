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
        val account = getOrCreateAccount()
        if (token != null) {
            accountManager.setAuthToken(account, authTokenType, token)
        } else {
            // Clear the auth token
            accountManager.invalidateAuthToken(accountType, accountManager.peekAuthToken(account, authTokenType))
        }
    }

    override fun loadUserId(): String? {
        val account = accountManager.getAccountsByType(accountType).firstOrNull() ?: return null
        return accountManager.getUserData(account, userIdKey)
    }

    override fun saveUserId(userId: String?) {
        val account = getOrCreateAccount()
        accountManager.setUserData(account, userIdKey, userId)
    }
}
