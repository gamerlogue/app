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
    // runCatching catches Throwable, cancellation included. Swallowing it would let a cancelled caller
    // keep running (and write stale state), so it goes back out instead of being reported as a failure.
    if (error is CancellationException) throw error
    when (error) {
        is IgdbException, is JsonApiException -> report(error)
    }
    error.printStackTrace()
}
