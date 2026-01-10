package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.concurrent.Volatile

class AppUiState {
    var networkException by mutableStateOf<Throwable?>(null)
    var showExceptionBottomSheet by mutableStateOf(true)
}

val LocalAppUiState = staticCompositionLocalOf<AppUiState> {
    error("LocalAppUiState not provided")
}

@Composable
fun rememberAppUiState(): AppUiState = remember { AppUiState() }

object AppUi {
    @Volatile
    private var holder: AppUiState? = null

    fun attach(state: AppUiState) { holder = state }
    fun detach(state: AppUiState) { if (holder === state) holder = null }

    fun reportNetworkException(t: Throwable) {
        holder?.let { s ->
            s.networkException = t
            s.showExceptionBottomSheet = true
        }
    }

    val current: AppUiState? get() = holder
}

