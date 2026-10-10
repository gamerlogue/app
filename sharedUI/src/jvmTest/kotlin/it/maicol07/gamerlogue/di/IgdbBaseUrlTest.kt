package it.maicol07.gamerlogue.di

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.post

/**
 * `igdbclient` is built against a placeholder host; this plugin is what moves its requests onto the
 * endpoint chosen in the settings, keeping the base path, the endpoint and the query.
 */
class IgdbBaseUrlTest : StringSpec({
    "a placeholder request is sent to the current endpoint, path and query preserved" {
        @Suppress("CanBeVal")
        var baseUrl = "http://10.0.2.2/api/igdb"
        val sent = mutableListOf<String>()
        val client = HttpClient(MockEngine { request -> sent += request.url.toString(); respondOk() }) {
            install(igdbBaseUrl { baseUrl })
        }

        client.post("https://$IGDB_PLACEHOLDER_HOST/games?x=1")
        baseUrl = "https://igdb.example.test/v4/"
        client.post("https://$IGDB_PLACEHOLDER_HOST/games")
        client.post("https://other.example.test/oauth2/token")

        sent shouldBe listOf(
            "http://10.0.2.2/api/igdb/games?x=1",
            "https://igdb.example.test/v4/games",
            "https://other.example.test/oauth2/token",
        )
    }
})
