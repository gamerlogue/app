package it.maicol07.gamerlogue.core

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.runCatching
import it.maicol07.gamerlogue.extensions.jsonApiErrorCode
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> ExceptionReporter.safeRequest(request: suspend () -> T) = safeRequest(emptySet(), request)

suspend fun <T> ExceptionReporter.safeRequest(expectedErrorCodes: Set<String>, request: suspend () -> T) =
    safeRequest({ it in expectedErrorCodes }, request)

/**
 * [safeRequest] for a call that can fail in an anticipated way the screen shows itself (another
 * user's private profile, a block, a rejected field): a failure whose JSON:API error code
 * [isExpected] accepts is returned without being reported to the global error sheet.
 */
suspend fun <T> ExceptionReporter.safeRequest(isExpected: (String) -> Boolean, request: suspend () -> T) = runCatching {
    request()
}.also {
    val error = it.getError() ?: return@also
    // Client libraries may wrap cancellation, but it must still escape instead of being reported.
    generateSequence(error) { it.cause }
        .filterIsInstance<CancellationException>()
        .firstOrNull()
        ?.let { throw it }
    if (error.jsonApiErrorCode?.let(isExpected) == true) return@also
    // Every remaining failure is surfaced, not just the two library exception types: a timeout or a
    // deserialization error is exactly the kind of thing that used to disappear here.
    Logger.e(error) { "Request failed" }
    report(error)
}
