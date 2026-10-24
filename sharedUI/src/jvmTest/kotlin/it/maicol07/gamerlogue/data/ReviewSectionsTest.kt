package it.maicol07.gamerlogue.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReviewSectionsTest : StringSpec({
    "rating_details values in another shape are skipped" {
        val entry = LibraryEntry().apply {
            ratingDetails = mapOf(
                "gameplay" to mapOf("rating" to 80, "review" to "Tight"),
                "Soundtrack" to mapOf("review" to "Great"),
                "legacy" to 7
            )
        }

        entry.reviewSections shouldBe listOf(
            ReviewSection("gameplay", 80.0, "Tight"),
            ReviewSection("Soundtrack", null, "Great")
        )
    }

    "an empty rating_details sent as a JSON array yields no sections" {
        LibraryEntry().apply { ratingDetails = emptyList<Any>() }.reviewSections shouldBe emptyList()
    }
})
