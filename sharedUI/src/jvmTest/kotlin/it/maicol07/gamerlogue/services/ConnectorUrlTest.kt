package it.maicol07.gamerlogue.services

import at.released.igdbclient.model.Game
import at.released.igdbclient.model.Platform
import at.released.igdbclient.model.PlatformFamily
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.services.connectors.EpicConnector
import it.maicol07.gamerlogue.services.connectors.GogConnector
import it.maicol07.gamerlogue.services.connectors.NintendoConnector
import it.maicol07.gamerlogue.services.connectors.SteamConnector

/**
 * The store-URL regexes are load-bearing and fail silently: [ServiceConnector.uidFromUrl] is what
 * turns an IGDB `external_games`/`websites` URL back into a store id for the wishlist push, so when a
 * store changes its URL shape the push just stops finding anything — no error, no log.
 */
class ConnectorUrlTest : StringSpec({
    "Steam round-trips an appid through its store URL" {
        val steam = SteamConnector()
        val url = steam.storeUrl("440")
        url shouldBe "https://store.steampowered.com/app/440"
        steam.uidFromUrl(url!!) shouldBe "440"
    }

    "a connector with no URL template builds no store URL" {
        GogConnector().storeUrl("1207658924").shouldBeNull()
    }

    "GOG recognises a store page by its slug, with or without a locale segment" {
        val gog = GogConnector()
        gog.uidFromUrl("https://www.gog.com/game/baldurs_gate_3") shouldBe "baldurs_gate_3"
        gog.uidFromUrl("https://www.gog.com/en/game/baldurs_gate_3") shouldBe "baldurs_gate_3"
        gog.uidFromUrl("https://www.gog.com/news/some_article").shouldBeNull()
    }

    "Epic recognises both the /p/ and /product/ page shapes" {
        val epic = EpicConnector(EpicApi(io.ktor.client.HttpClient()))
        epic.uidFromUrl("https://store.epicgames.com/en-US/p/alan-wake-2") shouldBe "alan-wake-2"
        epic.uidFromUrl("https://www.epicgames.com/store/product/alan-wake-2") shouldBe "alan-wake-2"
        epic.uidFromUrl("https://store.epicgames.com/en-US/browse").shouldBeNull()
    }

    "Epic drops the locale segment before opening a product page" {
        EpicConnector(EpicApi(io.ktor.client.HttpClient()))
            .normalizePushUrl("https://store.epicgames.com/en-US/p/alan-wake-2") shouldBe
            "https://store.epicgames.com/p/alan-wake-2"
    }

    "Nintendo reads the numeric title id out of an eShop URL" {
        val nintendo = NintendoConnector()
        nintendo.uidFromUrl("https://ec.nintendo.com/titles/70010000000026") shouldBe "70010000000026"
        nintendo.uidFromUrl("https://www.nintendo.com/store/products/zelda-switch").shouldBeNull()
    }

    "a PC store keeps only the game's PC platforms" {
        val game = game(
            platform(id = 6), // PC (Windows)
            platform(id = 3), // Linux
            platform(id = 48, family = 1), // PS4
            platform(id = 34), // Android — no family, but not a PC platform either
        )
        // Steam has no platform family: PC stores use the explicit PC allowlist instead.
        SteamConnector().platformIdsFor(game) shouldContainExactlyInAnyOrder listOf(6, 3)
    }

    "a console store keeps only the platforms of its own family" {
        val game = game(platform(id = 6), platform(id = 48, family = 1), platform(id = 167, family = 1))
        val nintendo = NintendoConnector() // platformFamily = 5
        nintendo.platformIdsFor(game) shouldBe emptyList()

        val playstationFamily = object : ServiceConnector(ExternalService.PLAYSTATION, "x", 0) {
            override val platformFamily = 1
            override val ownedGames = webRefs(WebStep("", ""))
        }
        playstationFamily.platformIdsFor(game) shouldContainExactlyInAnyOrder listOf(48, 167)
    }

    "a profile needs a username to count as one" {
        parseProfileJson("""{"username":"maicol","avatarUrl":"https://x/a.png"}""")
            ?.username shouldBe "maicol"
        parseProfileJson("""{"username":""}""").shouldBeNull()
        parseProfileJson("[]").shouldBeNull()
        parseProfileJson(null).shouldBeNull()
    }
})

private fun platform(id: Int, family: Int? = null) =
    Platform(id = id.toLong(), platform_family = family?.let { PlatformFamily(id = it.toLong()) })

private fun game(vararg platforms: Platform) = Game(id = 1L, platforms = platforms.toList())
