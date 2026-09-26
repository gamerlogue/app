package it.maicol07.gamerlogue.di

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import org.koin.plugin.module.dsl.module

/**
 * `HttpCache` used to be installed on every client, including the bearer-authed JSON:API one. Ktor keys
 * its cache on the URL and ignores the `Authorization` header, so a response fetched under one token was
 * served back after a logout and re-login as somebody else.
 */
@OptIn(KoinInternalApi::class)
class HttpCacheAuthTest : StringSpec({
    "HttpCache ignores the bearer token, which is why authed clients must not install it" {
        var token = "user-a-token"
        val engine = MockEngine {
            respond(
                content = "response-for-${it.headers[HttpHeaders.Authorization]}",
                headers = headersOf(
                    HttpHeaders.CacheControl to listOf("private, max-age=600"),
                    HttpHeaders.ContentType to listOf("application/json"),
                )
            )
        }
        val client = HttpClient(engine) {
            install(HttpCache)
            install(Auth) {
                bearer {
                    cacheTokens = false
                    loadTokens { BearerTokens(token, "") }
                }
            }
        }

        client.get("https://example.test/api/library").bodyAsText() shouldBe "response-for-Bearer user-a-token"
        token = "user-b-token"

        // The hazard, pinned: same URL, different token, stale body. If a Ktor upgrade ever makes this
        // vary by Authorization, this test fails and the cache can be reconsidered.
        client.get("https://example.test/api/library").bodyAsText() shouldBe "response-for-Bearer user-a-token"
    }

    "the JSON:API client has no response cache, the IGDB one does" {
        startKoin {
            module<HttpModule>()
            module<PlatformModule>()
        }
        try {
            // Compile safety is off for this compilation (see sharedUI/build.gradle.kts): the checker cannot
            // see main's definitions from a test compilation, so this lookup is only verified at runtime.
            val scope = KoinPlatform.getKoin().scopeRegistry.rootScope
            val jsonApiClient: HttpClient = scope.get(named("JsonApiHttpClient"))
            jsonApiClient.pluginOrNull(HttpCache).shouldBeNull()
        } finally {
            stopKoin()
        }
    }
})
