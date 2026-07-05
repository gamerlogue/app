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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Minimal PSN API client (psn-api style), run from Kotlin so it isn't subject to browser CORS.
 *
 * The only browser step is grabbing the `npsso` cookie from a logged-in session (see [PsnConnector]).
 * Owned games come from **trophy titles** (every game played on any console — PS3/Vita/PS4/PS5,
 * incl. disc games — unlike the purchased GraphQL which is PS4/PS5 digital only). The HTTP client must
 * NOT follow redirects (the auth code is read from the authorize 302 `Location`).
 */
class PsnApi(private val http: HttpClient) {

    /** Exchange an [npsso] cookie for a bearer access token (authorize → code → token). */
    suspend fun accessToken(npsso: String): String {
        val authResp = http.get(AUTHORIZE) {
            parameter("access_type", "offline")
            parameter("client_id", CLIENT_ID)
            parameter("redirect_uri", REDIRECT)
            parameter("response_type", "code")
            parameter("scope", SCOPE)
            header(HttpHeaders.Cookie, "npsso=$npsso")
        }
        val location = authResp.headers[HttpHeaders.Location] ?: error("PSN authorize: no redirect (npsso invalid?)")
        val code = CODE_REGEX.find(location)?.groupValues?.get(1) ?: error("PSN authorize: no code")

        val tokenResp = http.post(TOKEN) {
            header(HttpHeaders.Authorization, "Basic $BASIC")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("grant_type", "authorization_code")
                        append("code", code)
                        append("redirect_uri", REDIRECT)
                        append("token_format", "jwt")
                    },
                ),
            )
        }
        return JSON.parseToJsonElement(tokenResp.bodyAsText()).jsonObject["access_token"]
            ?.jsonPrimitive?.content ?: error("PSN token: no access_token")
    }

    /** The signed-in user's profile (onlineId + avatar). PSN profiles aren't publicly web-linkable. */
    suspend fun profile(token: String): ServiceProfile? {
        // The profiles endpoint rejects "me" (400); it needs the numeric accountId from the account service first.
        val accountResp = http.get(MY_ACCOUNT) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val accountId = JSON.parseToJsonElement(accountResp.bodyAsText()).jsonObject["accountId"]
            ?.jsonPrimitive?.content ?: error("PSN account: no accountId")

        val resp = http.get("$PROFILE_BASE/$accountId/profiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val obj = JSON.parseToJsonElement(resp.bodyAsText()).jsonObject
        val onlineId = obj["onlineId"]?.jsonPrimitive?.content ?: return null
        val avatar = obj["avatars"]?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { it["size"]?.jsonPrimitive?.content == "xl" || it["size"]?.jsonPrimitive?.content == "l" }
            ?.get("url")?.jsonPrimitive?.content
            ?.replaceFirst("http://", "https://") // PSN returns cleartext avatar URLs, blocked on Android
        return ServiceProfile(username = onlineId, avatarUrl = avatar)
    }

    /** The user's games via trophy titles (paginated). Names drive the IGDB name-fallback match. */
    suspend fun ownedGames(token: String): List<ExternalGameRef> {
        val out = mutableListOf<ExternalGameRef>()
        var offset = 0
        repeat(MAX_PAGES) {
            val resp = http.get(TROPHY_TITLES) {
                parameter("limit", PAGE_SIZE)
                parameter("offset", offset)
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            val obj = JSON.parseToJsonElement(resp.bodyAsText()).jsonObject
            val titles = obj["trophyTitles"]?.jsonArray ?: return out
            titles.forEach { entry ->
                val o = entry.jsonObject
                val name = o["trophyTitleName"]?.jsonPrimitive?.content ?: return@forEach
                val uid = o["npCommunicationId"]?.jsonPrimitive?.content ?: name
                out.add(ExternalGameRef(uid, name))
            }
            val total = obj["totalItemCount"]?.jsonPrimitive?.intOrNull ?: out.size
            offset += titles.size
            if (titles.isEmpty() || offset >= total) return out
        }
        return out
    }

    private companion object {
        const val CLIENT_ID = "09515159-7237-4370-9b40-3806e67c0891"
        const val CLIENT_SECRET = "ucPjka5tntB2KqsP"
        const val REDIRECT = "com.scee.psxandroid.scecompcall://redirect"
        const val SCOPE = "psn:mobile.v2.core psn:clientapp"
        const val AUTHORIZE = "https://ca.account.sony.com/api/authz/v3/oauth/authorize"
        const val TOKEN = "https://ca.account.sony.com/api/authz/v3/oauth/token"
        const val TROPHY_TITLES = "https://m.np.playstation.com/api/trophy/v1/users/me/trophyTitles"
        const val MY_ACCOUNT = "https://dms.api.playstation.com/api/v1/devices/accounts/me"
        const val PROFILE_BASE = "https://m.np.playstation.com/api/userProfile/v1/internal/users"
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 50
        val CODE_REGEX = Regex("code=([^&]+)")
        val JSON = Json { ignoreUnknownKeys = true }

        @OptIn(ExperimentalEncodingApi::class)
        val BASIC = Base64.encode("$CLIENT_ID:$CLIENT_SECRET".encodeToByteArray())
    }
}
