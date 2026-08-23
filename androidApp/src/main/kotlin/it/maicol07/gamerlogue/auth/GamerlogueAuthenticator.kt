package it.maicol07.gamerlogue.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle

/**
 * Authenticator for Gamerlogue accounts.
 * This class handles account authentication with the Android AccountManager system.
 */
class GamerlogueAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context) {

    // We don't support adding accounts from the system settings
    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ) = Bundle()

    // Return the auth token for an account
    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        val accountManager = AccountManager.get(context)
        val authToken = accountManager.peekAuthToken(account, authTokenType)

        if (!authToken.isNullOrEmpty()) {
            return Bundle().apply {
                putString(AccountManager.KEY_ACCOUNT_NAME, account?.name)
                putString(AccountManager.KEY_ACCOUNT_TYPE, account?.type)
                putString(AccountManager.KEY_AUTHTOKEN, authToken)
            }
        }

        return Bundle()
    }

    // Get the label for the account type
    override fun getAuthTokenLabel(authTokenType: String?) = "Gamerlogue"

    // We don't support editing account properties
    override fun editProperties(
        response: AccountAuthenticatorResponse?,
        accountType: String?
    ): Bundle {
        return Bundle()
    }

    // We don't support confirming credentials
    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ) = Bundle()

    // We don't support updating credentials
    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ) = Bundle()

    // We don't have any specific account features
    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ) = Bundle().apply { putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false) }
}

