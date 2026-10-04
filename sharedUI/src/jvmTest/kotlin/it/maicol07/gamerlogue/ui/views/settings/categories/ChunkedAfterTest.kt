package it.maicol07.gamerlogue.ui.views.settings.categories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ChunkedAfterTest : StringSpec({
    "closes a chunk after each match and keeps the trailing run" {
        listOf(1, 2, 3, 4, 5).chunkedAfter { it == 1 || it == 3 } shouldBe listOf(listOf(1), listOf(2, 3), listOf(4, 5))
    }

    "drops the empty chunk when the last element matches" {
        listOf(1, 2).chunkedAfter { it == 2 } shouldBe listOf(listOf(1, 2))
    }

    "keeps a single chunk without matches and nothing for an empty list" {
        listOf(1, 2).chunkedAfter { false } shouldBe listOf(listOf(1, 2))
        emptyList<Int>().chunkedAfter { true } shouldBe emptyList()
    }
})
