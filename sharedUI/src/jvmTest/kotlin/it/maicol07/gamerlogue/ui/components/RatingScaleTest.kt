package it.maicol07.gamerlogue.ui.components

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RatingScaleTest : StringSpec({
    "the scales convert the server rating" {
        RatingScale.TEN.format(75) shouldBe "7.5"
        RatingScale.HUNDRED.format(75) shouldBe "75"
    }
})
