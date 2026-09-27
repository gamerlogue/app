package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.expressive.SettingsButtonGroup
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import com.alorma.compose.settings.ui.expressive.SettingsSwitch
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__dynamic_colors
import gamerlogue.sharedui.generated.resources.settings__appearance
import gamerlogue.sharedui.generated.resources.settings__language
import gamerlogue.sharedui.generated.resources.settings__theme
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kdroidfilter.platformtools.getPlatform
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ContrastW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LanguageW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import it.maicol07.gamerlogue.BuildConfig
import it.maicol07.gamerlogue.appLanguageSettingsOpener
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.getDisplayLanguage
import it.maicol07.gamerlogue.extensions.getFlag
import it.maicol07.gamerlogue.extensions.supportsDeviceColors
import it.maicol07.gamerlogue.extensions.supportsSystemAppLanguage
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.theme.AppTheme
import it.maicol07.gamerlogue.ui.views.settings.components.SingleChoiceAlertDialog
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

const val TotalItems = 3

@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun AppearanceView() {
    // Injected in the body, not as a parameter: nav3ksp turns @Branch parameters into nav key
    // properties, and this is a dependency rather than a navigation argument.
    val preferences = koinInject<AppPreferences>()
    val theme by preferences.theme.collectAsStateWithLifecycle()
    val useDynamicColors by preferences.useDynamicColors.collectAsStateWithLifecycle()
    val language by preferences.language.collectAsStateWithLifecycle()

    ScreenScaffold(title = Res.string.settings__appearance) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ThemeSection(
                theme = theme,
                onThemeSelected = preferences::setTheme
            )

            if (getPlatform().supportsDeviceColors()) {
                DynamicColorsSwitch(
                    useDynamicColors = useDynamicColors,
                    onDynamicColorsToggled = preferences::setUseDynamicColors
                )
            }

            LanguageSection(language, preferences::setLanguage)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeSection(theme: AppTheme, onThemeSelected: (AppTheme) -> Unit) {
    val themeStrings = AppTheme.entries.associateWith { stringResource(it.label) }

    SettingsButtonGroup(
        title = { Text(stringResource(Res.string.settings__theme)) },
        items = AppTheme.entries,
        selectedItem = theme,
        onItemSelected = onThemeSelected,
        icon = { Icon(Icons.ContrastW500Rounded, null) },
        itemTitleMap = { themeStrings[it]!! },
        colors = ListItemDefaults.expressiveSegmentedColors(),
        shapes = ListItemDefaults.segmentedShapes(0, TotalItems)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LanguageSection(selectedLanguage: String?, onLanguageSelected: (Locale?) -> Unit) {
    var languageDialogOpen by remember { mutableStateOf(false) }

    val supportsSystemAppLanguage = getPlatform().supportsSystemAppLanguage()

    val openAppLanguageSettings = appLanguageSettingsOpener()

    SettingsMenuLink(
        title = { Text(stringResource(Res.string.settings__language)) },
        icon = { Icon(Icons.LanguageW500Rounded, null) },
        action = if (supportsSystemAppLanguage) {
            { Icon(Icons.OpenInNewW500Rounded, null) }
        } else {
            null
        },
        onClick = {
            if (supportsSystemAppLanguage) openAppLanguageSettings() else languageDialogOpen = true
        },
        colors = ListItemDefaults.expressiveSegmentedColors(),
        shapes = ListItemDefaults.segmentedShapes(2, TotalItems)
    )

    if (languageDialogOpen) {
        SingleChoiceAlertDialog(
            dialogTitle = stringResource(Res.string.settings__language),
            items = BuildConfig.AVAILABLE_LANGUAGES.values.toList(),
            // The stored override wins; with none, whatever the platform resolved is what is shown.
            selectedItem = BuildConfig.AVAILABLE_LANGUAGES.getOrElse(
                selectedLanguage ?: Locale.current.language
            ) { BuildConfig.AVAILABLE_LANGUAGES["en"] },
            onItemSelected = {
                onLanguageSelected(it)
                languageDialogOpen = false
            },
            itemIcon = { locale ->
                locale.getFlag()?.let {
                    Image(
                        it,
                        null,
                        Modifier.clip(MaterialTheme.shapes.small)
                    )
                }
            },
            itemTitle = { it.getDisplayLanguage(Locale.current)?.capitalize(Locale.current) ?: "" },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DynamicColorsSwitch(useDynamicColors: Boolean, onDynamicColorsToggled: (Boolean) -> Unit) = SettingsSwitch(
    useDynamicColors,
    title = { Text(stringResource(Res.string.settings__dynamic_colors)) },
    onCheckedChange = onDynamicColorsToggled,
    colors = ListItemDefaults.expressiveSegmentedColors(),
    shapes = ListItemDefaults.segmentedShapes(1, TotalItems)
)
