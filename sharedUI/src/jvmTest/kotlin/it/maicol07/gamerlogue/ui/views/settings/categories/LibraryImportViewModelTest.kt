package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.IgdbResult
import at.released.igdbclient.IgdbWebhookApi
import at.released.igdbclient.apicalypse.ApicalypseQuery
import at.released.igdbclient.error.IgdbHttpErrorResponse
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameResult
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.GameMatcher
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.MatchMemory
import it.maicol07.gamerlogue.services.SyncedGame
import it.maicol07.gamerlogue.services.TrackerSync
import it.maicol07.gamerlogue.services.connectors.BackloggdConnector
import it.maicol07.gamerlogue.services.connectors.BackloggeryConnector
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Buffer
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.UUID
import java.util.prefs.Preferences

private val noRequestsIgdb = object : IgdbClient {
    override val webhookApi: IgdbWebhookApi get() = error("No IGDB requests expected")
    override suspend fun <T : Any> execute(endpoint: IgdbEndpoint<T>, query: ApicalypseQuery): Nothing =
        error("No IGDB requests expected")
}

/** Answers every game query with games 1 and 2, whatever was asked. */
private class SyncFixtureIgdb : IgdbClient {
    override val webhookApi: IgdbWebhookApi get() = error("No IGDB webhooks expected")
    override suspend fun <T : Any> execute(
        endpoint: IgdbEndpoint<T>,
        query: ApicalypseQuery,
    ): IgdbResult<T, IgdbHttpErrorResponse> {
        val buffer = Buffer().write(GameResult.ADAPTER.encode(GameResult(games = listOf(Game(id = 1), Game(id = 2)))))
        return IgdbResult.Success(endpoint.resultParser(query, buffer))
    }
}

private fun entryJson(gameId: Int, status: GameLibraryStatus, owned: Boolean) =
    """{"type":"LibraryEntry","id":"$gameId","attributes":{"game_id":$gameId,"status":"${status.name}","owned":$owned}}"""

/** A backend whose library holds [entries]. */
private fun libraryClient(vararg entries: String) = HttpClient(
    MockEngine {
        respond(
            """{"data":[${entries.joinToString(",")}],"meta":{"totalItems":${entries.size}}}""",
            headers = headersOf("Content-Type", "application/vnd.api+json"),
        )
    }
)

private fun startLibraryKoin(client: HttpClient, settings: ObservableSettings) {
    startKoin {
        modules(
            module {
                single(named("JsonApiHttpClient")) { client }
                single { AppPreferences(settings) }
            }
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryImportViewModelTest : StringSpec({
    val preferences = Preferences.userRoot().node("gamerlogue-tests/${UUID.randomUUID()}")
    beforeTest { Dispatchers.setMain(StandardTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }
    afterSpec { preferences.removeNode() }

    for ((connector, shouldPush) in listOf(BackloggdConnector() to true, BackloggeryConnector() to true)) {
        "${connector.service}: an empty incoming wishlist advances only when outgoing writes are supported" {
            runTest {
                Dispatchers.setMain(StandardTestDispatcher(testScheduler))
                val client = libraryClient()
                startLibraryKoin(client, PreferencesSettings(preferences))
                val reporter = ExceptionReporter()
                val matcher = GameMatcher(noRequestsIgdb, reporter, MatchMemory(MapSettings()))
                val library = LibrarySync(matcher, reporter, LibraryChanges(), ProfileApi())
                val tracker = TrackerSync(PreferencesSettings(preferences), matcher, library)
                ImportHandoff.put(connector.service, emptyList())
                val viewModel = LibraryImportViewModel(
                    connector.service, ImportMode.WISHLIST, mapOf(connector.service to connector), matcher, library, tracker,
                )
                try {
                    advanceUntilIdle()
                    viewModel.uiState.value.rows.shouldBeEmpty()
                    viewModel.uiState.value.matching shouldBe false
                    viewModel.hasNextStep shouldBe shouldPush

                    viewModel.confirm().join()

                    viewModel.uiState.value.importedCount shouldBe 0
                    viewModel.uiState.value.pushPending shouldBe shouldPush
                    library.pendingWishlistImports[connector.service]?.added shouldBe if (shouldPush) 0 else null
                    reporter.state.value.shouldBeNull()
                } finally {
                    ImportHandoff.take(connector.service)
                    viewModel.viewModelScope.cancel()
                    client.close()
                    stopKoin()
                }
            }
        }
    }

    // Game 1 differs between the sides (a conflict); game 2 is BACKLOG on both (agreed). Both are on the tracker.
    suspend fun TestScope.syncPreview(deselectSecond: Boolean, block: suspend (LibraryImportViewModel, TrackerSync) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val client = libraryClient(
            entryJson(1, GameLibraryStatus.COMPLETED, owned = false),
            entryJson(2, GameLibraryStatus.BACKLOG, owned = true),
        )
        val settings = PreferencesSettings(preferences)
        startLibraryKoin(client, settings)
        val connector = BackloggdConnector()
        val reporter = ExceptionReporter()
        val matcher = GameMatcher(SyncFixtureIgdb(), reporter, MatchMemory(MapSettings()))
        val library = LibrarySync(matcher, reporter, LibraryChanges(), ProfileApi())
        val tracker = TrackerSync(settings, matcher, library)
        tracker.forget(connector.service)
        ImportHandoff.put(
            connector.service,
            listOf(
                ExternalGameRef("1", status = GameLibraryStatus.BACKLOG),
                ExternalGameRef("2", status = GameLibraryStatus.BACKLOG),
            ),
        )
        val preview = LibraryImportViewModel(
            connector.service, ImportMode.SYNC, mapOf(connector.service to connector), matcher, library, tracker,
        )
        try {
            preview.uiState.first { !it.matching }
            if (deselectSecond) preview.toggleIncluded(1)
            preview.confirm().join()
            block(preview, tracker)
        } finally {
            tracker.forget(connector.service)
            ImportHandoff.take(connector.service)
            preview.viewModelScope.cancel()
            client.close()
            stopKoin()
        }
    }

    "a sync records its agreed games only when the reviewed plan is applied" {
        runTest {
            syncPreview(deselectSecond = false) { preview, tracker ->
                preview.uiState.value.plan.orEmpty().map { it.change.gameId } shouldBe listOf(1)
                tracker.base(ExternalService.BACKLOGGD).isEmpty() shouldBe true
                preview.applySync().join()
                tracker.base(ExternalService.BACKLOGGD) shouldBe mapOf(2 to SyncedGame("2", GameLibraryStatus.BACKLOG))
            }
        }
    }

    "a tracker row left out of the sync is not planned as an addition to the tracker" {
        runTest {
            syncPreview(deselectSecond = true) { preview, _ ->
                preview.uiState.value.plan.orEmpty().map { it.change.gameId } shouldBe listOf(1)
            }
        }
    }
})
