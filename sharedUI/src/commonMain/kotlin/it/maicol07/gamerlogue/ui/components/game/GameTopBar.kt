package it.maicol07.gamerlogue.ui.components.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowBackW500Rounded
import it.maicol07.gamerlogue.LocalNavBackStack
import it.maicol07.gamerlogue.NavBackStack
import it.maicol07.gamerlogue.ui.components.layout.NetworkErrorAction

var LocalGameTopBarOverlayMode = staticCompositionLocalOf<MutableState<Boolean>> {
    error("No LocalGameTopBarOverlayMode provided")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameTopBar(
    gameName: String?,
    modifier: Modifier = Modifier,
    backStack: NavBackStack = LocalNavBackStack.current
) {
    val canNavigateBack = backStack.size > 1
    val isOverlayMode by LocalGameTopBarOverlayMode.current
    val containerColor by animateColorAsState(
        if (isOverlayMode) Color.Transparent
        else MaterialTheme.colorScheme.surface
    )
    val contentColor by animateColorAsState(
        if (isOverlayMode) Color.White
        else MaterialTheme.colorScheme.onSurface
    )

    TopAppBar(
        title = {
            AnimatedVisibility(!isOverlayMode) {
                Text(
                    text = gameName ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        modifier = modifier.zIndex(1f),
        navigationIcon = {
            if (canNavigateBack) {
                FilledTonalIconButton(
                    onClick = { backStack.removeLast() },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        Icons.ArrowBackW500Rounded,
                        contentDescription = null
                    )
                }
            }
        },
        actions = { NetworkErrorAction() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        )
    )
}
