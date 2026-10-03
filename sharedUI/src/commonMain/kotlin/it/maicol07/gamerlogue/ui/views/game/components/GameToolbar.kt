package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.library__add_to_backlog
import gamerlogue.sharedui.generated.resources.library__add_to_library
import gamerlogue.sharedui.generated.resources.library__add_to_playing
import gamerlogue.sharedui.generated.resources.library__remove_from_backlog
import gamerlogue.sharedui.generated.resources.library__remove_from_playing
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AddW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.BookmarkW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.EditW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlayCircleW500Rounded
import it.maicol07.gamerlogue.ui.components.TooltipBox
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BoxScope.GameToolbar(
    expanded: Boolean,
    currentGameStatus: GameLibraryStatus?,
    pendingStatus: GameLibraryStatus?,
    onBacklogClick: () -> Unit,
    onPlayingClick: () -> Unit,
    onAddClick: () -> Unit,
) = HorizontalFloatingToolbar(
    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().offset(y = -ScreenOffset),
    expanded = expanded,
    leadingContent = {
        GameToolbarToggleIconButton(
            Res.string.run {
                if (currentGameStatus == GameLibraryStatus.BACKLOG) {
                    library__remove_from_backlog
                } else {
                    library__add_to_backlog
                }
            },
            Icons.BookmarkW500Rounded,
            currentGameStatus == GameLibraryStatus.BACKLOG,
            loading = pendingStatus == GameLibraryStatus.BACKLOG,
            enabled = currentGameStatus == null || currentGameStatus == GameLibraryStatus.BACKLOG,
            onBacklogClick
        )
    },
    trailingContent = {
        GameToolbarToggleIconButton(
            Res.string.run {
                if (currentGameStatus == GameLibraryStatus.PLAYING) {
                    library__remove_from_playing
                } else {
                    library__add_to_playing
                }
            },
            Icons.PlayCircleW500Rounded,
            currentGameStatus == GameLibraryStatus.PLAYING,
            loading = pendingStatus == GameLibraryStatus.PLAYING,
            enabled = currentGameStatus == null || currentGameStatus == GameLibraryStatus.PLAYING,
            onPlayingClick
        )
    },
    content = {
        FilledIconButton(
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.width(64.dp),
            onClick = onAddClick
        ) {
            Icon(
                if (currentGameStatus == null) {
                    Icons.AddW500Rounded
                } else {
                    Icons.EditW500Rounded
                },
                contentDescription = stringResource(Res.string.library__add_to_library)
            )
        }
    },
)

@Composable
private fun GameToolbarToggleIconButton(
    text: StringResource,
    icon: ImageVector,
    checked: Boolean,
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) = TooltipBox(
    tooltip = {
        PlainTooltip {
            Text(stringResource(text))
        }
    }
) {
    FilledTonalIconToggleButton(
        checked = checked,
        enabled = enabled && !loading,
        onCheckedChange = { onClick() },
        shapes = IconButtonDefaults.toggleableShapes(),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            Icon(icon, contentDescription = stringResource(text))
        }
    }
}
