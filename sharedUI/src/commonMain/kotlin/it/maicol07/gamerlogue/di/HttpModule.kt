package it.maicol07.gamerlogue.di

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.ktor.IgdbKtorEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.http.contentType
import it.maicol07.gamerlogue.BuildConfig
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.services.PsnApi
import it.maicol07.spraypaintkt_ktor_integration.KtorHttpClient.Companion.VndApiJson
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Qualifier for the Ktor [HttpClient] used by the JSON:API layer (SprayPaintKT).
 * Kept separate from the IGDB client so tests can override just this one with a MockEngine.
 */
val JsonApiHttpClient = named("jsonApiHttpClient")

private fun kermitKtorLogger() = object : Logger {
    override fun log(message: String) {
        co.touchlab.kermit.Logger.v(tag = "HTTP Client") { message }
    }
}

val httpModule = module {
    // Plain client used by the IGDB client.
    single {
        HttpClient {
            install(Logging) {
                logger = kermitKtorLogger()
                level = LogLevel.HEADERS
            }
            install(HttpCache)
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 5)
                // IGDB rate limiting (HTTP 429) is a client error, so retry it explicitly with backoff.
                retryIf(maxRetries = 5) { _, response -> response.status.value == 429 }
                exponentialDelay()
            }
        }
    }

    // Client backing the JSON:API config; injected so it can be swapped in tests.
    single(JsonApiHttpClient) {
        HttpClient {
            defaultRequest {
                accept(VndApiJson)
                contentType(VndApiJson)
            }
            install(Logging) {
                logger = kermitKtorLogger()
                level = LogLevel.HEADERS
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        get<AuthTokenProvider>().accessToken?.let { BearerTokens(it, "") }
                    }
                    refreshTokens {
                        // No refresh yet
                        null
                    }
                }
            }
        }
    }

    single {
        IgdbClient(IgdbKtorEngine) {
            baseUrl = BuildConfig.IGDB_API_URL
            httpClient {
                this.httpClient = get()
            }
        }
    }

    // PSN API client: must NOT follow redirects (the OAuth code is read from the authorize 302).
    single {
        PsnApi(
            HttpClient {
                followRedirects = false
                install(Logging) {
                    logger = kermitKtorLogger()
                    level = LogLevel.HEADERS
                }
            },
        )
    }
}
