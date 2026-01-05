// Generated using MaterialKolor Builder version 1.2.1 (103)
// https://materialkolor.com/?color_seed=FF68A500&dark_mode=false&selected_preset_id=res-0&color_spec=SPEC_2025&package_name=it.maicol07.gamerlogue.theme&misc=true&expressive=true

package it.maicol07.gamerlogue.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
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
