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
    ): Bundle {
        return Bundle()
    }

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
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account?.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account?.type)
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            return result
        }

        return Bundle()
    }

    // Get the label for the account type
    override fun getAuthTokenLabel(authTokenType: String?): String {
        return "Gamerlogue"
    }

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
    ): Bundle {
        return Bundle()
    }

    // We don't support updating credentials
    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        return Bundle()
    }

    // We don't have any specific account features
    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle {
        val result = Bundle()
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
        return result
    }
}

