package it.maicol07.gamerlogue

import at.released.igdbclient.error.IgdbException
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.unwrapError
import it.maicol07.gamerlogue.ui.components.layout.AppUi
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> safeRequest(request: suspend () -> T) = runCatching {
    request()
}.apply {
    if (isErr) {
        val error = unwrapError()
        when (error) {
            is IgdbException -> {
                // Do not report cancellation exceptions
                if (error.cause !is CancellationException) {
                    AppUi.reportNetworkException(error)
                }
            }
        }
        error.printStackTrace()
    }
}
