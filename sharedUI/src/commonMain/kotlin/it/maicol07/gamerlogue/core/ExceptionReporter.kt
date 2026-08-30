package it.maicol07.gamerlogue.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

/**
 * The globally reported request failure and the state of the sheet showing it.
 *
 * Reported from ViewModels and services, so the state is a [StateFlow] the UI collects rather than
 * Compose state a non-composable would have to write into.
 *
 * One nullable value instead of three independent flags: the sheet cannot be open without an error to
 * show, and a dismissal cannot be pending without a sheet, so neither combination is representable.
 */
@Single
class ExceptionReporter {
    /** @param sheetOpen false when the error is only reachable from the top bar indicator. */
    data class ErrorState(
        val error: Throwable,
        val sheetOpen: Boolean = true,
    )

    val state: StateFlow<ErrorState?>
        field = MutableStateFlow<ErrorState?>(null)

    fun report(t: Throwable) {
        state.value = ErrorState(t)
    }

    /** Reopens the sheet for the error already reported; no-op when there is none. */
    fun show() = state.update { it?.copy(sheetOpen = true) }

    fun dismissSheet() = state.update { it?.copy(sheetOpen = false) }

    fun clearError() {
        state.value = null
    }

}
