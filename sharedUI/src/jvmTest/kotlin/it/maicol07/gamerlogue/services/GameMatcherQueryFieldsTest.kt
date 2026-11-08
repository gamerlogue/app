package it.maicol07.gamerlogue.services

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.IgdbResult
import at.released.igdbclient.IgdbWebhookApi
import at.released.igdbclient.apicalypse.ApicalypseQuery
import at.released.igdbclient.error.IgdbHttpErrorResponse
import com.russhwolf.settings.MapSettings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.services.connectors.SteamConnector
import okio.Buffer

/** Records every query and answers it with an empty result. */
private class RecordingIgdb : IgdbClient {
    val queries = mutableListOf<String>()
    override val webhookApi: IgdbWebhookApi get() = error("No IGDB webhooks expected")
    override suspend fun <T : Any> execute(
        endpoint: IgdbEndpoint<T>,
        query: ApicalypseQuery,
    ): IgdbResult<T, IgdbHttpErrorResponse> {
        queries += query.toString()
        return IgdbResult.Success(endpoint.resultParser(query, Buffer()))
    }
}

private val FIELDS_CLAUSE = Regex("(?:^|;)f ([^;]*);")

/** igdbclient's `fields()` replaces the list, so a second call for the identity fields would wipe the first. */
class GameMatcherQueryFieldsTest : StringSpec({
    "every matcher query asks for its own fields together with the identity ones" {
        val igdb = RecordingIgdb()
        val matcher = GameMatcher(igdb, ExceptionReporter(), MatchMemory(MapSettings()))
        matcher.searchByName("zelda")
        matcher.gamesByIds(listOf(1))
        // A store-id lookup (external_games) and a by-slug lookup (games).
        matcher.match(
            SteamConnector(),
            listOf(ExternalGameRef("570"), ExternalGameRef("slugged", igdbSlug = "some-slug"))
        )

        igdb.queries.size shouldBe 4
        igdb.queries.forEach { query ->
            val fields = FIELDS_CLAUSE.find(query)!!.groupValues[1].split(",")
            // external_games rows nest the game under `game.`; its own fields include `url`.
            val prefix = if ("url" in fields) "game." else ""
            fields.shouldContainAll("${prefix}cover.image_id", "${prefix}game_type", "${prefix}version_parent.id")
        }
    }
})
