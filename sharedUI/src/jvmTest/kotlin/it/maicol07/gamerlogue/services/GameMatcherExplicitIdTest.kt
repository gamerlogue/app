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
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.services.connectors.BackloggeryConnector
import it.maicol07.gamerlogue.ui.components.SyncPhase
import it.maicol07.gamerlogue.ui.components.WebSession
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.gamerlogue.ui.views.settings.categories.ImportHandoff
import it.maicol07.gamerlogue.ui.views.settings.categories.ImportMode
import it.maicol07.gamerlogue.ui.views.settings.categories.LibraryImportViewModel
import it.maicol07.gamerlogue.ui.views.settings.categories.LinkedServicesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Buffer
import java.util.UUID
import java.util.prefs.Preferences

private class IdFixtureIgdb : IgdbClient {
    override val webhookApi: IgdbWebhookApi get() = error("No webhook requests expected")
    var requests = 0
        private set

    override suspend fun <T : Any> execute(
        endpoint: IgdbEndpoint<T>,
        query: ApicalypseQuery
    ): IgdbResult<T, IgdbHttpErrorResponse> {
        endpoint shouldBe IgdbEndpoint.GAME
        requests++
        val response =
            GameResult(
                games = listOf(
                    Game(id = 42, name = "Canonical title"),
                    Game(id = 7, name = "Old match"),
                ),
            )
        return IgdbResult.Success(endpoint.resultParser(query, Buffer().write(GameResult.ADAPTER.encode(response))))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameMatcherExplicitIdTest : StringSpec({
    for ((confirmUnknown, excludeKnown) in listOf(false to false, true to false, true to true)) {
        "ID resync matcher confirmation=$confirmUnknown, exclude known=$excludeKnown" {
            runTest {
                Dispatchers.setMain(StandardTestDispatcher(testScheduler))
                val preferences = Preferences.userRoot().node("gamerlogue-tests/${UUID.randomUUID()}")
                val settings = PreferencesSettings(preferences)
                val reporter = ExceptionReporter()
                val matcher = GameMatcher(IdFixtureIgdb(), reporter)

                val library = LibrarySync(matcher, reporter, LibraryChanges(), ProfileApi())
                val tracker = TrackerSync(settings, matcher, library)
                tracker.record(
                    ExternalService.BACKLOGGERY,
                    mapOf(42 to SyncedGame("bg-100", GameLibraryStatus.BACKLOG)),
                )
                val connector = BackloggeryConnector()
                val viewModel =

                    LinkedServicesViewModel(mapOf(ExternalService.BACKLOGGERY to connector), settings, library, tracker)
                var preview: LibraryImportViewModel? = null
                var writes = 0
                val session = object : WebSession {
                    override val currentUrl: String = "https://backloggery.com/"
                    override suspend fun awaitLogin(connector: ServiceConnector) = Unit
                    override fun reportProgress(done: Int, total: Int) = Unit
                    override suspend fun run(step: WebStep): List<ExternalGameRef> {
                        writes++
                        if (excludeKnown) step.script shouldNotContain "bg-100" else step.script shouldContain "bg-100"
                        if (confirmUnknown) step.script shouldContain "bg-unknown" else step.script shouldNotContain "bg-unknown"
                        return listOfNotNull(
                            ExternalGameRef("bg-100", gameId = 42).takeUnless { excludeKnown },
                            ExternalGameRef("bg-unknown", gameId = 7).takeIf { confirmUnknown },
                        )
                    }

                    override suspend fun <T> read(source: DataSource<T>, phase: SyncPhase): T = when (source) {
                        is DataSource.Web -> source.parse("""[{"uid":"bg-100"},{"uid":"bg-unknown"}]""")
                        is DataSource.Api -> error("No API reads expected")
                    }

                    override suspend fun confirmPush(games: List<LibrarySync.OutgoingGame>): List<LibrarySync.OutgoingGame> =
                        error("No game additions expected")
                }
                try {
                    val refs = checkNotNull(viewModel.runReadForIdSync(ExternalService.BACKLOGGERY, session))
                    writes shouldBe 0
                    ImportHandoff.put(connector.service, refs)
                    val review = LibraryImportViewModel(
                        connector.service, ImportMode.ID_SYNC, mapOf(connector.service to connector), matcher, library, tracker,
                    )
                    preview = review
                    advanceUntilIdle()
                    review.uiState.value.rows[0].included shouldBe true
                    review.uiState.value.rows[1].confident shouldBe false
                    review.uiState.value.rows[1].included shouldBe false
                    if (confirmUnknown) {
                        review.startEdit(1)
                        review.chooseMatch(Game(id = 7, name = "Manually confirmed game"))
                    }
                    if (excludeKnown) review.toggleIncluded(0)
                    review.confirm()
                    advanceUntilIdle()
                    review.uiState.value.pushPending shouldBe true
                    reporter.state.value shouldBe null
                    viewModel.runApplyResyncIds(ExternalService.BACKLOGGERY, session)
                    val expectedWrites = (if (excludeKnown) 0 else 1) + (if (confirmUnknown) 1 else 0)
                    viewModel.uiState.value.outcome shouldBe LinkedServicesViewModel.SyncOutcome.IdsResynced(
                        expectedWrites,
                        2 - expectedWrites
                    )
                    writes shouldBe 1
                } finally {
                    viewModel.viewModelScope.cancel()
                    preview?.viewModelScope?.cancel()
                    ImportHandoff.take(connector.service)
                    preferences.removeNode()
                    Dispatchers.resetMain()
                }
            }

        }
    }

    "a tracker metadata id matches exactly despite a different title and keeps the remote uid" {
        val client = IdFixtureIgdb()
        val matcher = GameMatcher(client, ExceptionReporter())
        val refs = listOf(
            ExternalGameRef("bg-100", "User renamed this game", gameId = 42),
            ExternalGameRef("bg-200", "Another platform edition", gameId = 42),

        )
        val matched = matcher.match(BackloggeryConnector(), refs)
        matched.map { it.ref.uid } shouldBe listOf("bg-100", "bg-200")
        matched.map { it.game?.id } shouldBe listOf(42L, 42L)
        matched.all { it.confident } shouldBe true
        client.requests shouldBe 1
    }

    "tracker metadata takes precedence over an older stored mapping" {
        val preferences = Preferences.userRoot().node("gamerlogue-tests/${UUID.randomUUID()}")
        try {
            val reporter = ExceptionReporter()
            val matcher = GameMatcher(IdFixtureIgdb(), reporter)
            val library = LibrarySync(matcher, reporter, LibraryChanges(), ProfileApi())
            val tracker = TrackerSync(PreferencesSettings(preferences), matcher, library)
            tracker.record(ExternalService.BACKLOGGERY, mapOf(7 to SyncedGame("bg-100", GameLibraryStatus.BACKLOG)))
            val games = tracker.knownGames(ExternalService.BACKLOGGERY, listOf(ExternalGameRef("bg-100", gameId = 42)))
            games.getValue("bg-100").id shouldBe 42L
        } finally {
            preferences.removeNode()
        }
    }
})


