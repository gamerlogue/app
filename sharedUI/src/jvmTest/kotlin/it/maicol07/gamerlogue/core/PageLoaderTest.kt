package it.maicol07.gamerlogue.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.extensions.ResourcePage
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

private const val PageSize = 30

class PageLoaderTest : StringSpec({
    "a short page after a full one ends the list" {
        runTest {
            val pages = mutableListOf<Int>()
            val loader = pagedLoader { page ->
                pages += page
                val size = if (page == 1) PageSize else 5
                ResourcePage(List(size) { it }, total = 35)
            }

            loader.ensureLoaded()
            advanceUntilIdle()
            loader.state.value.endReached shouldBe false
            loader.loadMore()
            advanceUntilIdle()
            loader.loadMore()
            advanceUntilIdle()

            pages shouldBe listOf(1, 2)
            loader.state.value.items.size shouldBe 35
            loader.state.value.endReached shouldBe true
        }
    }

    "a failed page ends the list as failed instead of retrying" {
        runTest {
            var calls = 0
            val loader = pagedLoader {
                calls++
                null
            }

            loader.ensureLoaded()
            advanceUntilIdle()
            loader.loadMore()
            advanceUntilIdle()

            calls shouldBe 1
            loader.state.value.failed shouldBe true
            loader.state.value.isEmpty shouldBe false
        }
    }

    "ensureLoaded keeps what is loaded, refresh starts over" {
        runTest {
            var calls = 0
            val loader = pagedLoader {
                calls++
                ResourcePage(listOf(calls), total = 1)
            }

            loader.ensureLoaded()
            advanceUntilIdle()
            loader.ensureLoaded()
            advanceUntilIdle()
            calls shouldBe 1

            loader.refresh()
            advanceUntilIdle()
            calls shouldBe 2
            loader.state.value.items shouldBe listOf(2)
        }
    }

    "an empty collection reads as empty" {
        runTest {
            val loader = pagedLoader { ResourcePage(emptyList(), total = 0) }

            loader.ensureLoaded()
            advanceUntilIdle()

            loader.state.value.isEmpty shouldBe true
        }
    }
})

private fun TestScope.pagedLoader(fetch: suspend (Int) -> ResourcePage<Int>?) = PageLoader(this, fetch)
