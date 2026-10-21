package it.maicol07.gamerlogue.ui.views.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.get
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.safeRequest
import it.maicol07.gamerlogue.data.ProfileApi
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Only the settings action that needs a coroutine scope tied to the screen. Preferences are read and
 * written straight through `AppPreferences`, which outlives any screen.
 */
@KoinViewModel
class SettingsViewModel(
    private val authHandler: AuthenticationHandler,
    private val authProvider: AuthTokenProvider,
    private val profileApi: ProfileApi,
    private val exceptionReporter: ExceptionReporter
) : ViewModel() {
    // Goes through the handler, not the token provider: a cookie-based session also has to be dropped
    // server-side, or the next restore would sign the user straight back in.
    fun logout() {
        viewModelScope.launch { exceptionReporter.safeRequest { authHandler.logout() } }
    }

    /**
     * Deletes the account for good, then forgets the local session. Not through [AuthenticationHandler.logout]:
     * the server has already revoked every token and dropped every session, so the revoke or `POST /logout`
     * it sends would only be refused, leaving the local session in place.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            if (exceptionReporter.safeRequest { profileApi.deleteAccount() }.isOk) authProvider.clearSession()
        }
    }

    /** Downloads the account's data from the server and hands it to [save]; false when either fails or is cancelled. */
    suspend fun exportAccount(save: suspend (bytes: ByteArray, fileName: String) -> Boolean): Boolean {
        val data = exceptionReporter.safeRequest { profileApi.exportAccount() }.get() ?: return false
        return save(data.encodeToByteArray(), EXPORT_FILE_NAME)
    }

    private companion object {
        /** The name the server gives the export too. */
        const val EXPORT_FILE_NAME = "gamerlogue-export.json"
    }
}
