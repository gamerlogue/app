package it.maicol07.gamerlogue.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException

class SafeRequestTest : StringSpec({
    "a successful request does not clear another request's error" {
        runTest {
            val reporter = ExceptionReporter()
            val failure = IllegalStateException("first request failed")
            val finishSuccess = CompletableDeferred<Unit>()
            val success = async {
                reporter.safeRequest {
                    finishSuccess.await()
                    "second request succeeded"
                }
            }

            reporter.safeRequest<Unit> { throw failure }
            finishSuccess.complete(Unit)
            success.await()

            reporter.state.value?.error shouldBe failure
        }
    }

    "rethrows cancellation from the cause chain" {
        runTest {
            val cancellation = CancellationException("navigation changed")

            val thrown = shouldThrow<CancellationException> {
                ExceptionReporter().safeRequest<Unit> {
                    throw IllegalStateException("request failed", cancellation)
                }
            }

            thrown shouldBe cancellation
        }
    }
})
