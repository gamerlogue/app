package it.maicol07.gamerlogue.ui.views.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.auth__cancel
import gamerlogue.sharedui.generated.resources.auth__logout
import gamerlogue.sharedui.generated.resources.auth__logout_confirm_message
import gamerlogue.sharedui.generated.resources.nav__settings
import gamerlogue.sharedui.generated.resources.settings__appearance
import gamerlogue.sharedui.generated.resources.settings__linked_services
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LinkedServicesW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LogoutW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PaletteW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private data class SettingsEntry(
    val title: StringResource,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun SettingsView(
    viewModel: SettingsViewModel = koinInject()
) {
    val backStack = LocalNavigationState.current.backStack
    var logoutDialogOpen by remember { mutableStateOf(false) }
    val entries = listOf(
        SettingsEntry(Res.string.settings__appearance, Icons.PaletteW500Rounded, { backStack.add(RootNavTree.Appearance) }),
        SettingsEntry(Res.string.settings__linked_services, Icons.LinkedServicesW500Rounded, { backStack.add(RootNavTree.LinkedServices) }),
        SettingsEntry(Res.string.auth__logout, Icons.LogoutW500Rounded, { logoutDialogOpen = true }, destructive = true),
    )

    ScreenScaffold(title = Res.string.nav__settings) {
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            entries.forEachIndexed { index, entry ->
                val contentColor = if (entry.destructive) MaterialTheme.colorScheme.error else Color.Unspecified
                SettingsMenuLink(
                    title = { Text(stringResource(entry.title)) },
                    onClick = entry.onClick,
                    icon = { Icon(entry.icon, null) },
                    shapes = ListItemDefaults.segmentedShapes(index, entries.size),
                    colors = ListItemDefaults.expressiveSegmentedColors(
                        contentColor = contentColor,
                        leadingContentColor = contentColor,
                    ),
                )
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
