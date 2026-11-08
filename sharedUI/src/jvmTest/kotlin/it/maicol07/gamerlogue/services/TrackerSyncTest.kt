package it.maicol07.gamerlogue.services

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.IgdbResult
import at.released.igdbclient.IgdbWebhookApi
import at.released.igdbclient.apicalypse.ApicalypseQuery
import at.released.igdbclient.error.IgdbHttpErrorResponse
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameResult
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.PreferencesSettings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.services.connectors.BackloggdConnector
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.test.runTest
import okio.Buffer
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
    fun trackerSync(preferences: Preferences): TrackerSync {
        val reporter = ExceptionReporter()
        val matcher = GameMatcher(OtherGameIgdb(), reporter, MatchMemory(MapSettings()))
        return TrackerSync(
            PreferencesSettings(preferences),
            matcher,
            LibrarySync(matcher, reporter, LibraryChanges(), ProfileApi())
        )
    }

    "a pulled change whose game IGDB doesn't return is skipped and not recorded" {
        val preferences = Preferences.userRoot().node("gamerlogue-tests/${UUID.randomUUID()}")
        try {
            val tracker = trackerSync(preferences)
            val change = SyncChange(5, "u", null, GameLibraryStatus.BACKLOG, SyncSide.REMOTE)
            tracker.pull(ExternalService.BACKLOGGD, listOf(change), mutableListOf()) shouldBe 0
            tracker.base(ExternalService.BACKLOGGD).isEmpty() shouldBe true
        } finally {
            preferences.removeNode()
        }
    }

    "a game on the tracker under an unconfirmed row is never added to it" {
        runTest {
            val preferences = Preferences.userRoot().node("gamerlogue-tests/${UUID.randomUUID()}")
            try {
                val tracker = trackerSync(preferences)
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
            } finally {
                preferences.removeNode()
            }
        }
    }
})
