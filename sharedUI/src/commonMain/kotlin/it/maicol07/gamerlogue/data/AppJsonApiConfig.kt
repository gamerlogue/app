package it.maicol07.gamerlogue.data

import it.maicol07.gamerlogue.BuildConfig
import it.maicol07.spraypaintkt.PaginationStrategy
import it.maicol07.spraypaintkt.interfaces.HttpClient
import it.maicol07.spraypaintkt.interfaces.JsonApiConfig
import it.maicol07.spraypaintkt_annotation.DefaultInstance
import it.maicol07.spraypaintkt_ktor_integration.KtorHttpClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named

@DefaultInstance
data object AppJsonApiConfig : JsonApiConfig, KoinComponent {
    override val baseUrl: String = "${BuildConfig.GAMERLOGUE_URL}/api"

    // Backend is page-based (rejects page[offset] with "Page should not be less than 1").
    override val paginationStrategy: PaginationStrategy = PaginationStrategy.PAGE_BASED

    /**
     * Resolved per access rather than `by lazy`: this is a `data object`, so a cached value would outlive
     * the Koin instance the client came from and keep routing requests through a stopped context.
     *
     * The wrapper is built here rather than registered in [it.maicol07.gamerlogue.di.HttpModule]: the
     * Koin compiler plugin (1.0.2) silently fails to register a `KtorHttpClient` provider — no
     * diagnostic, the definition just is not there — under every shape tried (interface or concrete
     * return type, `binds = [...]`, `@Named` parameter, `Scope` parameter). Retry when the plugin is
     * updated. Construction is cheap: with `httpClient` passed explicitly, `KtorHttpClient`'s constructor
     * only stores the reference (its `HttpClient(...)` default argument is never evaluated), and Koin
     * caches the underlying Ktor singleton.
     */
    override val httpClient: HttpClient
        get() = KtorHttpClient(httpClient = get<io.ktor.client.HttpClient>(named("JsonApiHttpClient")))
}
