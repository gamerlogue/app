package it.maicol07.gamerlogue.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

/**
 * The globally reported request failures and the state of the sheet showing them.
 *
 * Reported from ViewModels and services, so the state is a [StateFlow] the UI collects rather than
 * Compose state a non-composable would have to write into.
 *
 * One nullable value instead of a list plus independent flags: the sheet cannot be open without an
 * error to show, and a dismissal cannot be pending without a sheet, so neither combination is
 * representable. The state is null rather than holding an empty list, which keeps `state != null`
 * the single test for "there is something to show".
 */
@Single
class ExceptionReporter {
    /** @param sheetOpen false when the errors are only reachable from the top bar indicator. */
    data class ErrorState(
        val errors: List<Throwable>,
        val sheetOpen: Boolean = true,
    )

    val state: StateFlow<ErrorState?>
        field = MutableStateFlow<ErrorState?>(null)

    /**
     * Appends to the errors already on screen. A failing request that retries would otherwise erase
     * the failure the user is reading; only the first report opens the sheet, so later ones do not
     * reopen a sheet the user has closed.
     */
    fun report(t: Throwable) = state.update { current ->
        ErrorState(
            errors = (current?.errors.orEmpty() + t).takeLast(MaxTrackedErrors),
            sheetOpen = current?.sheetOpen ?: true,
        )
    }

    /** Reopens the sheet for the errors already reported; no-op when there are none. */
    fun show() = state.update { it?.copy(sheetOpen = true) }

    fun dismissSheet() = state.update { it?.copy(sheetOpen = false) }

    fun clearError() {
        state.value = null
    }
}

/** A repeating failure would grow the list without bound; the oldest entries are the least useful. */
private const val MaxTrackedErrors = 10
