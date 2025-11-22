package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AndroidWifi3BarAlertW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowBackW500Rounded
import it.maicol07.gamerlogue.NavKeys
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTopBar(
    currentNavKey: NavKey,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topBarState = LocalTopBarState.current

    if (topBarState.visible) {
        TopAppBar(
            title = {
                topBarState.customTitle?.invoke()
                    ?: (currentNavKey as? NavKeys.NavKeyWithMeta)?.title?.let { Text(stringResource(it)) }
            },
            modifier = modifier,
            navigationIcon = {
                if (canNavigateBack) {
                    IconButton(
                        onClick = navigateUp,
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            Icons.ArrowBackW500Rounded,
                            contentDescription = null
                        )
                    }
                }
            },
            actions = {
                val appUiState = LocalAppUiState.current
                AnimatedVisibility(appUiState.networkException.value != null) {
                    IconButton(
                        onClick = { appUiState.showExceptionBottomSheet.value = true }
                    ) {
                        Icon(
                            Icons.AndroidWifi3BarAlertW500Rounded,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            colors = topBarState.customColors ?: TopAppBarDefaults.topAppBarColors()
        )
    }
}
