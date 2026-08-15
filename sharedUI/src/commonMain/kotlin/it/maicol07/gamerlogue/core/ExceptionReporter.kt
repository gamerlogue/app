package it.maicol07.gamerlogue.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

/**
 * The globally reported request failure and the state of the sheet showing it.
 *
 * Reported from ViewModels and services, so the state is a [StateFlow] the UI collects rather than
 * Compose state a non-composable would have to write into.
 */
@Single
class ExceptionReporter {
    val exception: StateFlow<Throwable?>
        field = MutableStateFlow<Throwable?>(null)

    val sheetOpen: StateFlow<Boolean>
        field = MutableStateFlow(false)

    /** Set when a successful retry should animate the sheet closed. */
    val dismissRequested: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun report(t: Throwable) {
        exception.value = t
        sheetOpen.value = true
    }

    fun show() {
        sheetOpen.value = true
    }

    fun dismissSheet() {
        sheetOpen.value = false
    }

    fun clearError() {
        exception.value = null
        sheetOpen.value = false
        dismissRequested.value = false
    }

    /** Triggers an animated close if the sheet is open; no-op otherwise. */
    fun requestDismiss() {
        if (sheetOpen.value) dismissRequested.value = true
    }
}
