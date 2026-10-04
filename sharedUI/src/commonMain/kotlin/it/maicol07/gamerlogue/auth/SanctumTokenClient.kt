package it.maicol07.gamerlogue.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Calls the native Sanctum token endpoints without managing local session state. */
internal class SanctumTokenClient(private val httpClient: HttpClient, private val serverUrl: () -> String) {
    suspend fun exchange(code: String, verifier: String): TokenResponse = httpClient.submitForm(
        url = "${serverUrl()}/api/sanctum/token/exchange",
        formParameters = parameters {
            append("code", code)
            append("code_verifier", verifier)
        },
    ).body()

    suspend fun refresh(refreshToken: String): TokenResponse {
        val response = httpClient.post("${serverUrl()}/api/sanctum/token/refresh") {
            expectSuccess = false
            setBody(FormDataContent(parameters { append("refresh_token", refreshToken) }))
        }
        if (response.status == HttpStatusCode.OK) return response.body()
        if (response.status == HttpStatusCode.TooManyRequests) {
            throw TokenEndpointException(response.status, response.bodyAsText())
        }

        val error = runCatching { response.body<TokenErrorResponse>().error }.getOrNull()
        throw AuthenticationLostException(error, response.status)
    }

    suspend fun revoke(refreshToken: String) {
        val response = httpClient.submitForm(
            url = "${serverUrl()}/api/sanctum/token/revoke",
            formParameters = parameters { append("refresh_token", refreshToken) },
        ) { expectSuccess = false }
        if (response.status.value !in SUCCESS_STATUS_CODES) {
            throw TokenEndpointException(response.status, response.bodyAsText())
        }
    }
}

@Serializable
internal data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user_id") val userId: String,
    @SerialName("expires_at") val accessExpiresAt: String,
    @SerialName("refresh_expires_at") val refreshExpiresAt: String,
)

@Serializable
private data class TokenErrorResponse(val error: String)

internal class NotAuthenticatedException : IllegalStateException("No refreshable authentication session")

internal class AuthenticationLostException(error: String?, status: HttpStatusCode, cause: Throwable? = null) :
    IllegalStateException("Authentication lost: status=${status.value}, error=${error ?: "unknown"}", cause)

internal class TokenEndpointException(status: HttpStatusCode, body: String) :
    IllegalStateException("Token endpoint failed: status=${status.value}, body=$body")

private val SUCCESS_STATUS_CODES = HttpStatusCode.OK.value until HttpStatusCode.MultipleChoices.value
