package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__sync_interrupt_confirm
import gamerlogue.sharedui.generated.resources.settings__sync_interrupt_continue
import gamerlogue.sharedui.generated.resources.settings__sync_interrupt_message
import gamerlogue.sharedui.generated.resources.settings__sync_interrupt_title
import org.jetbrains.compose.resources.stringResource

/**
 * Guards leaving a running sync flow: while [running], back requests [onAsk] instead of popping the screen,
 * and while [asking] a dialog warns that the operation will be interrupted — [onConfirm] leaves anyway,
 * [onDismiss] keeps it going.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SyncInterruptGuard(
    running: Boolean,
    asking: Boolean,
    onAsk: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = running,
        onBackCompleted = onAsk,
    )
    if (!asking) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings__sync_interrupt_title)) },
        text = { Text(stringResource(Res.string.settings__sync_interrupt_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.settings__sync_interrupt_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.settings__sync_interrupt_continue))
            }
        },
    )
}
