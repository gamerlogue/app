package it.maicol07.gamerlogue.services

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Minimal Ubisoft Connect API client, run from Kotlin so it isn't subject to browser CORS (mirrors
 * [PsnApi]/[XboxApi]/[EpicApi]). The credential is the `ticket`/`sessionId` already held by the
 * logged-in `connect.ubisoft.com` web session (the connector's credential step reads it from the page,
 * no separate token exchange like Epic/PSN needs) — `public-ubiservices.ubi.com`/`api-ubiservices.ubi.com`
 * are different origins so a same-origin `fetch` there would be CORS-blocked.
 *
 * Ownership comes from the REST entitlements endpoint the current Ubisoft Connect client itself uses,
 * not the older Club GraphQL "AllGames" query — Ubisoft's gateway now rejects that query outright for
 * third-party app ids (403 "not currently available for Application ... on the Gateway public
 * entrypoint"), as documented by the actively-maintained `galaxy-integration-uplay` fork after they hit
 * the same wall. Entitlements only carry ids, so names/platforms are resolved in batches via a scoped
 * GraphQL query keyed by `spaceIds` (distinct from the blocked one) — games without a PC platform are
 * dropped, since this connector only ever represents the PC (Ubisoft Connect launcher) library. Ubisoft
 * has no `external_game_source` in IGDB's (deprecated but still-used) fixed enum, so games are matched
 * by name, like the Nintendo connector.
 */
class UbisoftApi(private val http: HttpClient) {

    /** The session credential captured by [it.maicol07.gamerlogue.services.connectors.UbisoftConnector]. */
    data class Session(val ticket: String, val sessionId: String)

    /** Owned PC games: entitlements give ownership + ids, a batched GraphQL query resolves name/platform. */
    suspend fun ownedGames(session: Session): List<ExternalGameRef> {
        // The captured ticket may still be scoped to whatever appId last used it in the browser, not
        // ours — the entitlement API 401s on a mismatch. Renewing it under [AppId] first (same call the
        // Connect client itself makes on an appId switch) makes the rest of this deterministic instead of
        // depending on the page's own in-flight renewal having finished by the time we read localStorage.
        val renewed = renewSession(session) ?: session
        val spaceIds = ownedSpaceIds(renewed)
        if (spaceIds.isEmpty()) return emptyList()
        return spaceIds.chunked(SPACE_ID_BATCH_SIZE).flatMap { batch -> gameDetails(renewed, batch) }
    }

    /** Exchange [session]'s ticket for one freshly minted under [AppId], or null if the renewal fails. */
    private suspend fun renewSession(session: Session): Session? {
        val resp = http.put(SESSIONS) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Ubi_v1 t=${session.ticket}")
            header("Ubi-AppId", AppId)
            setBody("{}")
        }
        val body = resp.bodyAsText()
        val obj = runCatching { apiJson.parseToJsonElement(body).jsonObject }.getOrNull()
        val ticket = obj?.string("ticket")
        val sessionId = obj?.string("sessionId")
        if (ticket == null || sessionId == null) {
            Logger.w { "Ubisoft session renewal: unexpected response, status=${resp.status} body=${body.take(500)}" }
            return null
        }
        return Session(ticket, sessionId)
    }

    /** Distinct space ids of owned, non-expired game entitlements. */
    private suspend fun ownedSpaceIds(session: Session): List<String> {
        val resp = http.get(ENTITLEMENTS) {
            header(HttpHeaders.Authorization, "Ubi_v1 t=${session.ticket}")
            header("Ubi-AppId", AppId)
            header("Ubi-SessionId", session.sessionId)
            header("Ubi-LocaleCode", "en-US")
        }
        val body = resp.bodyAsText()
        val root = runCatching { apiJson.parseToJsonElement(body).jsonObject }.getOrNull()
        val entitlements = root?.get("entitlements")?.let { it.jsonArrayOrNodes() }
        if (entitlements == null) {
            Logger.w { "Ubisoft entitlements: unexpected response, status=${resp.status} body=${body.take(500)}" }
            return emptyList()
        }
        return entitlements.mapNotNull { it as? JsonObject }
            .filter { entitlement ->
                entitlement.string("accessLevel").equalsIgnoreCase("owned") &&
                    entitlement.string("type").equalsIgnoreCase("game") &&
                    !entitlement.string("availability").equalsIgnoreCase("expired")
            }
            .mapNotNull { it.string("spaceId")?.takeIf(String::isNotBlank) }
            .distinct()
    }

    /** Name + PC-platform check for one batch of [spaceIds] via the (still-open) scoped GraphQL query. */
    private suspend fun gameDetails(session: Session, spaceIds: List<String>): List<ExternalGameRef> {
        val payload = buildJsonObject {
            put("operationName", "GetOwnedGames")
            put(
                "variables",
                buildJsonObject {
                    put("spaceIds", buildJsonArray { spaceIds.forEach { add(it) } })
                },
            )
            put("query", GET_OWNED_GAMES_QUERY)
        }
        val resp = http.post(GRAPHQL) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Ubi_v1 t=${session.ticket}")
            header("Ubi-AppId", AppId)
            header("Ubi-SessionId", session.sessionId)
            setBody(payload.toString())
        }
        val body = resp.bodyAsText()
        val games = runCatching { apiJson.parseToJsonElement(body).jsonObject }.getOrNull()
            ?.get("data")?.jsonObject?.get("games")?.jsonArray
        if (games == null) {
            Logger.w { "Ubisoft game details: unexpected response, status=${resp.status} body=${body.take(500)}" }
            return emptyList()
        }
        return games.mapNotNull { node ->
            val obj = node as? JsonObject ?: return@mapNotNull null
            if (!containsPcPlatform(obj)) return@mapNotNull null
            val spaceId = obj.string("spaceId") ?: return@mapNotNull null
            val name = obj.string("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ExternalGameRef(spaceId, name)
        }
    }

    /** True if a `"type": "PC"` field appears anywhere in this platform-shaped subtree. */
    private fun containsPcPlatform(element: JsonElement): Boolean = when (element) {
        is JsonObject -> element["type"]?.jsonPrimitive?.content.equalsIgnoreCase("pc") ||
            element.values.any(::containsPcPlatform)
        is JsonArray -> element.any(::containsPcPlatform)
        else -> false
    }

    private fun String?.equalsIgnoreCase(other: String) = this?.equals(other, ignoreCase = true) == true

    /** Some Ubisoft endpoints wrap a list as a bare array, others as `{ "nodes": [...] }`. */
    private fun JsonElement.jsonArrayOrNodes() = when (this) {
        is JsonArray -> this
        is JsonObject -> this["nodes"]?.jsonArray
        else -> null
    }

    private companion object {
        // Fixed Ubisoft Connect client app id (matches the actively-maintained galaxy-integration-uplay
        // fork's current value — the older Club-only app id is now rejected by Ubisoft's gateway).
        const val AppId = "f68a4bb5-608a-4ff2-8123-be8ef797e0a6"
        const val ENTITLEMENTS = "https://api-ubiservices.ubi.com/v1/profiles/me/global/ubiconnect/entitlement/api/entitlements"
        const val GRAPHQL = "https://public-ubiservices.ubi.com/v1/profiles/me/uplay/graphql"
        const val SESSIONS = "https://public-ubiservices.ubi.com/v3/profiles/sessions"
        const val SPACE_ID_BATCH_SIZE = 50

        val GET_OWNED_GAMES_QUERY = """
            query GetOwnedGames(${'$'}spaceIds: [String!]) {
                games(spaceIds: ${'$'}spaceIds) {
                    id
                    spaceId
                    name
                    platform { ...PlatformFragment }
                    availablePlatformGroups { ...PlatformFragment }
                    availablePlatforms { nodes { ...PlatformFragment } }
                }
            }
            fragment PlatformFragment on Platform {
                id
                name
                type
                applicationId
            }
        """.trimIndent()
    }
}
