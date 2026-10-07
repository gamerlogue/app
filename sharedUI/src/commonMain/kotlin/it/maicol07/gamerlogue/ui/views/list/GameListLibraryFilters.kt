package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_ok
import gamerlogue.sharedui.generated.resources.gamelist__date_any
import gamerlogue.sharedui.generated.resources.gamelist__hours_range
import gamerlogue.sharedui.generated.resources.gamelist__hours_range_open
import gamerlogue.sharedui.generated.resources.gamelist__hours_range_up_to
import gamerlogue.sharedui.generated.resources.gamelist__library_sort_created
import gamerlogue.sharedui.generated.resources.gamelist__library_sort_updated
import gamerlogue.sharedui.generated.resources.gamelist__max_hours
import gamerlogue.sharedui.generated.resources.gamelist__min_hours
import gamerlogue.sharedui.generated.resources.gamelist__my_rating
import gamerlogue.sharedui.generated.resources.gamelist__not_owned
import gamerlogue.sharedui.generated.resources.gamelist__remove_filter
import gamerlogue.sharedui.generated.resources.gamelist__sort_field
import gamerlogue.sharedui.generated.resources.library__completion_status
import gamerlogue.sharedui.generated.resources.library__end_date
import gamerlogue.sharedui.generated.resources.library__owned
import gamerlogue.sharedui.generated.resources.library__played_time
import gamerlogue.sharedui.generated.resources.library__start_date
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CalendarMonthW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DateRangeW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HourglassW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Inventory2W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SortW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarW500Rounded
import it.maicol07.gamerlogue.data.LibraryEntrySchema.CompletionStatus
import it.maicol07.gamerlogue.ui.components.SingleSelectConnectedButtonGroup
import it.maicol07.gamerlogue.ui.components.formatDate
import it.maicol07.gamerlogue.ui.components.pickerMillisToDate
import it.maicol07.gamerlogue.ui.components.toPickerMillis
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Slider stops of the rating filter: one per point across 0..[MaxEntryRating]. */
private const val EntryRatingSteps = 9

/** Enough for any real play time; keeps the field from overflowing an Int. */
private const val MaxHoursDigits = 6

private val FieldGap = 12.dp

internal val LibrarySortField.label: StringResource
    get() = when (this) {
        LibrarySortField.UPDATED_AT -> Res.string.gamelist__library_sort_updated
        LibrarySortField.CREATED_AT -> Res.string.gamelist__library_sort_created
        LibrarySortField.RATING -> Res.string.gamelist__my_rating
        LibrarySortField.PLAYED_TIME -> Res.string.library__played_time
        LibrarySortField.START_DATE -> Res.string.library__start_date
        LibrarySortField.END_DATE -> Res.string.library__end_date
    }

internal val Boolean.ownedLabel: StringResource
    get() = if (this) Res.string.library__owned else Res.string.gamelist__not_owned

/** The span as `from – to`, or `≥ from` / `≤ to` when a side is open. */
internal fun DateSpan.label(): String = when {
    from != null && to != null -> "${formatDate(from)} – ${formatDate(to)}"
    from != null -> "≥ ${formatDate(from)}"
    to != null -> "≤ ${formatDate(to)}"
    else -> ""
}

/** The played-time span, or null when neither bound is set. */
@Composable
internal fun LibraryFilterState.playedTimeLabel(): String? {
    val min = minPlayedTime
    val max = maxPlayedTime
    return when {
        min != null && max != null -> stringResource(Res.string.gamelist__hours_range, min, max)
        min != null -> stringResource(Res.string.gamelist__hours_range_open, min)
        max != null -> stringResource(Res.string.gamelist__hours_range_up_to, max)
        else -> null
    }
}

