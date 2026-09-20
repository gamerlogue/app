package it.maicol07.gamerlogue.ui.views.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.safeRequest
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Only the settings action that needs a coroutine scope tied to the screen. Preferences are read and
 * written straight through `AppPreferences`, which outlives any screen.
 */
@KoinViewModel
class SettingsViewModel(
    private val authHandler: AuthenticationHandler,
    private val exceptionReporter: ExceptionReporter
) : ViewModel() {
    // Goes through the handler, not the token provider: a cookie-based session also has to be dropped
    // server-side, or the next restore would sign the user straight back in.
    fun logout() {
        viewModelScope.launch { exceptionReporter.safeRequest { authHandler.logout() } }
    }
}
