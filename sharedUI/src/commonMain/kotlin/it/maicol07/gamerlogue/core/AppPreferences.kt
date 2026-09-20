package it.maicol07.gamerlogue.core

import androidx.compose.ui.text.intl.Locale
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanOrNullStateFlow
import com.russhwolf.settings.coroutines.getStringOrNullStateFlow
import it.maicol07.gamerlogue.applyAppLanguage
import it.maicol07.gamerlogue.ui.theme.AppTheme
import it.maicol07.gamerlogue.ui.views.settings.utils.SettingsKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Single

/**
 * The single place that knows how user preferences are stored.
 *
 * Both the theme layer and the settings screens read from here, so the key names and the meaning of
 * an absent value (system default) live in one file instead of being re-derived at every call site.
 *
 * App-scoped rather than tied to a screen: [AppTheme] is applied above the navigation host, which
 * has no ViewModel store of its own.
 */
@OptIn(ExperimentalSettingsApi::class)
@Single
class AppPreferences(private val settings: ObservableSettings) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val theme: StateFlow<AppTheme> =
        settings.getBooleanOrNullStateFlow(scope, SettingsKeys.IS_DARK_THEME.name)
            .mapState(AppTheme::of)

    val useDynamicColors: StateFlow<Boolean> =
        settings.getBooleanOrNullStateFlow(scope, SettingsKeys.USE_DYNAMIC_COLORS.name)
            .mapState { it ?: true }

    val language: StateFlow<String?> =
        settings.getStringOrNullStateFlow(scope, SettingsKeys.LANGUAGE.name)

    init {
        // The stored language has to reach the platform before the first strings are resolved.
        applyAppLanguage(language.value)
    }

    fun setTheme(theme: AppTheme) {
        val isDark = theme.isDark
        if (isDark == null) {
            settings.remove(SettingsKeys.IS_DARK_THEME.name)
        } else {
            settings.putBoolean(SettingsKeys.IS_DARK_THEME.name, isDark)
        }
    }

    fun setUseDynamicColors(use: Boolean) {
        settings.putBoolean(SettingsKeys.USE_DYNAMIC_COLORS.name, use)
    }

    fun setLanguage(language: Locale?) {
        if (language == null) {
            settings.remove(SettingsKeys.LANGUAGE.name)
        } else {
            settings.putString(SettingsKeys.LANGUAGE.name, language.language)
        }
        applyAppLanguage(language?.language)
    }

    private fun <T, R> StateFlow<T>.mapState(transform: (T) -> R): StateFlow<R> =
        map(transform).stateIn(scope, SharingStarted.Eagerly, transform(value))
}
