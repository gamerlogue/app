package it.maicol07.gamerlogue.core

import at.released.igdbclient.error.IgdbException
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.runCatching
import it.maicol07.spraypaintkt.JsonApiException
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> ExceptionReporter.safeRequest(request: suspend () -> T) = runCatching {
    request()
}.also {
    val error = it.getError() ?: run {
        requestDismiss()
        return@also
    }
    // Client libraries may wrap cancellation, but it must still escape instead of being reported.
    generateSequence(error) { it.cause }
        .filterIsInstance<CancellationException>()
        .firstOrNull()
        ?.let { throw it }
    when (error) {
        is IgdbException, is JsonApiException -> report(error)
    }
    error.printStackTrace()
}
