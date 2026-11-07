package it.maicol07.gamerlogue.services

import at.released.igdbclient.model.Game
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class GameMatcherRankingTest : StringSpec({
    "name candidates put the closest title first, keeping IGDB's order on ties" {
        val candidates = listOf(
            Game(id = 1, name = "The Clown's Forest 3: Haunting Apparitions"),
            Game(id = 2, name = "Forest 2"),
            Game(id = 3, name = "FOREST 3"),
            Game(id = 4, name = "Forest 4"),
        )
        candidates.rankedBy("Forest 3™").map { it.id } shouldContainExactly listOf(3L, 2L, 4L, 1L)
    }
})
