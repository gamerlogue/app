package it.maicol07.gamerlogue.ui.views.userlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_cancel
import gamerlogue.sharedui.generated.resources.lists__delete
import gamerlogue.sharedui.generated.resources.lists__delete_confirm
import gamerlogue.sharedui.generated.resources.lists__description
import gamerlogue.sharedui.generated.resources.lists__edit
import gamerlogue.sharedui.generated.resources.lists__name
import gamerlogue.sharedui.generated.resources.lists__new
import gamerlogue.sharedui.generated.resources.lists__private
import gamerlogue.sharedui.generated.resources.lists__private_hint
import gamerlogue.sharedui.generated.resources.lists__ranked
import gamerlogue.sharedui.generated.resources.lists__ranked_hint
import gamerlogue.sharedui.generated.resources.lists__save
import it.maicol07.gamerlogue.data.UserList
import it.maicol07.gamerlogue.data.UserListSchema
import it.maicol07.gamerlogue.data.UserSchema
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What the list dialog edits. */
data class UserListForm(val name: String, val description: String?, val private: Boolean, val ranked: Boolean) {
    fun applyTo(list: UserList) {
        list.name = name
        list.description = description
        list.visibility = if (private) UserSchema.VISIBILITY_PRIVATE else UserSchema.VISIBILITY_PUBLIC
        list.isRanked = ranked
    }
}

/**
 * Creates ([initial] null) or edits a list's name, description, visibility and ranking. [onDelete],
 * for an existing list, adds the delete action, confirmed before it runs.
 */
@Composable
fun UserListFormDialog(
    initial: UserListSchema?,
    onSave: (UserListForm) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val name = rememberTextFieldState(initial?.name.orEmpty())
    val description = rememberTextFieldState(initial?.description.orEmpty())
    var private by remember { mutableStateOf(initial?.isPrivate == true) }
    var ranked by remember { mutableStateOf(initial?.isRanked == true) }
    var deleteOpen by remember { mutableStateOf(false) }
    val validName = name.text.isNotBlank() && name.text.length <= UserListSchema.MAX_NAME_LENGTH
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) Res.string.lists__new else Res.string.lists__edit)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            ) {
                FormTextFields(name, description)
                HintedSwitch(Res.string.lists__private, Res.string.lists__private_hint, private) { private = it }
                HintedSwitch(Res.string.lists__ranked, Res.string.lists__ranked_hint, ranked) { ranked = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = validName,
                onClick = {
                    onSave(
                        UserListForm(
                            name = name.text.toString().trim(),
                            description = description.text.toString()
                                .trim()
                                .take(UserListSchema.MAX_DESCRIPTION_LENGTH)
                                .ifBlank { null },
                            private = private,
                            ranked = ranked,
                        )
                    )
                    onDismiss()
                },
                shapes = ButtonDefaults.shapes(),
            ) { Text(stringResource(Res.string.lists__save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) DeleteButton { deleteOpen = true }
                TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        },
    )
    if (deleteOpen && onDelete != null) {
        DeleteConfirmDialog(
            onConfirm = {
                onDismiss()
                onDelete()
            },
            onDismiss = { deleteOpen = false }
        )
    }
}

@Composable
private fun FormTextFields(name: TextFieldState, description: TextFieldState) {
    TextField(
        state = name,
        label = { Text(stringResource(Res.string.lists__name)) },
        lineLimits = TextFieldLineLimits.SingleLine,
        modifier = Modifier.fillMaxWidth()
    )
    TextField(
        state = description,
        label = { Text(stringResource(Res.string.lists__description)) },
        lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 5),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DeleteButton(onClick: () -> Unit) = TextButton(
    onClick = onClick,
    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
    shapes = ButtonDefaults.shapes(),
) { Text(stringResource(Res.string.lists__delete)) }

@Composable
private fun DeleteConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.lists__delete)) },
    text = { Text(stringResource(Res.string.lists__delete_confirm)) },
    confirmButton = { DeleteButton(onConfirm) },
    dismissButton = {
        TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(Res.string.common_cancel))
        }
    },
)

/** A switch with the line telling what it does. */
@Composable
private fun HintedSwitch(label: StringResource, hint: StringResource, checked: Boolean, onChange: (Boolean) -> Unit) =
    Column {
        SwitchRow(stringResource(label), checked, onChange)
        Text(
            stringResource(hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) = Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    Text(label, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
}
