package it.maicol07.gamerlogue

import at.released.igdbclient.error.IgdbException
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.unwrapError
import it.maicol07.gamerlogue.ui.components.layout.AppUi
import it.maicol07.spraypaintkt.JsonApiException
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> safeRequest(request: suspend () -> T) = runCatching {
    request()
}.apply {
    if (isErr) {
        val error = unwrapError()
        // Do not report cancellation exceptions
        if (error.cause !is CancellationException) {
            when (error) {
                is IgdbException, is JsonApiException -> AppUi.reportNetworkException(error)
            }
        }
        error.printStackTrace()
    }
}
