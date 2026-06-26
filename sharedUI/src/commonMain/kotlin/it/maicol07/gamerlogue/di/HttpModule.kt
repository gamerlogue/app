package it.maicol07.gamerlogue.di

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.ktor.IgdbKtorEngine
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
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
import it.maicol07.gamerlogue.services.XboxApi
import it.maicol07.spraypaintkt_ktor_integration.KtorHttpClient.Companion.VndApiJson
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val ktorHttpClientConfig: HttpClientConfig<*>.() -> Unit = {
    install(HttpCache)
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                co.touchlab.kermit.Logger.v(tag = "HTTP Client") { message }
            }
        }
        level = LogLevel.HEADERS
    }
    install(HttpRequestRetry) {
        maxRetries = 3
        retryIf { _, response ->
            response.status.value in 500..599
        }
    }
}

@Suppress("unused")
@Module
@Configuration
object HttpModule {
    /**
     * Provides an instance of [IgdbClient] configured with a custom Ktor HTTP engine and base URL.
     *
     * The client is initialized using the [IgdbKtorEngine] and a base URL defined in [BuildConfig.IGDB_API_URL].
     * A custom [HttpClient] is configured and provided to handle HTTP communication, where additional settings
     * can be applied through the `ktorHttpClientConfig` function.
     *
     * This method is annotated with `@Single` indicating it provides a singleton instance in the Koin dependency injection setup.
     *
     * @return A configured instance of [IgdbClient].
     */
    @Single
    fun provideIgdbClient() = IgdbClient(IgdbKtorEngine) {
        baseUrl = BuildConfig.IGDB_API_URL
        httpClient {
            this.httpClient = HttpClient {
                ktorHttpClientConfig()
            }
        }
    }

    @Single
    @Named("JsonApiHttpClient")
    fun provideJsonApiHttpClient(authTokenProvider: AuthTokenProvider) = HttpClient {
        defaultRequest {
            accept(VndApiJson)
            contentType(VndApiJson)
        }
        ktorHttpClientConfig()
        install(Auth) {
            bearer {
                loadTokens {
                    authTokenProvider.accessToken?.let { BearerTokens(it, "") }
                }
            }
        }
    }

    // PSN API client: must NOT follow redirects (the OAuth code is read from the authorize 302).
    @Single
    fun providePsnApi() = PsnApi(
        HttpClient {
            followRedirects = false
            ktorHttpClientConfig()
        }
    )

    // Xbox Live API client (token chain + titlehub); plain JSON calls, the MSA token comes from the WebView.
    @Single
    fun provideXboxApi() = XboxApi(
        HttpClient {
            ktorHttpClientConfig()
        }
    )
}
