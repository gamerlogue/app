package it.maicol07.gamerlogue.auth

import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import io.ktor.util.generateNonceBlocking
import okio.ByteString.Companion.encodeUtf8

/** The half of the PKCE proof that has to survive until the callback comes back. */
data class PendingLogin(
    val verifier: String,
    val state: String,
)

data class PkceLoginAttempt(
    val pending: PendingLogin,
    val challenge: String,
)

internal fun createPkceLoginAttempt(): PkceLoginAttempt {
    val verifier = generateNonceBlocking(PKCE_NONCE_LENGTH)
    return PkceLoginAttempt(
        pending = PendingLogin(
            verifier = verifier,
            state = generateNonceBlocking(PKCE_NONCE_LENGTH),
        ),
        challenge = pkceChallenge(verifier),
    )
}

internal fun pkceChallenge(verifier: String): String =
    verifier.encodeUtf8().sha256().base64Url().trimEnd('=')

internal fun callbackMatchesState(query: String, expectedState: String): Boolean =
    callbackParameter(query, "state") == expectedState

internal fun authorizationCode(query: String): String? =
    callbackParameter(query, "code")?.takeIf { it.length == AUTHORIZATION_CODE_LENGTH }

internal fun buildAuthUrl(serverUrl: String, redirectUri: String, proof: PkceLoginAttempt): String =
    URLBuilder("$serverUrl/sanctum/token").apply {
        parameters.append("token_name", "Gamerlogue")
        parameters.append("code_challenge", proof.challenge)
        parameters.append("code_challenge_method", "S256")
        parameters.append("state", proof.pending.state)
        parameters.append("redirect_uri", redirectUri)
    }.buildString()

private fun callbackParameter(query: String, name: String): String? =
    parseQueryString(query.substringAfter('?')).getAll(name)?.singleOrNull()

// generateNonceBlocking counts characters, not bytes, and RFC 7636 requires a verifier of 43 to 128 of them —
// the backend enforces the lower bound and rejects the exchange with a 422 below it. 64 hex characters stay
// inside the unreserved charset and carry 32 bytes of entropy.
internal const val PKCE_NONCE_LENGTH = 64

private const val AUTHORIZATION_CODE_LENGTH = 64
