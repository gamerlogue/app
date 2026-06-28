package it.maicol07.gamerlogue.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single
class ExceptionReporter {
    val exception: StateFlow<Throwable?>
        field = MutableStateFlow<Throwable?>(null)

    var sheetOpen by mutableStateOf(false)
        private set

    // Set when a successful retry should animate the sheet closed.
    var dismissRequested by mutableStateOf(false)
        private set

    fun report(t: Throwable) {
        exception.value = t; sheetOpen = true
    }

    fun show() {
        sheetOpen = true
    }

    fun dismissSheet() {
        sheetOpen = false
    }

    fun clearError() {
        exception.value = null
        sheetOpen = false
        dismissRequested = false
    }

    // Triggers animated close if the sheet is open; no-op otherwise.
    fun requestDismiss() {
        if (sheetOpen) dismissRequested = true
    }
}
