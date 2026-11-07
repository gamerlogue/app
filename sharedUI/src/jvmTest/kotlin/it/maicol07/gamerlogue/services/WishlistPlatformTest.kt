package it.maicol07.gamerlogue.services

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class WishlistPlatformTest : StringSpec({
    val game = LibrarySync.OutgoingGame(
        42, "42", "Test game", null, null,
        platformNames = mapOf(6 to listOf("PC", "PC"), 167 to listOf("PlayStation 5", "PS5")),
    )

    "a game on multiple platforms requires an explicit choice" {
        game.defaultPlatformId shouldBe null
    }

    "the platform already chosen in Gamerlogue is retained" {
        game.copy(platformIds = listOf(167)).defaultPlatformId shouldBe 167
    }

    "a game with only one platform can be preselected" {
        game.copy(platformNames = mapOf(6 to listOf("PC"))).defaultPlatformId shouldBe 6
    }

    "an unavailable saved platform does not silently choose another platform" {
        game.copy(platformIds = listOf(999)).defaultPlatformId shouldBe null
    }

    "a game without platform metadata requires metadata before it can be sent" {
        game.copy(platformNames = emptyMap()).defaultPlatformId shouldBe null
        game.copy(platformNames = mapOf(6 to emptyList())).defaultPlatformId shouldBe null
    }

    "a preferred platform is preselected only where the game releases and has no library platform" {
        game.preselectedPlatformId(listOf(48, 167, 6)) shouldBe 167
        game.copy(platformIds = listOf(6)).preselectedPlatformId(listOf(167)) shouldBe 6
        game.copy(platformIds = listOf(999)).preselectedPlatformId(listOf(167)) shouldBe null
        game.preselectedPlatformId(listOf(48)) shouldBe null
    }

    "confirmed platforms rank by use ahead of the previous preference" {
        val pushed = listOf(6, 167, 167).map { game.copy(platformIds = listOf(it)) }
        LibrarySync.OutgoingGame.rankPlatforms(pushed, listOf(6, 48)) shouldBe listOf(167, 6, 48)
    }
})
