// Generated using MaterialKolor Builder version 1.2.1 (103)
// https://materialkolor.com/?color_seed=FF68A500&dark_mode=false&selected_preset_id=res-0&color_spec=SPEC_2025&package_name=it.maicol07.gamerlogue.theme&misc=true&expressive=true

package it.maicol07.gamerlogue.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicMaterialThemeState
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanOrNullStateFlow
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import it.maicol07.gamerlogue.ui.views.settings.utils.SettingsKeys
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSettingsApi::class)
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val settings = koinInject<ObservableSettings>()
    val isDarkSetting by remember {
        settings.getBooleanOrNullStateFlow(
            coroutineScope,
            SettingsKeys.IS_DARK_THEME.name
        )
    }.collectAsState()
    val isDarkTheme = when {
        isDarkSetting != null -> isDarkSetting!!
        else -> isSystemInDarkMode()
    }

    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = isDarkTheme,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        seedColor = SeedColor,
    )

    DynamicMaterialExpressiveTheme(
        state = dynamicThemeState,
        motionScheme = MotionScheme.expressive(),
        animate = true,
        content = content,
    )
}
