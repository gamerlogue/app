package it.maicol07.gamerlogue.core

import at.released.igdbclient.error.IgdbException
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.runCatching
import it.maicol07.spraypaintkt.JsonApiException
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> ExceptionReporter.safeRequest(request: suspend () -> T) = runCatching {
    request()
}.also {
    val error = it.getError() ?: return@also
    if (error.cause !is CancellationException) {
        when (error) {
            is IgdbException, is JsonApiException -> report(error)
        }
    }
    error.printStackTrace()
}
