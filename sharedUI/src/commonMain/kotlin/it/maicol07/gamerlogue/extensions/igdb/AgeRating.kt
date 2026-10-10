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

fun AgeRating.formattedCoverUrl(): String? {
    val url = rating_cover_url
    if (url.isBlank()) return null
    val fullUrl = if (url.startsWith("//")) "https:$url" else if (!url.startsWith("http")) "https://$url" else url
    return fullUrl.replace("t_thumb", "t_rating_cover").replace("t_micro", "t_rating_cover")
}
