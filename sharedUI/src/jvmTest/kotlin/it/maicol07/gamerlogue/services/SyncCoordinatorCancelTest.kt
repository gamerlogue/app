package it.maicol07.gamerlogue.services

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.IgdbResult
import at.released.igdbclient.IgdbWebhookApi
import at.released.igdbclient.apicalypse.ApicalypseQuery
import at.released.igdbclient.error.IgdbHttpErrorResponse
import com.russhwolf.settings.MapSettings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.services.connectors.BackloggeryConnector
import it.maicol07.gamerlogue.ui.components.SyncPhase
import it.maicol07.gamerlogue.ui.components.WebSession
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private class NoIgdb : IgdbClient {
    override val webhookApi: IgdbWebhookApi get() = error("No IGDB webhooks expected")
    override suspend fun <T : Any> execute(
        endpoint: IgdbEndpoint<T>,
        query: ApicalypseQuery,
    ): IgdbResult<T, IgdbHttpErrorResponse> = error("No IGDB calls expected")
}

/** A session whose sign-in never ends, so a sync stays current until cancelled. */
private object StuckSession : WebSession {
    override val currentUrl: String? = null
    override suspend fun awaitLogin(connector: ServiceConnector) = awaitCancellation()
    override fun reportProgress(done: Int, total: Int) = Unit
    override suspend fun run(step: WebStep): List<ExternalGameRef> = emptyList()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> read(source: DataSource<T>, phase: SyncPhase): T = Unit as T
    override suspend fun confirmPush(games: List<LibrarySync.OutgoingGame>) = games
}

@OptIn(ExperimentalCoroutinesApi::class)
class SyncCoordinatorCancelTest : StringSpec({
    suspend fun coordinator(history: SyncHistory, dispatcher: CoroutineDispatcher): SyncCoordinator {
        val settings = MapSettings()
        val reporter = ExceptionReporter()
        val matcher = GameMatcher(NoIgdb(), reporter, MatchMemory(testSyncStorage(dispatcher)))
        val wishlistState = WishlistSyncState(testSyncStorage(dispatcher))
        val library = LibrarySync(matcher, reporter, LibraryChanges(), ProfileApi(), wishlistState)
        val tracker = TrackerSync(testSyncStorage(dispatcher), matcher, library)
        // Synced once before, so its library sync runs unattended.
        tracker.record(ExternalService.BACKLOGGERY, mapOf(42 to SyncedGame("bg-1", GameLibraryStatus.BACKLOG)))
        settings.putBoolean(ExternalService.BACKLOGGERY.settingsKey(SyncCoordinator.CONNECTED), true)
        settings.putBoolean(ExternalService.BACKLOGGERY.settingsKey(SyncCoordinator.LIBRARY_SYNC), true)
        val connectors = mapOf<ExternalService, ServiceConnector>(ExternalService.BACKLOGGERY to BackloggeryConnector())
        val host = object : BackgroundWebHost {
            override suspend fun <T> withSession(block: suspend (WebSession) -> T): T = block(StuckSession)
        }
        val runner = SyncRunner(connectors, settings, library, tracker, matcher)
        return SyncCoordinator(settings, runner, history, host, LibraryChanges())
    }

    "cancelling the current sync records it and leaves a queued one skipped from running" {
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val history = SyncHistory(testSyncStorage(dispatcher))
            val coordinator = coordinator(history, dispatcher)
            val first = async { coordinator.runDue(SyncTrigger.PERIODIC, wishlist = false) }
            val second = async { coordinator.runDue(SyncTrigger.CHANGE, wishlist = false) }
            runCurrent()
            coordinator.queue.value.current?.service shouldBe ExternalService.BACKLOGGERY
            (coordinator.queue.value.done + 1 to coordinator.queue.value.total) shouldBe (1 to 2)

            coordinator.skip(coordinator.queue.value.waiting.single())
            coordinator.cancelCurrent()

            first.await() shouldBe emptyList()
            second.await() shouldBe emptyList()
            history.runs.first().map { it.result } shouldBe listOf(SyncResult.CANCELLED)
            coordinator.queue.value shouldBe SyncQueue()
        }
    }

    "a background sync the system stops is recorded as interrupted" {
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val history = SyncHistory(testSyncStorage(dispatcher))
            val coordinator = coordinator(history, dispatcher)
            val run = async { coordinator.runDue(SyncTrigger.PERIODIC, wishlist = false) }
            runCurrent()
            run.cancel()
            runCurrent()
            history.runs.first().map { it.result } shouldBe listOf(SyncResult.INTERRUPTED)
            coordinator.queue.value shouldBe SyncQueue()
        }
    }

    "cancelling a manual sync returns no outcome and records it as cancelled" {
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val history = SyncHistory(testSyncStorage(dispatcher))
            val coordinator = coordinator(history, dispatcher)
            val run = async { coordinator.runManual(ExternalService.BACKLOGGERY, SyncKind.LIBRARY, StuckSession) }
            runCurrent()
            coordinator.running.value shouldBe setOf(ExternalService.BACKLOGGERY)
            coordinator.cancelManual(ExternalService.BACKLOGGERY)
            run.await() shouldBe null
            history.runs.first().map { it.result } shouldBe listOf(SyncResult.CANCELLED)
        }
    }
})
