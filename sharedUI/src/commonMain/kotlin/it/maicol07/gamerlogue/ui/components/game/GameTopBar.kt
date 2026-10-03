package it.maicol07.gamerlogue.ui.components.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common__back
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowBackW500Rounded
import it.maicol07.gamerlogue.ui.components.layout.GlobalErrorAction
import it.maicol07.gamerlogue.ui.navigation.AppNavigationState
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import org.jetbrains.compose.resources.stringResource

/** [isOverlayMode]: transparent over the banner, with the title hidden while the header shows it. */
@Composable
fun GameTopBar(
    gameName: String?,
    isOverlayMode: Boolean,
    modifier: Modifier = Modifier,
    navigationState: AppNavigationState = LocalNavigationState.current
) {
    val canNavigateBack = navigationState.backStack.size > 1
    val containerColor by animateColorAsState(
        if (isOverlayMode) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.surface
        }
    )
    val contentColor by animateColorAsState(
        if (isOverlayMode) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurface
        }
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
                    onClick = navigationState::navigateBack,
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        Icons.ArrowBackW500Rounded,
                        contentDescription = stringResource(Res.string.common__back)
                    )
                }
            }
        },
        actions = { GlobalErrorAction() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        )
    )
}
