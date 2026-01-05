package it.maicol07.gamerlogue.data

import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.contentType
import it.maicol07.spraypaintkt.PaginationStrategy
import it.maicol07.spraypaintkt.interfaces.HttpClient
import it.maicol07.spraypaintkt.interfaces.JsonApiConfig
import it.maicol07.spraypaintkt_annotation.DefaultInstance
import it.maicol07.spraypaintkt_ktor_integration.KtorHttpClient
import it.maicol07.spraypaintkt_ktor_integration.KtorHttpClient.Companion.VndApiJson
import it.maicol07.gamerlogue.auth.AuthState
import io.ktor.client.plugins.auth.providers.BearerTokens
import it.maicol07.gamerlogue.BuildConfig

@DefaultInstance
data object AppJsonApiConfig : JsonApiConfig {
    override val baseUrl: String = "${BuildConfig.GAMERLOGUE_URL}/api"
    override val paginationStrategy: PaginationStrategy = PaginationStrategy.OFFSET_BASED
    override val httpClient: HttpClient = KtorHttpClient(
        httpClientOptions = {
            defaultRequest {
                accept(VndApiJson)
                contentType(VndApiJson)
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val token = AuthState.token
                        token?.let { BearerTokens(it, "") }
                    }
                    refreshTokens {
                        // No refresh yet
                        null
                    }
                }
            }
        }
    )
}
