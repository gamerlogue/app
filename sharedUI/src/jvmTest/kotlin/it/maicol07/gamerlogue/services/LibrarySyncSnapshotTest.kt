package it.maicol07.gamerlogue.services

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.IgdbResult
import at.released.igdbclient.IgdbWebhookApi
import at.released.igdbclient.apicalypse.ApicalypseQuery
import at.released.igdbclient.error.IgdbHttpErrorResponse
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameResult
import com.russhwolf.settings.PreferencesSettings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.services.connectors.BackloggdConnector
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.gamerlogue.ui.views.settings.categories.ImportHandoff
import it.maicol07.gamerlogue.ui.views.settings.categories.ImportMode
import it.maicol07.gamerlogue.ui.views.settings.categories.LibraryImportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okio.Buffer
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.prefs.Preferences

private class SnapshotFixtureIgdb : IgdbClient {
    override val webhookApi: IgdbWebhookApi get() = error("Unexpected webhook")

    override suspend fun <T : Any> execute(
        endpoint: IgdbEndpoint<T>,
        query: ApicalypseQuery,
    ): IgdbResult<T, IgdbHttpErrorResponse> {
        val buffer = Buffer()
        if (endpoint == IgdbEndpoint.GAME) {
            buffer.write(GameResult.ADAPTER.encode(GameResult(games = listOf(Game(id = 1), Game(id = 2)))))
        }
        return IgdbResult.Success(endpoint.resultParser(query, buffer))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibrarySyncSnapshotTest : StringSpec({
    "reviewed wishlist reloads the library at confirm and carries successful imports into the push preview" {
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val node = Preferences.userRoot().node("gamerlogue-tests/${UUID.randomUUID()}")
            val settings = PreferencesSettings(node)
            val reads = AtomicInteger()
            val writes = AtomicInteger()
            val writtenMethods = mutableListOf<HttpMethod>()
            val client = HttpClient(
                MockEngine { request ->
                    val body = if (request.method == HttpMethod.Get) {
                        reads.incrementAndGet()
                        """
                        {"data":[{"type":"LibraryEntry","id":"1","attributes":{"game_id":1,"status":"BACKLOG","owned":false}}],
                         "meta":{"totalItems":1}}
                        """.trimIndent()
                    } else {
                        writtenMethods += request.method
                        writes.incrementAndGet()
                        val data = Json.parseToJsonElement(
                            (request.body as TextContent).text
                        ).jsonObject.getValue("data").jsonObject
                        val id = if (request.method == HttpMethod.Post) JsonPrimitive("2") else data.getValue("id")
                        JsonObject(mapOf("data" to JsonObject(data + ("id" to id)))).toString()
                    }
                    respond(body, headers = headersOf("Content-Type", "application/vnd.api+json"))
                }
            )
            startKoin {
                modules(
                    module {
                        single(named("JsonApiHttpClient")) { client }
                        single { AppPreferences(settings) }
                    }
                )
            }
            val reporter = ExceptionReporter()
            val matcher = GameMatcher(SnapshotFixtureIgdb(), reporter)
            val library = LibrarySync(matcher, reporter, LibraryChanges(), ProfileApi())
            val connector = BackloggdConnector()
            ImportHandoff.put(connector.service, listOf(ExternalGameRef("2")))
            val preview = LibraryImportViewModel(
                connector.service,
                ImportMode.WISHLIST,
                mapOf(connector.service to connector),
                matcher,
                library,
                TrackerSync(settings, matcher, library),
            )
            try {
                preview.uiState.first { !it.matching }.loading shouldBe false
                reads.get() shouldBe 1
                preview.confirm().join()
                reads.get() shouldBe 2
                writes.get() shouldBe 1
                val push = library.pendingWishlistImports.getValue(connector.service)
                push.added shouldBe 1
                push.toPush.map { it.gameId }.sorted() shouldBe listOf(1, 2)
                push.toPush.first { it.gameId == 2 }.alreadyOnWishlist shouldBe true
                writtenMethods shouldBe listOf(HttpMethod.Post)

                val owned = LibraryEntry().apply {
                    id = "1"
                    isPersisted = true
                    gameId = 1
                    status = GameLibraryStatus.COMPLETED
                    this.owned = true
                    editionsIds = listOf(9)
                    platformsIds = listOf(6)
                }
                val snapshot = mutableListOf(owned)
                val editions = listOf(10L, 11L).map { Game(id = it, version_parent = Game(id = 1)) }
                library.importOwned(connector, editions, snapshot) shouldBe 1
                reads.get() shouldBe 2
                writes.get() shouldBe 2
                writtenMethods shouldBe listOf(HttpMethod.Post, HttpMethod.Patch)
                snapshot.single().gameId shouldBe 1
                snapshot.single().editionsIds shouldBe listOf(9, 10, 11)
                snapshot.single().status shouldBe GameLibraryStatus.COMPLETED
                snapshot.single().owned shouldBe true
                owned.editionsIds shouldBe listOf(9)
            } finally {
                preview.viewModelScope.cancel()
                client.close()
                stopKoin()
                node.removeNode()
                Dispatchers.resetMain()
            }
        }
    }
})
