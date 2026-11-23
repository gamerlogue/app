package it.maicol07.gamerlogue.services

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Nintendo's public European eShop search (the one nintendo.com's own search uses), called off-WebView: the browser
 * can't, it sends no CORS headers. An eShop id is per region, so this only finds European ones.
 */
class NintendoApi(private val http: HttpClient) {

    /**
     * The European eShop id of the game called [name], searched in [language] (falling back to English): only a
     * title equal to it once case, spacing and symbols are set aside, so a near name never picks another game.
     */
    suspend fun europeanTitleId(name: String, language: String): String? {
        val lang = language.lowercase().takeIf { it in SEARCH_LANGUAGES } ?: "en"
        val body = http.get("https://searching.nintendo-europe.com/$lang/select") {
            parameter("q", name)
            parameter("fq", "type:GAME")
            parameter("rows", ROWS)
            parameter("wt", "json")
        }.bodyAsText()
        return parseTitleId(body, name)
    }

    private companion object {
        const val ROWS = 20
        val SEARCH_LANGUAGES = setOf("en", "it", "de", "fr", "es", "nl", "pt", "ru")
    }
}

/** The first eShop id of the search result in [body] whose title is [name], compared by [titleKey]; null for none. */
internal fun parseTitleId(body: String, name: String): String? {
    val key = titleKey(name)
    val docs = (Json.parseToJsonElement(body) as? JsonObject)?.get("response")?.jsonObject?.get("docs")?.jsonArray
    return docs.orEmpty()
        .map { it.jsonObject }
        .firstOrNull { titleKey(it["title"]?.jsonPrimitive?.content.orEmpty()) == key }
        ?.get("nsuid_txt")?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
}

/** A title as only its letters and digits, lowercased: "Core Keeper™" and "core keeper" are the same game. */
internal fun titleKey(title: String): String = title.filter(Char::isLetterOrDigit).lowercase()
