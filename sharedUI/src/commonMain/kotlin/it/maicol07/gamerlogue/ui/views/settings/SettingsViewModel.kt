package it.maicol07.gamerlogue.ui.views.settings

import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanOrNullStateFlow
import com.russhwolf.settings.coroutines.getStringOrNullStateFlow
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.safeRequest
import it.maicol07.gamerlogue.ui.views.settings.utils.SettingsKeys
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@OptIn(ExperimentalSettingsApi::class)
@KoinViewModel
class SettingsViewModel(
    private val settings: ObservableSettings,
    private val authHandler: AuthenticationHandler,
    private val exceptionReporter: ExceptionReporter
) : ViewModel() {
    val isDarkTheme = settings.getBooleanOrNullStateFlow(viewModelScope, SettingsKeys.IS_DARK_THEME.name)
    val language = settings.getStringOrNullStateFlow(viewModelScope, SettingsKeys.LANGUAGE.name)
    val useDynamicColors = settings.getBooleanOrNullStateFlow(viewModelScope, SettingsKeys.USE_DYNAMIC_COLORS.name)

    fun setTheme(theme: AppTheme) {
        if (theme == AppTheme.SYSTEM) {
            settings.remove(SettingsKeys.IS_DARK_THEME.name)
        } else {
            settings.putBoolean(SettingsKeys.IS_DARK_THEME.name, theme == AppTheme.DARK)
        }
    }

    fun setLanguage(language: Locale?) = settings.apply {
        if (language != null) {
            putString(
                SettingsKeys.LANGUAGE.name,
                language.language
            )
        } else {
            remove(SettingsKeys.LANGUAGE.name)
        }
    }
    fun setUseDynamicColors(use: Boolean) = settings.putBoolean(SettingsKeys.USE_DYNAMIC_COLORS.name, use)

    // Goes through the handler, not the token provider: a cookie-based session also has to be dropped
    // server-side, or the next restore would sign the user straight back in.
    fun logout() {
        viewModelScope.launch { exceptionReporter.safeRequest { authHandler.logout() } }
    }
}
