package it.maicol07.gamerlogue.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_ok
import gamerlogue.sharedui.generated.resources.common_select_date
import gamerlogue.sharedui.generated.resources.date__input_placeholder
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DateRangeW500Rounded
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun DatePickerFieldDialog(
    label: String,
    initialDate: Long? = null,
    modifier: Modifier = Modifier,
    onDateSelected: (Long?) -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var showDialog by remember { mutableStateOf(false) }

    TextField(
        value = selectedDate?.let { convertMillisToDate(it) } ?: "",
        onValueChange = { },
        label = { Text(label) },
        placeholder = { Text(stringResource(Res.string.date__input_placeholder)) },
        leadingIcon = {
            Icon(Icons.DateRangeW500Rounded, contentDescription = stringResource(Res.string.common_select_date))
        },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(selectedDate) {
                awaitEachGesture {
                    // Modifier.clickable doesn't work for text fields, so we use Modifier.pointerInput
                    // in the Initial pass to observe events before the text field consumes them
                    // in the Main pass.
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (upEvent != null) {
                        showDialog = true
                    }
                }
            }
    )

    if (showDialog) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            confirmButton = {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = {
                        selectedDate = state.selectedDateMillis
                        onDateSelected(selectedDate)
                        showDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.common_ok))
                }
            },
            onDismissRequest = { showDialog = false }
        ) {
            DatePicker(
                state = state,
            )
        }
    }
}

fun convertMillisToDate(millis: Long): String {
    Instant.fromEpochMilliseconds(millis).let {
        val kdate = it.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        return "${kdate.month.number}/${kdate.day}/${kdate.year}"
    }
}
