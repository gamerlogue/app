package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AndroidWifi3BarAlertW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowBackW500Rounded
import it.maicol07.gamerlogue.NavBackStack
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTopBar(
    title: StringResource?,
    modifier: Modifier = Modifier,
    backStack: NavBackStack = koinInject(),
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { title?.let { Text(stringResource(it)) } },
        modifier = modifier,
        navigationIcon = {
            if (backStack.size > 1) {
                IconButton(
                    onClick = { backStack.removeAt(backStack.lastIndex) },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(Icons.ArrowBackW500Rounded, contentDescription = null)
                }
            }
        },
        actions = {
            actions()
            NetworkErrorAction()
        }
    )
}

/** Network-error indicator shared by every top bar; opens the exception bottom sheet when tapped. */
@Composable
fun NetworkErrorAction() {
    val appUiState = LocalAppUiState.current
    AnimatedVisibility(appUiState.networkException != null) {
        IconButton(onClick = { appUiState.showExceptionBottomSheet = true }) {
            Icon(
                Icons.AndroidWifi3BarAlertW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
