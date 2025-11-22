package it.maicol07.gamerlogue.di

import Gamerlogue.composeApp.BuildConfig
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.ktor.IgdbKtorEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.logging.Logging
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
object HttpModule {
    @Single
    fun provideHttpClient() = HttpClient {
        install(Logging)
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            exponentialDelay()
        }
    }

    @Single
    fun provideIgdbClient(httpClient: HttpClient) = IgdbClient(IgdbKtorEngine) {
        baseUrl = BuildConfig.IGDB_API_URL
        httpClient {
            this.httpClient = httpClient
        }
    }
}
