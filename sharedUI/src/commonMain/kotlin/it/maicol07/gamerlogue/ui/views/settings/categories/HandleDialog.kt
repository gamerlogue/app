package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_cancel
import gamerlogue.sharedui.generated.resources.settings__service_connect
import gamerlogue.sharedui.generated.resources.settings__service_handle_label
import gamerlogue.sharedui.generated.resources.settings__service_handle_message
import gamerlogue.sharedui.generated.resources.settings__service_handle_title
import org.jetbrains.compose.resources.stringResource

/** Asks for the public username of a service with no sign-in (Stash). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HandleDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val handle = rememberTextFieldState()
    val value = handle.text.toString().trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings__service_handle_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.settings__service_handle_message))
                TextField(
                    state = handle,
                    label = { Text(stringResource(Res.string.settings__service_handle_label)) },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = value.isNotEmpty(), onClick = { onConfirm(value) }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.settings__service_connect))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )
}
