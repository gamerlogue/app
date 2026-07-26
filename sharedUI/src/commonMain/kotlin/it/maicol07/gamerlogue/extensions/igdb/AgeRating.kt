package it.maicol07.gamerlogue.extensions.igdb

import at.released.igdbclient.model.AgeRating
import at.released.igdbclient.model.AgeRatingCategoryEnum
import at.released.igdbclient.model.AgeRatingRatingEnum

fun AgeRating.displayTitle(): String? {
    val catStr = category.name
    val ratStr = rating.name

    val catName = when {
        catStr.contains("UNSPECIFIED", ignoreCase = true) || catStr.contains("NULL", ignoreCase = true) || catStr.isBlank() -> null
        category == AgeRatingCategoryEnum.ESRB -> "ESRB"
        category == AgeRatingCategoryEnum.PEGI -> "PEGI"
        category == AgeRatingCategoryEnum.CERO -> "CERO"
        category == AgeRatingCategoryEnum.USK -> "USK"
        category == AgeRatingCategoryEnum.GRAC -> "GRAC"
        category == AgeRatingCategoryEnum.CLASS_IND -> "CLASS IND"
        category == AgeRatingCategoryEnum.ACB -> "ACB"
        else -> catStr.replace("AGE_RATING_CATEGORY_", "").replace("_", " ").trim()
    }

    val ratName = when {
        ratStr.contains("UNSPECIFIED", ignoreCase = true) || ratStr.contains("NULL", ignoreCase = true) || ratStr.isBlank() -> null
        rating == AgeRatingRatingEnum.THREE -> "3"
        rating == AgeRatingRatingEnum.SEVEN -> "7"
        rating == AgeRatingRatingEnum.TWELVE -> "12"
        rating == AgeRatingRatingEnum.SIXTEEN -> "16"
        rating == AgeRatingRatingEnum.EIGHTEEN -> "18"
        rating == AgeRatingRatingEnum.RP -> "Rating Pending"
        rating == AgeRatingRatingEnum.EC -> "Early Childhood"
        rating == AgeRatingRatingEnum.E -> "Everyone"
        rating == AgeRatingRatingEnum.E10 -> "Everyone 10+"
        rating == AgeRatingRatingEnum.T -> "Teen"
        rating == AgeRatingRatingEnum.M -> "Mature 17+"
        rating == AgeRatingRatingEnum.AO -> "Adults Only 18+"
        rating == AgeRatingRatingEnum.CERO_A -> "A"
        rating == AgeRatingRatingEnum.CERO_B -> "B"
        rating == AgeRatingRatingEnum.CERO_C -> "C"
        rating == AgeRatingRatingEnum.CERO_D -> "D"
        rating == AgeRatingRatingEnum.CERO_Z -> "Z"
        rating == AgeRatingRatingEnum.USK_0 -> "0"
        rating == AgeRatingRatingEnum.USK_6 -> "6"
        rating == AgeRatingRatingEnum.USK_12 -> "12"
        rating == AgeRatingRatingEnum.USK_16 -> "16"
        rating == AgeRatingRatingEnum.USK_18 -> "18"
        else -> {
            val s = ratStr.replace("AGE_RATING_RATING_", "").replace("_", " ").trim()
            if (s.contains("UNSPECIFIED", ignoreCase = true) || s.contains("NULL", ignoreCase = true)) null else s
        }
    }

    val result = when {
        catName != null && ratName != null -> {
            if (ratName.startsWith(catName, ignoreCase = true)) ratName else "$catName $ratName"
        }
        ratName != null -> ratName
        catName != null -> catName
        else -> null
    }

    if (result == null || result.lowercase().contains("null") || result.lowercase().contains("unspecified")) {
        return null
    }
    return result
}

fun AgeRating.formattedCoverUrl(): String? {
    val url = rating_cover_url ?: return null
    if (url.isBlank()) return null
    val fullUrl = if (url.startsWith("//")) "https:$url" else if (!url.startsWith("http")) "https://$url" else url
    return fullUrl.replace("t_thumb", "t_rating_cover").replace("t_micro", "t_rating_cover")
}
