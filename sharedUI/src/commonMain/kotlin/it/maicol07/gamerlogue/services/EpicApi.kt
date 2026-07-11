package it.maicol07.gamerlogue.services

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Minimal Epic Games launcher API client, run from Kotlin so it isn't subject to browser CORS —
 * mirrors [PsnApi]/[XboxApi]. The only browser step is grabbing a short-lived authorization code from
 * the logged-in web session (the connector's credential step hits `id/api/redirect`); everything after
 * (token exchange, library, catalog) is cross-origin and would be CORS-blocked in the WebView.
 *
 * Owned games come from the launcher's library-service; titles are resolved from the catalog-service
 * (library records carry only catalog ids, no names) and drive the IGDB name-fallback match. The
 * launcher client id/secret are the public "fortnitePCGameClient" credentials every Epic library tool
 * uses (legendary, Heroic, …) — required to exchange the code, not a user secret.
 */
class EpicApi(private val http: HttpClient) {

    /** A launcher token plus the identity Epic returns alongside it (used for the profile, no extra call). */
    data class Token(val accessToken: String, val accountId: String, val displayName: String)

    /** Exchange the web session's short-lived authorization [code] for a launcher token. */
    suspend fun token(code: String): Token {
        val resp = http.post(TOKEN) {
            header(HttpHeaders.Authorization, "Basic $BASIC")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("grant_type", "authorization_code")
                        append("code", code)
                        append("token_type", "eg1")
                    },
                ),
            )
        }
        val obj = apiJson.parseToJsonElement(resp.bodyAsText()).jsonObject
        return Token(
            accessToken = obj.requireString("access_token", "Epic token: no access_token (code expired/invalid?)"),
            accountId = obj.string("account_id").orEmpty(),
            displayName = obj.string("displayName").orEmpty(),
        )
    }

    /** The user's owned games: list the library (cursor-paginated), then resolve titles via the catalog. */
    suspend fun ownedGames(token: String): List<ExternalGameRef> {
        val records = libraryRecords(token)
        return records.groupBy({ it.first }, { it.second })
            .flatMap { (namespace, ids) -> catalogGames(token, namespace, ids.distinct()) }
    }

    /** Library records as (namespace, catalogItemId), cursor-paginated. Skips Unreal Engine assets. */
    private suspend fun libraryRecords(token: String): List<Pair<String, String>> {
        val records = mutableListOf<Pair<String, String>>()
        var cursor: String? = null
        var page = 0
        while (page < MAX_PAGES) {
            val resp = http.get(LIBRARY) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("includeMetadata", "false")
                cursor?.let { parameter("cursor", it) }
            }
            val obj = apiJson.parseToJsonElement(resp.bodyAsText()).jsonObject
            obj["records"]?.jsonArray?.forEach { entry ->
                val o = entry.jsonObject
                val ns = o.string("namespace")
                val id = o.string("catalogItemId")
                if (ns != null && ns != "ue" && id != null) records.add(ns to id)
            }
            cursor = obj["responseMetadata"]?.jsonObject?.string("nextCursor")
            if (cursor.isNullOrBlank()) break
            page++
        }
        return records
    }

    /** Resolve catalog titles for one namespace's item [ids] (batched), keeping games and their DLC. */
    private suspend fun catalogGames(token: String, namespace: String, ids: List<String>): List<ExternalGameRef> {
        val out = mutableListOf<ExternalGameRef>()
        ids.chunked(CATALOG_CHUNK).forEach { chunk ->
            val resp = http.get("$CATALOG/$namespace/bulk/items") {
                header(HttpHeaders.Authorization, "Bearer $token")
                chunk.forEach { parameter("id", it) }
                parameter("country", "US")
                parameter("locale", "en-US")
            }
            val obj = apiJson.parseToJsonElement(resp.bodyAsText()).jsonObject
            chunk.forEach { id ->
                val item = obj[id]?.jsonObject ?: return@forEach
                val title = item.string("title")
                if (title != null && isImportable(item)) out.add(ExternalGameRef(id, title))
            }
        }
        return out
    }

    /**
     * Keep games and their DLC/add-ons; drop pure software (the namespace filter already skips Unreal
     * assets). Epic tags most games with an `applications` category too, so "has applications" alone
     * doesn't mean software — only exclude items that are applications AND not games. DLC carry neither
     * category, so `!isSoftware` keeps them.
     */
    private fun isImportable(item: JsonObject): Boolean {
        val paths = item["categories"]?.jsonArray
            ?.mapNotNull { it.jsonObject.string("path") } ?: return true
        val isGame = paths.any { it == "games" || it.startsWith("games/") }
        val isApp = paths.any { path ->
            path == "applications" || path.startsWith("applications/") ||
                path == "software" || path.startsWith("software/")
        }
        return isGame || !isApp
    }

    private companion object {
        // Public "fortnitePCGameClient" launcher credentials (same as legendary/Heroic).
        const val CLIENT_ID = "34a02cf8f4414e29b15921876da36f9a"
        const val CLIENT_SECRET = "daafbccc737745039dffe53d94fc76cf"
        const val TOKEN = "https://account-public-service-prod.ol.epicgames.com/account/api/oauth/token"
        const val LIBRARY = "https://library-service.live.use1a.on.epicgames.com/library/api/public/items"
        const val CATALOG = "https://catalog-public-service-prod06.ol.epicgames.com/catalog/api/shared/namespace"
        const val MAX_PAGES = 50
        const val CATALOG_CHUNK = 40
        val BASIC = basicAuth(CLIENT_ID, CLIENT_SECRET)
    }
}
