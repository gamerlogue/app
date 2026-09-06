package it.maicol07.gamerlogue.ui.views.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.auth__logout
import gamerlogue.sharedui.generated.resources.settings__appearance
import gamerlogue.sharedui.generated.resources.settings__linked_services
import gamerlogue.sharedui.generated.resources.nav__settings
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun SettingsView(
    viewModel: SettingsViewModel = koinInject()
) {
    val navigationState = LocalNavigationState.current
    val navigateTo: (NavKey) -> Unit = { navigationState.backStack.add(it) }
    ScreenScaffold(title = Res.string.nav__settings) {
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            val sections = remember {
                mapOf(
                    Res.string.settings__appearance to Triple(
                        { navigateTo(RootNavTree.Appearance) },
                        Icons.PaletteW500Rounded,
                        null
                    ),
                    Res.string.settings__linked_services to Triple(
                        { navigateTo(RootNavTree.LinkedServices) },
                        Icons.LinkedServicesW500Rounded,
                        null
                    ),
                    Res.string.auth__logout to Triple(
                        { viewModel.logout() },
                        Icons.LogoutW500Rounded,
                        @Composable {
                            ListItemDefaults.expressiveSegmentedColors(
                                contentColor = MaterialTheme.colorScheme.error,
                                overlineContentColor = MaterialTheme.colorScheme.error,
                                trailingContentColor = MaterialTheme.colorScheme.error,
                                leadingContentColor = MaterialTheme.colorScheme.error,
                                supportingContentColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            )
                        }
                    )
                )
            }

            sections.onEachIndexed { index, (title, attributes) ->
                val (onClick, icon, itemColor) = attributes
                SettingsMenuLink(
                    title = { Text(stringResource(title)) },
                    onClick = onClick,
                    icon = { Icon(icon, null) },
                    shapes = ListItemDefaults.segmentedShapes(index, sections.size),
                    colors = itemColor?.invoke() ?: ListItemDefaults.expressiveSegmentedColors(),
                )
            }
        }
    }
}
