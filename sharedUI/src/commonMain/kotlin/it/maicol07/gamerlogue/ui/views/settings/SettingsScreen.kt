package it.maicol07.gamerlogue.ui.views.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.graphics.shapes.RoundedPolygon
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.auth__cancel
import gamerlogue.sharedui.generated.resources.auth__logout
import gamerlogue.sharedui.generated.resources.auth__logout_confirm_message
import gamerlogue.sharedui.generated.resources.nav__settings
import gamerlogue.sharedui.generated.resources.settings__appearance
import gamerlogue.sharedui.generated.resources.settings__appearance_summary
import gamerlogue.sharedui.generated.resources.settings__group_account
import gamerlogue.sharedui.generated.resources.settings__group_advanced
import gamerlogue.sharedui.generated.resources.settings__group_app
import gamerlogue.sharedui.generated.resources.settings__linked_services
import gamerlogue.sharedui.generated.resources.settings__linked_services_summary
import gamerlogue.sharedui.generated.resources.settings__server
import gamerlogue.sharedui.generated.resources.settings__server_summary
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DnsW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LinkedServicesW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LogoutW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PaletteW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes
import it.maicol07.gamerlogue.ui.components.SectionIcon
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.layout.SegmentedListLayout
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.settings.components.SettingsGroupHeader
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private data class SettingsGroup(val title: StringResource, val entries: List<SettingsEntry>)

private data class SettingsEntry(
    val title: StringResource,
    val summary: StringResource?,
    val icon: ImageVector,
    val iconShape: RoundedPolygon,
    val iconContainerColor: Color,
    val iconColor: Color,
    val onClick: () -> Unit,
    /** Destructive entries are tinted with the error color and, being actions, have no chevron. */
    val destructive: Boolean = false,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun SettingsView(
    viewModel: SettingsViewModel = koinInject()
) {
    val backStack = LocalNavigationState.current.backStack
    val colors = MaterialTheme.colorScheme
    var logoutDialogOpen by remember { mutableStateOf(false) }
    val groups = listOf(
        SettingsGroup(
            Res.string.settings__group_app,
            listOf(
                SettingsEntry(
                    Res.string.settings__appearance, Res.string.settings__appearance_summary,
                    Icons.PaletteW500Rounded, MaterialShapes.Cookie6Sided, colors.primaryContainer, colors.onPrimaryContainer,
                    { backStack.add(RootNavTree.Appearance) },
                ),
                SettingsEntry(
                    Res.string.settings__linked_services, Res.string.settings__linked_services_summary,
                    Icons.LinkedServicesW500Rounded, MaterialShapes.Clover4Leaf, colors.tertiaryContainer, colors.onTertiaryContainer,
                    { backStack.add(RootNavTree.LinkedServices) },
                ),
            )
        ),
        SettingsGroup(
            Res.string.settings__group_advanced,
            listOf(
                SettingsEntry(
                    Res.string.settings__server, Res.string.settings__server_summary,
                    Icons.DnsW500Rounded, MaterialShapes.Pill, colors.secondaryContainer, colors.onSecondaryContainer,
                    { backStack.add(RootNavTree.ServerSettings) },
                ),
            )
        ),
        SettingsGroup(
            Res.string.settings__group_account,
            listOf(
                SettingsEntry(
                    Res.string.auth__logout, null,
                    Icons.LogoutW500Rounded, MaterialShapes.Cookie4Sided, colors.errorContainer, colors.onErrorContainer,
                    { logoutDialogOpen = true }, destructive = true,
                ),
            )
        ),
    )

    ScreenScaffold(title = Res.string.nav__settings) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding)
        ) {
            groups.forEach { group ->
                SettingsGroupHeader(stringResource(group.title))
                SegmentedListLayout {
                    group.entries.forEachIndexed { index, entry -> SettingsEntryItem(entry, index, group.entries.size) }
                }
            }
        }
    }

    if (logoutDialogOpen) {
        AlertDialog(
            onDismissRequest = { logoutDialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    logoutDialogOpen = false
                    viewModel.logout()
                }) { Text(stringResource(Res.string.auth__logout)) }
            },
            dismissButton = {
                TextButton(onClick = { logoutDialogOpen = false }) { Text(stringResource(Res.string.auth__cancel)) }
            },
            icon = { Icon(Icons.LogoutW500Rounded, null) },
            title = { Text(stringResource(Res.string.auth__logout)) },
            text = { Text(stringResource(Res.string.auth__logout_confirm_message)) },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsEntryItem(entry: SettingsEntry, index: Int, count: Int) {
    val contentColor = if (entry.destructive) MaterialTheme.colorScheme.error else Color.Unspecified
    SettingsMenuLink(
        title = { Text(stringResource(entry.title)) },
        subtitle = entry.summary?.let { { Text(stringResource(it)) } },
        onClick = entry.onClick,
        icon = { SectionIcon(entry.icon, entry.iconShape, entry.iconContainerColor, entry.iconColor) },
        action = if (entry.destructive) null else {
            { Icon(Icons.KeyboardArrowRightW500Rounded, null) }
        },
        shapes = ListItemDefaults.expressiveSegmentedShapes(index, count),
        colors = ListItemDefaults.expressiveSegmentedColors(contentColor = contentColor),
    )
}
