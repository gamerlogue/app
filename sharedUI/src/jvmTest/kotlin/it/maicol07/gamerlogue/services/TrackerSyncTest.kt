package it.maicol07.gamerlogue.services

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.IgdbResult
import at.released.igdbclient.IgdbWebhookApi
import at.released.igdbclient.apicalypse.ApicalypseQuery
import at.released.igdbclient.error.IgdbHttpErrorResponse
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameResult
import com.russhwolf.settings.PreferencesSettings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.services.connectors.BackloggdConnector
import it.maicol07.gamerlogue.ui.components.SyncPhase
import it.maicol07.gamerlogue.ui.components.WebSession
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.fakefilesystem.FakeFileSystem
import java.util.UUID
import java.util.prefs.Preferences

/** Whatever game is asked for, IGDB answers with game 99 only. */
private class OtherGameIgdb : IgdbClient {
    override val webhookApi: IgdbWebhookApi get() = error("No IGDB webhooks expected")
    override suspend fun <T : Any> execute(
        endpoint: IgdbEndpoint<T>,
        query: ApicalypseQuery,
    ): IgdbResult<T, IgdbHttpErrorResponse> =
        IgdbResult.Success(
            endpoint.resultParser(
                query,
                Buffer().write(GameResult.ADAPTER.encode(GameResult(games = listOf(Game(id = 99)))))
            ),
        )
}

class TrackerSyncTest : StringSpec({
    fun trackerSync(storage: SyncStorage = testSyncStorage()): TrackerSync {
        val reporter = ExceptionReporter()
        val matcher = GameMatcher(OtherGameIgdb(), reporter, MatchMemory(testSyncStorage()))
        return TrackerSync(
            storage,
            matcher,
            LibrarySync(
                matcher,
                reporter,
                LibraryChanges(),
                ProfileApi(),
                WishlistSyncState(testSyncStorage()),
            )
        )
    }

    "a pulled change whose game IGDB doesn't return is skipped and not recorded" {
        val tracker = trackerSync()
        val change = SyncChange(5, "u", null, GameLibraryStatus.BACKLOG, SyncSide.REMOTE)
        tracker.pull(ExternalService.BACKLOGGD, listOf(change), mutableListOf()) shouldBe 0
        tracker.base(ExternalService.BACKLOGGD).isEmpty() shouldBe true
    }

    "a game on the tracker under an unconfirmed row is never added to it" {
        runTest {
            val tracker = trackerSync()
            val entry = LibraryEntry().apply {
                id = "1"
                isPersisted = true
                gameId = 1
                status = GameLibraryStatus.PLAYING
                owned = true
            }
            val connector = BackloggdConnector()
            tracker.plan(connector, emptyMap(), listOf(entry)).changes.map { it.gameId } shouldBe listOf(1)
            tracker.plan(connector, emptyMap(), listOf(entry), onTracker = setOf(1)).changes.shouldBeEmpty()
        }
    }

    // A settings value held 8 KB on the desktop: a library this size never got stored there.
    "the state of a large library survives a restart, per service" {
        val fileSystem = FakeFileSystem()
        val directory = testSyncDirectory()
        val games = (1..5_000).associateWith { SyncedGame("uid-$it", GameLibraryStatus.BACKLOG) }
        withSyncStorage(fileSystem, directory) { storage ->
            val tracker = trackerSync(storage)
            tracker.record(ExternalService.BACKLOGGD, games)
            tracker.record(ExternalService.GROUVEE, mapOf(1 to SyncedGame("g1", GameLibraryStatus.PLAYING)))
            tracker.prune(ExternalService.BACKLOGGD, setOf(1, 2))
            tracker.forget(ExternalService.GROUVEE)
        }

        withSyncStorage(fileSystem, directory) { storage ->
            val tracker = trackerSync(storage)
            tracker.base(ExternalService.BACKLOGGD) shouldBe games - setOf(1, 2)
            tracker.base(ExternalService.GROUVEE) shouldBe emptyMap()
        }
    }

    "a tracker that held games at the last sync and now reads none fails instead of looking emptied" {
        runTest {
            val preferences = Preferences.userRoot().node("gamerlogue-tests/${UUID.randomUUID()}")
            try {
                val settings = PreferencesSettings(preferences)
                val reporter = ExceptionReporter()
                val matcher = GameMatcher(OtherGameIgdb(), reporter, MatchMemory(testSyncStorage()))
                val library = LibrarySync(
                    matcher,
                    reporter,
                    LibraryChanges(),
                    ProfileApi(),
                    WishlistSyncState(testSyncStorage()),
                )
                val tracker = TrackerSync(testSyncStorage(), matcher, library)
                tracker.record(ExternalService.BACKLOGGD, mapOf(1 to SyncedGame("u1", GameLibraryStatus.PLAYING)))
                val connector = BackloggdConnector()
                val runner = SyncRunner(mapOf(connector.service to connector), settings, library, tracker, matcher)
                val readsNothing = object : WebSession {
                    override val currentUrl: String = "https://backloggd.com/"
                    override suspend fun awaitLogin(connector: ServiceConnector) = Unit
                    override fun reportProgress(done: Int, total: Int) = Unit
                    override suspend fun run(step: WebStep): List<ExternalGameRef> = error("No writes expected")
                    override suspend fun <T> read(source: DataSource<T>, phase: SyncPhase): T = when (source) {
                        is DataSource.Web -> source.parse("[]")
                        is DataSource.Api -> error("No API reads expected")
                    }

                    override suspend fun confirmPush(games: List<LibrarySync.OutgoingGame>) =
                        error("No confirmations expected")
                }

                shouldThrow<ServiceReadException> { runner.syncLibrary(ExternalService.BACKLOGGD, readsNothing) }
            } finally {
                preferences.removeNode()
            }
        }
    }
})