/** The filter sheet sections of the library scope: the library sort, then the entry filters. */
@Composable
internal fun LibraryFilterSections(filter: LibraryFilterState, onFilterChange: (LibraryFilterState) -> Unit) {
    LibrarySortSection(filter, onFilterChange)

    FilterCard {
        FilterSectionHeader(icon = Icons.CheckCircleW500Rounded, title = Res.string.library__completion_status)
        SingleSelectConnectedButtonGroup(
            options = CompletionStatus.entries,
            selected = filter.completionStatus,
            onSelectedChange = { onFilterChange(filter.copy(completionStatus = it)) },
            toggleButtonText = { stringResource(it.displayName) }
        )
    }

    FilterCard {
        FilterSectionHeader(icon = Icons.Inventory2W500Rounded, title = Res.string.library__owned)
        SingleSelectConnectedButtonGroup(
            options = listOf(true, false),
            selected = filter.owned,
            onSelectedChange = { onFilterChange(filter.copy(owned = it)) },
            toggleButtonText = { stringResource(it.ownedLabel) }
        )
    }

    RangeFilterSection(
        icon = Icons.StarW500Rounded,
        title = Res.string.gamelist__my_rating,
        trailingText = { "★ ${it.start.toInt()} – ${it.endInclusive.toInt()}" },
        value = filter.minRating..filter.maxRating,
        valueRange = 0f..MaxEntryRating,
        steps = EntryRatingSteps,
        onValueChangeFinished = { onFilterChange(filter.copy(minRating = it.start, maxRating = it.endInclusive)) }
    )

    PlayedTimeSection(filter, onFilterChange)

    DateSpanSection(
        icon = Icons.CalendarMonthW500Rounded,
        title = Res.string.library__start_date,
        span = filter.startDate,
        onSpanChange = { onFilterChange(filter.copy(startDate = it)) }
    )
    DateSpanSection(
        icon = Icons.DateRangeW500Rounded,
        title = Res.string.library__end_date,
        span = filter.endDate,
        onSpanChange = { onFilterChange(filter.copy(endDate = it)) }
    )
}

@Composable
private fun LibrarySortSection(filter: LibraryFilterState, onFilterChange: (LibraryFilterState) -> Unit) = FilterCard {
    FilterSectionHeader(icon = Icons.SortW500Rounded, title = Res.string.gamelist__sort_field) {
        SortDirectionButton(
            direction = filter.sortDirection,
            enabled = true,
            onDirectionChange = { onFilterChange(filter.copy(sortDirection = it)) }
        )
    }
    SingleSelectConnectedButtonGroup(
        options = LibrarySortField.entries,
        selected = filter.sortField,
        onSelectedChange = { onFilterChange(filter.copy(sortField = it ?: LibrarySortField.UPDATED_AT)) },
        toggleButtonText = { stringResource(it.label) },
        deselectable = false
    )
}

/** Two open-ended fields rather than a slider: play time has no natural upper bound. */
@Composable
private fun PlayedTimeSection(filter: LibraryFilterState, onFilterChange: (LibraryFilterState) -> Unit) = FilterCard {
    FilterSectionHeader(icon = Icons.HourglassW500Rounded, title = Res.string.library__played_time)
    Row(horizontalArrangement = Arrangement.spacedBy(FieldGap)) {
        HoursField(
            label = stringResource(Res.string.gamelist__min_hours),
            value = filter.minPlayedTime,
            onValueChange = { onFilterChange(filter.copy(minPlayedTime = it)) },
            modifier = Modifier.weight(1f)
        )
        HoursField(
            label = stringResource(Res.string.gamelist__max_hours),
            value = filter.maxPlayedTime,
            onValueChange = { onFilterChange(filter.copy(maxPlayedTime = it)) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * A whole number of hours; empty means no bound. Applied on the keyboard's Done or when the field
 * loses focus rather than per keystroke, since every change reloads the list.
 */
@Composable
private fun HoursField(label: String, value: Int?, onValueChange: (Int?) -> Unit, modifier: Modifier) {
    var text by remember(value) { mutableStateOf(value?.toString().orEmpty()) }
    val commit = { text.toIntOrNull().let { if (it != value) onValueChange(it) } }
    OutlinedTextField(
        value = text,
        onValueChange = { input -> if (input.length <= MaxHoursDigits && input.all(Char::isDigit)) text = input },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        modifier = modifier.onFocusChanged { if (!it.isFocused) commit() }
    )
}

/** A span of days picked in a date range picker; the header's close button clears it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSpanSection(
    icon: ImageVector,
    title: StringResource,
    span: DateSpan?,
    onSpanChange: (DateSpan?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    FilterCard {
        FilterSectionHeader(icon = icon, title = title) {
            if (span != null) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { onSpanChange(null) }) {
                    Icon(
                        Icons.CloseW500Rounded,
                        contentDescription = stringResource(Res.string.gamelist__remove_filter, stringResource(title))
                    )
                }
            }
        }
        FilledTonalButton(
            shapes = ButtonDefaults.shapes(),
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.DateRangeW500Rounded,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(span?.label() ?: stringResource(Res.string.gamelist__date_any))
        }
    }

    if (showPicker) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = span?.from?.toPickerMillis(),
            initialSelectedEndDateMillis = span?.to?.toPickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = {
                        val from = state.selectedStartDateMillis?.let(::pickerMillisToDate)
                        val to = state.selectedEndDateMillis?.let(::pickerMillisToDate)
                        onSpanChange(if (from == null && to == null) null else DateSpan(from, to))
                        showPicker = false
                    }
                ) {
                    Text(stringResource(Res.string.common_ok))
                }
            }
        ) {
            DateRangePicker(state = state, modifier = Modifier.weight(1f))
        }
    }
}
