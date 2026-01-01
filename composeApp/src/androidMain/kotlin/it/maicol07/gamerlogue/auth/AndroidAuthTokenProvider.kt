package it.maicol07.gamerlogue.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context

class AndroidAuthTokenProvider(context: Context) : AuthTokenProvider {
    private val accountManager: AccountManager = AccountManager.get(context)
    private val accountType = "it.maicol07.gamerlogue"
    private val authTokenType = "Bearer"
    private val accountName = "Gamerlogue"

    private fun getOrCreateAccount(): Account {
        val existing = accountManager.accounts.find { it.type == accountType }
        if (existing != null) return existing

        val account = Account(accountName, accountType)
        // Add account without password (we use token-based auth)
        accountManager.addAccountExplicitly(account, null, null)
        return account
    }

    override fun getToken(): String? {
        val account = accountManager.accounts.find { it.type == accountType } ?: return null
        return accountManager.peekAuthToken(account, authTokenType)
    }

    override fun setToken(token: String?) {
        val account = getOrCreateAccount()
        if (token != null) {
            accountManager.setAuthToken(account, authTokenType, token)
        } else {
            // Clear the auth token
            accountManager.invalidateAuthToken(accountType, accountManager.peekAuthToken(account, authTokenType))
        }
        AuthState.token = token
    }
}

