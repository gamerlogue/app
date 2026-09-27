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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common__back
import gamerlogue.sharedui.generated.resources.exception__action_show
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowBackW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import it.maicol07.gamerlogue.ui.navigation.AppNavigationState
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.core.ExceptionReporter
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AppTopBar(
    title: StringResource?,
    modifier: Modifier = Modifier,
    navigationState: AppNavigationState = LocalNavigationState.current,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { title?.let { Text(stringResource(it)) } },
        modifier = modifier,
        navigationIcon = {
            if (navigationState.backStack.size > 1) {
                IconButton(
                    onClick = navigationState::navigateBack,
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(Icons.ArrowBackW500Rounded, stringResource(Res.string.common__back))
                }
            }
        },
        actions = {
            actions()
            GlobalErrorAction()
        }
    )
}

@Composable
fun GlobalErrorAction() {
    val reporter = koinInject<ExceptionReporter>()
    val errorState by reporter.state.collectAsStateWithLifecycle()
    AnimatedVisibility(errorState != null) {
        IconButton(onClick = { reporter.show() }) {
            Icon(
                Icons.ErrorW500Rounded,
                contentDescription = stringResource(Res.string.exception__action_show),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
