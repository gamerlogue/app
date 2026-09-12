package it.maicol07.gamerlogue.auth

import io.kotest.assertions.throwables.shouldThrow
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Clock

class AuthenticationHandlerTest : StringSpec({
    "PKCE challenge matches RFC 7636" {
        pkceChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk") shouldBe
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
    }

    "PKCE callback validates state and exchanges the code once" {
        val provider = TestAuthTokenProvider()
        val engine = MockEngine {
            respond(
                content = """{"access_token":"new-token","refresh_token":"new-refresh-token","user_id":"2",""" +
                    """"expires_at":"2099-01-01T00:00:00Z","refresh_expires_at":"2099-02-01T00:00:00Z"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val handler = TestAuthenticationHandler(provider, testClient(engine))
        val proof = PkceLoginAttempt(PendingLogin("v".repeat(43), "expected-state"), "challenge")

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
        provider.session.value.refreshToken shouldBe "new-refresh-token"
        provider.session.value.userId shouldBe "2"
        provider.session.value.accessExpiresAtEpochMillis shouldBe 4_070_908_800_000L
    }

    "restore rejects partial bearer sessions" {
        val provider = TestAuthTokenProvider(
            AuthTokenProvider.PersistedSession(
                accessToken = "expired-token",
                userId = "1",
                accessExpiresAtEpochMillis = Clock.System.now().toEpochMilliseconds() - 1,
            )
        )

        provider.session.value shouldBe AuthTokenProvider.Session()
        provider.persisted shouldBe AuthTokenProvider.PersistedSession()
    }

    "concurrent requests rotate an expired access token once" {
        val calls = AtomicInteger()
        val provider = TestAuthTokenProvider(
            AuthTokenProvider.PersistedSession(
                accessToken = "expired-token",
                refreshToken = "refresh-token",
                userId = "1",
                accessExpiresAtEpochMillis = Clock.System.now().toEpochMilliseconds() - 1,
                refreshExpiresAtEpochMillis = Long.MAX_VALUE,
            )
        )
        val engine = MockEngine {
            calls.incrementAndGet()
            delay(50)
            respond(
                content = """{"access_token":"fresh-token","refresh_token":"fresh-refresh-token","user_id":"1",""" +
                    """"expires_at":"2099-01-01T00:00:00Z","refresh_expires_at":"2099-02-01T00:00:00Z"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val handler = TestAuthenticationHandler(provider, testClient(engine))

        val tokens = coroutineScope { List(4) { async { handler.loadBearerTokens() } }.awaitAll() }

        tokens.map { it?.accessToken } shouldBe List(4) { "fresh-token" }
        calls.get() shouldBe 1
        provider.persisted.refreshToken shouldBe "fresh-refresh-token"
    }

    "refresh rejection clears the compromised session" {
        val provider = TestAuthTokenProvider(
            AuthTokenProvider.PersistedSession(
                accessToken = "expired-token",
                refreshToken = "reused-refresh-token",
                userId = "1",
                accessExpiresAtEpochMillis = Clock.System.now().toEpochMilliseconds() - 1,
                refreshExpiresAtEpochMillis = Long.MAX_VALUE,
            )
        )
        val engine = MockEngine {
            respond(
                content = """{"error":"refresh_token_reused","message":"Token family revoked"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        shouldThrow<AuthenticationLostException> {
            TestAuthenticationHandler(provider, testClient(engine)).loadBearerTokens()
        }
        provider.session.value shouldBe AuthTokenProvider.Session()
        provider.persisted shouldBe AuthTokenProvider.PersistedSession()
    }

    "failed native revoke keeps the session for a later retry" {
        val provider = TestAuthTokenProvider()
        provider.updateCredentials("access", "refresh", "1", Long.MAX_VALUE - 1, Long.MAX_VALUE)
        val engine = MockEngine { respond("backend unavailable", HttpStatusCode.InternalServerError) }

        shouldThrow<TokenEndpointException> {
            TestAuthenticationHandler(provider, testClient(engine)).logout()
        }
        provider.session.value.accessToken shouldBe "access"
        provider.persisted.refreshToken shouldBe "refresh"
    }

    "profile loaded for an old account is discarded" {
        val provider = TestAuthTokenProvider()
        provider.updateCredentials("old-token", "old-refresh", "1", Long.MAX_VALUE - 1, Long.MAX_VALUE)
        val oldSession = provider.session.value
        val staleUser = User().apply { id = "1" }

        provider.updateCredentials("new-token", "new-refresh", "2", Long.MAX_VALUE - 1, Long.MAX_VALUE)
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
) : NativeAuthenticationHandler(provider, client) {
    private var pending: PendingLogin? = null

    fun begin(proof: PkceLoginAttempt): String {
        pending = proof.pending
        return buildAuthUrl("gamerlogue://auth/callback", proof)
    }

    override fun launchLogin(attempt: PkceLoginAttempt) = Unit

    override suspend fun handleCallback(query: String): Boolean {
        val current = pending ?: return false
        if (!callbackMatchesState(query, current.state)) return false
        pending = null
        return exchangeCallback(query, current)
    }
}

private class TestAuthTokenProvider(
    var persisted: PersistedSession = PersistedSession(),
) : AuthTokenProvider() {
    init {
        restore()
    }

    override fun loadPersistedSession() = persisted

    override fun savePersistedSession(persisted: PersistedSession) {
        this.persisted = persisted
    }
}
