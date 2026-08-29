package it.maicol07.gamerlogue.di

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import it.maicol07.gamerlogue.data.AppJsonApiConfig
import it.maicol07.spraypaintkt_ktor_integration.KtorHttpClient
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.module

/**
 * `AppJsonApiConfig` is a `data object` outside the Koin graph that reaches into it through
 * `KoinComponent.get()`, so nothing checks that wiring at compile time — a wrong qualifier or a
 * definition the Koin compiler plugin failed to register would surface only on the first backend call.
 */
class HttpModuleBindingTest : StringSpec({
    "AppJsonApiConfig resolves its HTTP client from the running Koin instance" {
        startKoin {
            module<HttpModule>()
            // The qualified Ktor client needs AuthTokenProvider for its bearer plugin.
            module<PlatformModule>()
        }
        try {
            AppJsonApiConfig.httpClient.shouldBeInstanceOf<KtorHttpClient>()
        } finally {
            stopKoin()
        }
    }

    "the client is re-resolved per access, so it never outlives its Koin instance" {
        startKoin {
            module<HttpModule>()
            // The qualified Ktor client needs AuthTokenProvider for its bearer plugin.
            module<PlatformModule>()
        }
        val first = try {
            AppJsonApiConfig.httpClient
        } finally {
            stopKoin()
        }

        startKoin {
            module<HttpModule>()
            // The qualified Ktor client needs AuthTokenProvider for its bearer plugin.
            module<PlatformModule>()
        }
        try {
            // A `by lazy` here would hand back `first`, still wrapping the stopped instance's client.
            AppJsonApiConfig.httpClient shouldNotBeSameInstanceAs first
        } finally {
            stopKoin()
        }
    }
})
