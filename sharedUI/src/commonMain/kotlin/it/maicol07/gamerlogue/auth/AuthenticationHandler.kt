package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.URLBuilder
import io.ktor.http.parameters
import io.ktor.http.parseQueryString
import it.maicol07.gamerlogue.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.time.Instant

data class LoginProof(
    val verifier: String,
    val challenge: String,
    val state: String,
)

internal fun createLoginProof(
    verifierEntropy: ByteArray,
    stateEntropy: ByteArray,
    sha256: (ByteArray) -> ByteArray,
): LoginProof {
    val verifier = verifierEntropy.base64Url()
    return LoginProof(
        verifier = verifier,
        challenge = sha256(verifier.encodeToByteArray()).base64Url(),
        state = stateEntropy.base64Url(),
    )
}

private fun ByteArray.base64Url(): String =
    Base64.UrlSafe.encode(this).trimEnd('=')

abstract class AuthenticationHandler(
    protected val authProvider: AuthTokenProvider,
    private val authClient: HttpClient,
) {
    abstract fun login()
    abstract suspend fun handleCallback(query: String): Boolean
    open suspend fun restoreSession() = Unit

    protected fun callbackMatchesState(query: String, expectedState: String): Boolean =
        callbackParameter(query, "state") == expectedState

    protected suspend fun exchangeCallback(query: String, proof: LoginProof): Boolean {
        if (!callbackMatchesState(query, proof.state)) return false
        val code = callbackParameter(query, "code")?.takeIf { it.length == AuthorizationCodeLength }
            ?: return false
        val response = authClient.submitForm(
            url = "${BuildConfig.GAMERLOGUE_URL}/api/sanctum/token/exchange",
            formParameters = parameters {
                append("code", code)
                append("code_verifier", proof.verifier)
            },
        ).body<TokenExchangeResponse>()

        authProvider.updateCredentials(
            token = response.token,
            userId = response.userId,
            expiresAtEpochMillis = Instant.parse(response.expiresAt).toEpochMilliseconds(),
        )
        return true
    }

    protected fun getAuthUrl(redirectUri: String, proof: LoginProof): String =
        URLBuilder("${BuildConfig.GAMERLOGUE_URL}/sanctum/token").apply {
            parameters.append("token_name", "Gamerlogue")
            parameters.append("code_challenge", proof.challenge)
            parameters.append("code_challenge_method", "S256")
            parameters.append("state", proof.state)
            parameters.append("redirect_uri", redirectUri)
        }.buildString()

    private fun callbackParameter(query: String, name: String): String? =
        parseQueryString(query.substringAfter('?')).getAll(name)?.singleOrNull()

    @Serializable
    private data class TokenExchangeResponse(
        val token: String,
        @SerialName("user_id") val userId: String,
        @SerialName("expires_at") val expiresAt: String,
    )

    private companion object {
        const val AuthorizationCodeLength = 64
    }
}

@Composable
expect fun rememberAuthenticationHandler(): AuthenticationHandler

internal val LocalAuthenticationHandler = staticCompositionLocalOf<AuthenticationHandler> {
    error("AuthenticationHandler is not available")
}
