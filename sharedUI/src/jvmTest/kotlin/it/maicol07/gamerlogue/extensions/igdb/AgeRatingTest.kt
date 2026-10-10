package it.maicol07.gamerlogue.extensions.igdb

import at.released.igdbclient.model.AgeRating
import at.released.igdbclient.model.AgeRatingCategory
import at.released.igdbclient.model.AgeRatingOrganization
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun rating(org: String, rating: String) =
    AgeRating(organization = AgeRatingOrganization(name = org), rating_category = AgeRatingCategory(rating = rating))

class AgeRatingTest : StringSpec({
    "ratings without a cover fall back to the igdb.com badge" {
        rating("ACB", "MA 15+").formattedCoverUrl() shouldBe "https://www.igdb.com/icons/rating_icons/acb/acb_ma_15.png"
        rating("CLASS_IND", "16").formattedCoverUrl() shouldBe
            "https://www.igdb.com/icons/rating_icons/class_ind/class_ind_16.png"
        rating("GRAC", "18+").formattedCoverUrl() shouldBe "https://www.igdb.com/icons/rating_icons/grac/grac_19.png"
        AgeRating().formattedCoverUrl() shouldBe null
    }
})
