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

    // Resolved lazily so Koin (started by App/tests) is ready at first request.
    // The underlying Ktor client (auth, headers) lives in httpModule and can be
    // swapped with a MockEngine in tests via the [JsonApiHttpClient] qualifier.
    override val httpClient: HttpClient by lazy {
        KtorHttpClient(httpClient = get<io.ktor.client.HttpClient>(named("JsonApiHttpClient")))
    }
}
