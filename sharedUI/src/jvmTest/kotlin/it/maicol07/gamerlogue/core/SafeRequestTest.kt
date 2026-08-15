package it.maicol07.gamerlogue.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException

class SafeRequestTest : StringSpec({
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
