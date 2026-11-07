package it.maicol07.gamerlogue.services

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.IgdbResult
import at.released.igdbclient.IgdbWebhookApi
import at.released.igdbclient.apicalypse.ApicalypseQuery
import at.released.igdbclient.error.IgdbHttpErrorResponse
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameResult
import at.released.igdbclient.model.GameType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.services.connectors.BackloggdConnector
import okio.Buffer

private class CategoryFixtureIgdb : IgdbClient {
    override val webhookApi: IgdbWebhookApi get() = error("Unexpected webhook")
    private val base = Game(id = 1, name = "Base", game_type = GameType(id = 0))
    private val games = listOf(
        base,
        Game(id = 2, name = "Edition", version_parent = base),
        Game(id = 3, game_type = GameType(id = 1)),
        Game(id = 4, game_type = GameType(id = 3)),
        Game(id = 5, game_type = GameType(id = 13)),
        Game(id = 6, game_type = GameType(id = 14)),
        Game(id = 7, game_type = GameType(id = 2)),
    )

    override suspend fun <T : Any> execute(
        endpoint: IgdbEndpoint<T>,
        query: ApicalypseQuery,
    ): IgdbResult<T, IgdbHttpErrorResponse> {
        endpoint shouldBe IgdbEndpoint.GAME
        return IgdbResult.Success(
            endpoint.resultParser(query, Buffer().write(GameResult.ADAPTER.encode(GameResult(games = games))))
        )
    }
}

class GameMatcherCategoryTest : StringSpec({
    "identity matches keep editions, DLCs and expansions but reject bundles, packs and updates" {
        val matcher = GameMatcher(CategoryFixtureIgdb(), ExceptionReporter())
        val matches = matcher.match(BackloggdConnector(), (1..7).map { ExternalGameRef(it.toString()) })
        matches.filter { it.confident }.map { it.ref.uid } shouldBe listOf("1", "2", "3", "7")
        matches.first { it.ref.uid == "2" }.game?.version_parent?.id shouldBe 1L
        matches.filter { !it.confident }.map { it.ref.uid } shouldBe listOf("4", "5", "6")
    }

    "name candidates follow the same exclusions as GameList, including editions" {
        val matcher = GameMatcher(CategoryFixtureIgdb(), ExceptionReporter())
        matcher.searchByName("Base").map { it.id } shouldBe listOf(1L, 7L)
    }

    "lookups by id keep add-ons and bundles: those ids come from the library, not from matching" {
        val matcher = GameMatcher(CategoryFixtureIgdb(), ExceptionReporter())
        matcher.gamesByIds(listOf(3, 4, 5, 6)).map { it.id } shouldBe (1L..7L).toList()
    }
})
