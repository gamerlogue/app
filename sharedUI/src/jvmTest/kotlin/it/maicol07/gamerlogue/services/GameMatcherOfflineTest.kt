package it.maicol07.gamerlogue.services

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.IgdbResult
import at.released.igdbclient.IgdbWebhookApi
import at.released.igdbclient.apicalypse.ApicalypseQuery
import at.released.igdbclient.error.IgdbHttpErrorResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.services.connectors.BackloggeryConnector
import java.io.IOException

/** An IGDB server that can't be reached. */
private class UnreachableIgdb : IgdbClient {
    override val webhookApi: IgdbWebhookApi get() = error("No webhook requests expected")

    override suspend fun <T : Any> execute(
        endpoint: IgdbEndpoint<T>,
        query: ApicalypseQuery,
    ): IgdbResult<T, IgdbHttpErrorResponse> = throw IOException("connection refused")
}

class GameMatcherOfflineTest : StringSpec({
    "an IGDB that doesn't answer fails the match instead of reporting games with no match" {
        val matcher = GameMatcher(UnreachableIgdb(), ExceptionReporter(), MatchMemory(testSyncStorage()))
        shouldThrow<IgdbUnavailableException> {
            matcher.match(BackloggeryConnector(), listOf(ExternalGameRef("1", "Hades")))
        }
    }
})
