package it.maicol07.gamerlogue.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun NumericField(
    state: TextFieldState,
    label: String,
    suffix: String? = null,
    leadingIcon: ImageVector? = null,
    allowDecimals: Boolean = true,
    modifier: Modifier = Modifier,
) {
    TextField(
        state = state,
        inputTransformation = InputTransformation.then {
            var string = asCharSequence().toString()

            if (!allowDecimals && string.contains('.')) {
                revertAllChanges()
            } else {
                // Remove dots to let string pass
                string = string.replace(".", "")
            }

            if (string.isNotBlank() && string.toDoubleOrNull() == null) {
                revertAllChanges()
            }
        },
        modifier = modifier,
        label = { Text(label) },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        leadingIcon = leadingIcon?.let { @Composable { Icon(it, null) } },
        suffix = suffix?.let { { Text(suffix) } }
    )
}
