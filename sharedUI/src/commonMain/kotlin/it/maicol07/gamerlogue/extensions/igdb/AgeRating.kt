package it.maicol07.gamerlogue.extensions.igdb

import at.released.igdbclient.model.AgeRating

/** "<organization> <rating>" (e.g. "PEGI 18"), or whichever half IGDB provides; null when both are missing. */
fun AgeRating.displayTitle(): String? {
    val orgName = organization?.name?.replace("_", " ")?.trim()?.ifEmpty { null }
    val ratName = rating_category?.rating?.trim()?.ifEmpty { null }
    return when {
        orgName != null && ratName != null ->
            if (ratName.startsWith(orgName, ignoreCase = true)) ratName else "$orgName $ratName"
        else -> ratName ?: orgName
    }
}

/** The rating's own cover, else the badge igdb.com shows for it: IGDB leaves `rating_cover_url` empty for most ratings. */
fun AgeRating.formattedCoverUrl(): String? {
    val url = rating_cover_url
    if (url.isBlank()) return igdbRatingIconUrl()
    val fullUrl = if (url.startsWith("//")) "https:$url" else if (!url.startsWith("http")) "https://$url" else url
    return fullUrl.replace("t_thumb", "t_rating_cover").replace("t_micro", "t_rating_cover")
}

/**
 * ponytail: igdb.com's own, undocumented badge path (`/icons/rating_icons/<org>/<org>_<rating>.png`),
 * checked against all 40 IGDB rating categories; only GRAC 18+ is filed as `19`. If igdb.com moves
 * the badges, bundle them instead.
 */
private fun AgeRating.igdbRatingIconUrl(): String? {
    val org = organization?.name?.ratingIconSlug() ?: return null
    val rating = rating_category?.rating?.ratingIconSlug() ?: return null
    val file = if (org == "grac" && rating == "18") "19" else rating
    return "https://www.igdb.com/icons/rating_icons/$org/${org}_$file.png"
}

private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")

private fun String.ratingIconSlug(): String? =
    lowercase().replace("+", "").replace(NON_ALPHANUMERIC, "_").trim('_').ifEmpty { null }
