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
     * The European eShop id of the game called [name] (see [parseTitleId]). Searched in English whatever the account's
     * language: the ids are the same across Europe, and English titles are the ones IGDB names games by.
     */
    suspend fun europeanTitleId(name: String): String? {
        val body = http.get("https://searching.nintendo-europe.com/en/select") {
            parameter("q", name)
            parameter("fq", "type:GAME")
            parameter("rows", ROWS)
            parameter("wt", "json")
        }.bodyAsText()
        return parseTitleId(body, name)
    }

    private companion object {
        const val ROWS = 20
    }
}

/** The eShop id of the search result in [body] for [name] ([byTitle]). */
internal fun parseTitleId(body: String, name: String): String? {
    val docs = (Json.parseToJsonElement(body) as? JsonObject)?.get("response")?.jsonObject?.get("docs")?.jsonArray
    val doc = docs.orEmpty()
        .map { it.jsonObject }
        .map { it["title"]?.jsonPrimitive?.content.orEmpty() to it }
        .byTitle(name)
    return doc?.get("nsuid_txt")?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
}

/** The value of the (title, value) search result for [name]: the first titled like it by [titleKey], else the only one a
 *  typo away ([isNearTitle]); null for none, or for several near ones, so a guess never picks between games. */
internal fun <T> List<Pair<String, T>>.byTitle(name: String): T? =
    firstOrNull { titleKey(it.first) == titleKey(name) }?.second
        ?: filter { isNearTitle(it.first, name) }.singleOrNull()?.second

/** A title as only its letters and digits, lowercased: "Core Keeper™" and "core keeper" are the same game. */
internal fun titleKey(title: String): String = title.filter(Char::isLetterOrDigit).lowercase()

/**
 * Whether [a] and [b] differ only by a typo or an accent ("Pokemon" / "Pokémon"): as many words, the same numbers, and a
 * few letters apart at most. A word or number more is another game ("Xenoblade Chronicles X", "Hades II", "Game 3").
 * ponytail: a same-length sequel letter swap ("Game A" / "Game B") still passes; compare words one to one if it shows.
 */
internal fun isNearTitle(a: String, b: String): Boolean {
    val words = { s: String -> s.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter(String::isNotEmpty) }
    val digits = { s: String -> s.filter(Char::isDigit) }
    val key = titleKey(b)
    return words(a).size == words(b).size && digits(a) == digits(b) &&
        editDistance(titleKey(a), key) <= key.length / NEAR_TITLE_LETTERS_PER_TYPO
}

private const val NEAR_TITLE_LETTERS_PER_TYPO = 10
