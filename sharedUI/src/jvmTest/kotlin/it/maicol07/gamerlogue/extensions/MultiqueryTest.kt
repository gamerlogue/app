package it.maicol07.gamerlogue.extensions

import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameTimeToBeat
import at.released.igdbclient.model.UnpackedMultiQueryResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MultiqueryTest : StringSpec({
    val responses = listOf(UnpackedMultiQueryResult(name = "game", results = listOf(Game(id = 1L))))

    "returns results of the requested type" {
        responses.multiqueryResults<Game>("game").map { it.id } shouldBe listOf(1L)
    }

    "rejects results of a different type" {
        shouldThrow<IllegalStateException> {
            responses.multiqueryResults<GameTimeToBeat>("game")
        }
    }
})
