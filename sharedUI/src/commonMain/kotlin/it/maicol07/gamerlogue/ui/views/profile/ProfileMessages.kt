package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__retry
import gamerlogue.sharedui.generated.resources.profile__blocked
import gamerlogue.sharedui.generated.resources.profile__blocked_hint
import gamerlogue.sharedui.generated.resources.profile__load_error
import gamerlogue.sharedui.generated.resources.profile__unblock
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AndroidWifi3BarAlertW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.BlockW500Rounded
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val LoadingPaddingScale = 4
private const val MessagePaddingScale = 2

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ProfileLoading() = Box(
    modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPadding * LoadingPaddingScale),
    contentAlignment = Alignment.Center,
) {
    ContainedLoadingIndicator()
}

@Composable
internal fun ProfileMessage(
    icon: ImageVector,
    title: StringResource,
    hint: StringResource?,
    action: @Composable () -> Unit = {},
) = Box(
    modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPadding * MessagePaddingScale),
    contentAlignment = Alignment.Center,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyState(icon, stringResource(title), hint?.let { stringResource(it) })
        action()
    }
}

@Composable
internal fun ProfileBlocked(canUnblock: Boolean, onUnblock: () -> Unit) =
    ProfileMessage(Icons.BlockW500Rounded, Res.string.profile__blocked, Res.string.profile__blocked_hint) {
        if (canUnblock) MessageButton(Res.string.profile__unblock, onUnblock)
    }

@Composable
internal fun ProfileLoadError(onRetry: () -> Unit) =
    ProfileMessage(Icons.AndroidWifi3BarAlertW500Rounded, Res.string.profile__load_error, null) {
        MessageButton(Res.string.game__retry, onRetry)
    }

@Composable
private fun MessageButton(label: StringResource, onClick: () -> Unit) = Button(
    onClick = onClick,
    shapes = ButtonDefaults.shapes(),
    modifier = Modifier.padding(top = Dimens.SectionGap),
) {
    Text(stringResource(label))
}
