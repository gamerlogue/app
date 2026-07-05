package it.maicol07.gamerlogue.services

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Minimal Xbox Live API client, run from Kotlin so it isn't subject to browser CORS — mirrors [PsnApi].
 *
 * The only browser step is grabbing the MSA `access_token` from a logged-in session (see [XboxConnector]).
 * From it we run the standard Xbox Live auth chain — user token → XSTS token (+ userhash/xuid) — then list
 * the user's games from the **titlehub title history** (every title launched on the account). Names drive
 * the IGDB name-fallback match (`external_game_source=11`).
 */
class XboxApi(private val http: HttpClient) {

    /** Run the full chain (access token → user token → XSTS → titlehub) and return the owned titles. */
    suspend fun ownedGames(accessToken: String): List<ExternalGameRef> {
        val userToken = userToken(accessToken)
        val (uhs, xsts, xuid) = xstsToken(userToken)
        return titleHistory(uhs, xsts, xuid)
    }

    /** The signed-in user's profile (gamertag + avatar) via the same user→XSTS chain as [ownedGames]. */
    suspend fun profile(accessToken: String): ServiceProfile? {
        val userToken = userToken(accessToken)
        val (uhs, xsts, _) = xstsToken(userToken)
        val resp = http.get(PROFILE) {
            header(HttpHeaders.Authorization, "XBL3.0 x=$uhs;$xsts")
            header("x-xbl-contract-version", "2")
        }
        val settings = JSON.parseToJsonElement(resp.bodyAsText()).jsonObject["profileUsers"]?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("settings")?.jsonArray ?: return null
        val byId = settings.associate {
            it.jsonObject["id"]?.jsonPrimitive?.content to it.jsonObject["value"]?.jsonPrimitive?.contentOrNull
        }
        val gamertag = byId["Gamertag"] ?: return null
        return ServiceProfile(
            username = gamertag,
            avatarUrl = byId["GameDisplayPicRaw"],
            profileUrl = "https://account.xbox.com/en-us/profile?gamertag=$gamertag",
        )
    }

    /** Exchange the MSA [accessToken] for an Xbox Live user token. */
    private suspend fun userToken(accessToken: String): String {
        // ponytail: RPS RpsTicket is the raw MBI_SSL token. If a token from the modern OAuth (d=) flow is
        // used instead, this needs the "d=$accessToken" prefix — tune live if auth fails here.
        val body = buildJsonObject {
            putJsonObject("Properties") {
                put("AuthMethod", "RPS")
                put("SiteName", "user.auth.xboxlive.com")
                put("RpsTicket", accessToken)
            }
            put("RelyingParty", "http://auth.xboxlive.com")
            put("TokenType", "JWT")
        }
        val resp = http.post(USER_AUTH) {
            contentType(ContentType.Application.Json)
            header("x-xbl-contract-version", "1")
            setBody(JSON.encodeToString(JsonObject.serializer(), body))
        }
        return JSON.parseToJsonElement(resp.bodyAsText()).jsonObject["Token"]
            ?.jsonPrimitive?.content ?: error("Xbox user token: no Token (access_token invalid?)")
    }

    /** Exchange the [userToken] for an XSTS token, returning (userhash, xstsToken, xuid). */
    private suspend fun xstsToken(userToken: String): Triple<String, String, String> {
        val body = buildJsonObject {
            putJsonObject("Properties") {
                put("SandboxId", "RETAIL")
                putJsonArray("UserTokens") { add(userToken) }
            }
            put("RelyingParty", "http://xboxlive.com")
            put("TokenType", "JWT")
        }
        val resp = http.post(XSTS_AUTH) {
            contentType(ContentType.Application.Json)
            header("x-xbl-contract-version", "1")
            setBody(JSON.encodeToString(JsonObject.serializer(), body))
        }
        val obj = JSON.parseToJsonElement(resp.bodyAsText()).jsonObject
        val token = obj["Token"]?.jsonPrimitive?.content ?: error("Xbox XSTS: no Token")
        val xui = obj["DisplayClaims"]?.jsonObject?.get("xui")?.jsonArray?.firstOrNull()?.jsonObject
            ?: error("Xbox XSTS: no DisplayClaims")
        val uhs = xui["uhs"]?.jsonPrimitive?.content ?: error("Xbox XSTS: no uhs")
        val xuid = xui["xid"]?.jsonPrimitive?.content ?: error("Xbox XSTS: no xid")
        return Triple(uhs, token, xuid)
    }

    /** The user's title history (every game launched on the account). */
    private suspend fun titleHistory(uhs: String, xsts: String, xuid: String): List<ExternalGameRef> {
        val resp = http.get(titleHistoryUrl(xuid)) {
            header(HttpHeaders.Authorization, "XBL3.0 x=$uhs;$xsts")
            header("x-xbl-contract-version", "2")
            header(HttpHeaders.AcceptLanguage, "en-US")
        }
        val titles = JSON.parseToJsonElement(resp.bodyAsText()).jsonObject["titles"]?.jsonArray
            ?: return emptyList()
        return titles.mapNotNull { entry ->
            val o = entry.jsonObject
            // titleHistory spans every title the account ever launched, incl. non-Microsoft ones. Keep
            // only store-packaged titles: those carry a Package Family Name (`pfn`); the rest don't.
            val pfn = o["pfn"]?.jsonPrimitive?.contentOrNull
            if (pfn.isNullOrBlank()) return@mapNotNull null
            val name = o["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val uid = o["titleId"]?.jsonPrimitive?.content ?: name
            ExternalGameRef(uid, name)
        }
    }

    private companion object {
        const val USER_AUTH = "https://user.auth.xboxlive.com/user/authenticate"
        const val XSTS_AUTH = "https://xsts.auth.xboxlive.com/xsts/authorize"
        const val PROFILE = "https://profile.xboxlive.com/users/me/profile/settings" +
            "?settings=Gamertag,GameDisplayPicRaw"
        fun titleHistoryUrl(xuid: String) =
            "https://titlehub.xboxlive.com/users/xuid($xuid)/titles/titleHistory/decoration/scid"
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
