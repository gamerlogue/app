package it.maicol07.gamerlogue.core

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.runCatching
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> ExceptionReporter.safeRequest(request: suspend () -> T) = runCatching {
    request()
}.also {
    val error = it.getError() ?: return@also
    // Client libraries may wrap cancellation, but it must still escape instead of being reported.
    generateSequence(error) { it.cause }
        .filterIsInstance<CancellationException>()
        .firstOrNull()
        ?.let { throw it }
    // Every remaining failure is surfaced, not just the two library exception types: a timeout or a
    // deserialization error is exactly the kind of thing that used to disappear here.
    Logger.e(error) { "Request failed" }
    report(error)
}
