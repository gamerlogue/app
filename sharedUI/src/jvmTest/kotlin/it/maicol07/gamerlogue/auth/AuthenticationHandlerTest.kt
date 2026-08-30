package it.maicol07.gamerlogue.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.maicol07.gamerlogue.data.User
import kotlin.time.Clock

class AuthenticationHandlerTest : StringSpec({
    "PKCE callback validates state and exchanges the code once" {
        val provider = TestAuthTokenProvider()
        val engine = MockEngine {
            respond(
                content = """{"token":"new-token","user_id":"2","expires_at":"2099-01-01T00:00:00Z"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val handler = TestAuthenticationHandler(provider, testClient(engine))
        val proof = LoginProof("v".repeat(43), "challenge", "expected-state")

        val authUrl = Url(handler.begin(proof))
        authUrl.parameters["code_challenge"] shouldBe "challenge"
        authUrl.parameters["code_challenge_method"] shouldBe "S256"
        authUrl.parameters["state"] shouldBe "expected-state"
        handler.handleCallback("code=${"c".repeat(64)}&state=wrong") shouldBe false
        engine.requestHistory.size shouldBe 0
        handler.handleCallback("code=${"c".repeat(64)}&state=expected-state") shouldBe true
        engine.requestHistory.size shouldBe 1
        val exchangeForm = engine.requestHistory.single().body as FormDataContent
        exchangeForm.formData["code"] shouldBe "c".repeat(64)
        exchangeForm.formData["code_verifier"] shouldBe "v".repeat(43)
        handler.handleCallback("code=${"c".repeat(64)}&state=expected-state") shouldBe false
        provider.session.value.accessToken shouldBe "new-token"
        provider.session.value.userId shouldBe "2"
        provider.session.value.expiresAtEpochMillis shouldBe 4_070_908_800_000L
    }

    "restore rejects expired or partial bearer sessions" {
        val provider = TestAuthTokenProvider(
            savedToken = "expired-token",
            savedUserId = "1",
            savedExpiresAt = Clock.System.now().toEpochMilliseconds() - 1,
        )

        provider.session.value shouldBe AuthTokenProvider.Session()
        provider.savedToken shouldBe null
        provider.savedUserId shouldBe null
        provider.savedExpiresAt shouldBe null
    }

    "profile loaded for an old account is discarded" {
        val provider = TestAuthTokenProvider()
        provider.updateCredentials("old-token", "1", Long.MAX_VALUE)
        val oldSession = provider.session.value
        val staleUser = User().apply { id = "1" }

        provider.updateCredentials("new-token", "2", Long.MAX_VALUE)
        provider.updateUser(staleUser, oldSession)

        provider.session.value.user shouldBe null
    }
})

private fun testClient(engine: MockEngine) = HttpClient(engine) {
    install(ContentNegotiation) { json() }
}

private class TestAuthenticationHandler(
    provider: AuthTokenProvider,
    client: HttpClient,
) : AuthenticationHandler(provider, client) {
    private var proof: LoginProof? = null

    fun begin(value: LoginProof): String {
        proof = value
        return getAuthUrl("gamerlogue://auth/callback", value)
    }

    override fun login() = Unit

    override suspend fun handleCallback(query: String): Boolean {
        val current = proof ?: return false
        if (!callbackMatchesState(query, current.state)) return false
        proof = null
        return exchangeCallback(query, current)
    }
}

private class TestAuthTokenProvider(
    var savedToken: String? = null,
    var savedUserId: String? = null,
    var savedExpiresAt: Long? = null,
) : AuthTokenProvider() {
    init {
        restore()
    }

    override fun loadToken() = savedToken
    override fun saveToken(token: String?) {
        savedToken = token
    }

    override fun loadUserId() = savedUserId
    override fun saveUserId(userId: String?) {
        savedUserId = userId
    }

    override fun loadExpiresAtEpochMillis() = savedExpiresAt
    override fun saveExpiresAtEpochMillis(value: Long?) {
        savedExpiresAt = value
    }
}
