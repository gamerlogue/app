package it.maicol07.gamerlogue.di

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.ktor.IgdbKtorEngine
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import it.maicol07.gamerlogue.AppEnvironment
import it.maicol07.gamerlogue.BuildConfig
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import it.maicol07.gamerlogue.auth.configurePlatformSession
import it.maicol07.gamerlogue.services.EpicApi
import it.maicol07.gamerlogue.services.PsnApi
import it.maicol07.gamerlogue.services.UbisoftApi
import it.maicol07.gamerlogue.services.XboxApi
import it.maicol07.spraypaintkt_ktor_integration.KtorHttpClient.Companion.VndApiJson
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.coroutines.cancellation.CancellationException

/**
 * Shared across every client. Deliberately **without** `HttpCache`: Ktor keys the cache on the URL and
 * ignores the `Authorization` header, so on a per-user endpoint a response fetched under one token is
 * replayed under the next one (see `HttpCacheAuthTest`). Only the IGDB client, whose credentials are
 * the app's rather than the user's, installs it.
 */
private val ktorHttpClientConfig: HttpClientConfig<*>.() -> Unit = {
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                co.touchlab.kermit.Logger.v(tag = "HTTP Client") { message }
            }
        }
        // HEADERS logs the Authorization header, so it stays out of anything but a local build.
        level = if (BuildConfig.APP_ENV == AppEnvironment.LOCAL) LogLevel.HEADERS else LogLevel.NONE
    }
    install(HttpTimeout) {
        requestTimeoutMillis = RequestTimeoutMillis
        connectTimeoutMillis = ConnectTimeoutMillis
        socketTimeoutMillis = SocketTimeoutMillis
    }
    install(HttpRequestRetry) {
        maxRetries = 3
        retryIf { _, response ->
            response.status.value in ServerErrorRange || response.status == HttpStatusCode.TooManyRequests
        }
        // Without this a dropped connection fails the call outright; cancellation must still propagate.
        retryOnExceptionIf { _, cause -> cause !is CancellationException }
        // Honours Retry-After, which is what makes the 429 branch above worth having.
        exponentialDelay()
    }
}

private val ServerErrorRange = 500..599

// Generous enough for a cold backend, short enough that a dead connection surfaces as an error
// instead of an indefinite spinner.
private const val RequestTimeoutMillis = 30_000L
private const val ConnectTimeoutMillis = 15_000L
private const val SocketTimeoutMillis = 30_000L

private val PlatformSession = createClientPlugin("PlatformSession") {
    onRequest { request, _ -> request.configurePlatformSession() }
}

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
                // Safe here, unlike on the user-scoped clients: IGDB responses are the same for every
                // user, so a URL-keyed cache cannot leak one user's data to another.
                install(HttpCache)
            }
        }
    }

    @Single
    @Named("JsonApiHttpClient")
    fun provideJsonApiHttpClient(
        authTokenProvider: AuthTokenProvider,
        authenticationHandler: AuthenticationHandler,
    ) = HttpClient {
        defaultRequest {
            accept(VndApiJson)
            contentType(VndApiJson)
        }
        install(PlatformSession)
        ktorHttpClientConfig()
        HttpResponseValidator {
            validateResponse { response ->
                val current = authTokenProvider.session.value
                val requestBearer = response.call.request.headers[HttpHeaders.Authorization]
                val bearerMatches = current.accessToken?.let { requestBearer == "Bearer $it" } == true
                val belongsToCurrentSession = bearerMatches ||
                    (current.isAuthenticated && current.accessToken == null)
                val refreshAlreadyTried = response.call.request.attributes.contains(AuthCircuitBreaker)
                if (response.status == HttpStatusCode.Unauthorized && belongsToCurrentSession &&
                    (current.refreshToken == null || refreshAlreadyTried)
                ) {
                    authTokenProvider.clearSession()
                }
            }
        }
        install(Auth) {
            bearer {
                cacheTokens = false
                nonCancellableRefresh = true
                loadTokens { authenticationHandler.loadBearerTokens() }
                refreshTokens {
                    val requestToken = response.call.request.headers[HttpHeaders.Authorization]
                        ?.removePrefix("Bearer ")
                    authenticationHandler.refreshBearerTokens(requestToken)
                }
            }
        }
    }

    @Single
    @Named("AuthHttpClient")
    fun provideAuthHttpClient() = HttpClient {
        expectSuccess = true
        install(PlatformSession)
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = RequestTimeoutMillis
            connectTimeoutMillis = ConnectTimeoutMillis
            socketTimeoutMillis = SocketTimeoutMillis
        }
        install(HttpRequestRetry) {
            maxRetries = 1
            retryIf { _, response ->
                (response.status == HttpStatusCode.TooManyRequests).also { retry ->
                    if (retry) co.touchlab.kermit.Logger.w { "Token endpoint rate limited; retrying after Retry-After" }
                }
            }
            retryOnExceptionIf { request, cause ->
                val retry = request.url.build().encodedPath.endsWith("/api/sanctum/token/refresh") &&
                    cause !is CancellationException
                if (retry) co.touchlab.kermit.Logger.w(cause) { "Token refresh transport failed; retrying once" }
                retry
            }
            exponentialDelay()
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

    // Epic launcher API client (token exchange + library/catalog); plain JSON, the auth code comes from
    // the WebView. Off-WebView so it isn't CORS-blocked like the same calls would be in the browser.
    @Single
    fun provideEpicApi() = EpicApi(
        HttpClient {
            ktorHttpClientConfig()
        }
    )

    // Ubisoft Connect API client (GraphQL owned games); plain JSON, the session ticket comes from the
    // WebView. Off-WebView so it isn't CORS-blocked like the same calls would be in the browser.
    @Single
    fun provideUbisoftApi() = UbisoftApi(
        HttpClient {
            ktorHttpClientConfig()
        }
    )
}
