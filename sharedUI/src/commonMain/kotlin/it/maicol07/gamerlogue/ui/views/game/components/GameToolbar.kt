package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.BookmarkW500RoundedFill
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.EditW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlayCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlayCircleW500RoundedFill
import it.maicol07.gamerlogue.ui.components.TooltipBox
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Library actions for the game: backlog and playing toggles around a larger add/edit button, so the
 * primary action stands out by size rather than by color.
 */
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
            Icons.BookmarkW500RoundedFill,
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
            Icons.PlayCircleW500RoundedFill,
            currentGameStatus == GameLibraryStatus.PLAYING,
            loading = pendingStatus == GameLibraryStatus.PLAYING,
            enabled = currentGameStatus == null || currentGameStatus == GameLibraryStatus.PLAYING,
            onPlayingClick
        )
    },
    content = {
        GameToolbarAddButton(inLibrary = currentGameStatus != null, onClick = onAddClick)
    },
)

/** The primary action, larger than the toggles; its icon animates between add and edit. */
@Composable
private fun GameToolbarAddButton(inLibrary: Boolean, onClick: () -> Unit) = FilledIconButton(
    onClick = onClick,
    shapes = IconButtonDefaults.shapes(),
    modifier = Modifier.size(
        IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)
    )
) {
    val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    AnimatedContent(
        inLibrary,
        transitionSpec = {
            (scaleIn(spatialSpec) + fadeIn(effectsSpec)) togetherWith (scaleOut(spatialSpec) + fadeOut(effectsSpec))
        }
    ) { editing ->
        Icon(
            if (editing) Icons.EditW500Rounded else Icons.AddW500Rounded,
            contentDescription = stringResource(Res.string.library__add_to_library),
            modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameToolbarToggleIconButton(
    text: StringResource,
    icon: ImageVector,
    checkedIcon: ImageVector,
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
    FilledIconToggleButton(
        checked = checked,
        enabled = enabled && !loading,
        onCheckedChange = { onClick() },
        shapes = IconButtonDefaults.toggleableShapes(),
    ) {
        if (loading) {
            // Icon color and no track, to keep 3:1 contrast on the button container.
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = LocalContentColor.current,
                trackColor = Color.Transparent
            )
        } else {
            Icon(if (checked) checkedIcon else icon, contentDescription = stringResource(text))
        }
    }
}
