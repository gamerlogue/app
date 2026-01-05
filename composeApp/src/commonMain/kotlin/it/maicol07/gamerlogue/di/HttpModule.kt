package it.maicol07.gamerlogue.di

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.ktor.IgdbKtorEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import it.maicol07.gamerlogue.BuildConfig
import org.koin.dsl.module

val httpModule = module {
    single {
        HttpClient {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        co.touchlab.kermit.Logger.v("HTTP Client") { message }
                    }
                }
                level = LogLevel.HEADERS
            }
            install(HttpCache)
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 5)
                exponentialDelay()
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
}
