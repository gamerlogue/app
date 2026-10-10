package it.maicol07.gamerlogue.ui.views.game.components

import at.released.igdbclient.model.Artwork
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameVideo
import at.released.igdbclient.model.Screenshot
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import okio.ByteString.Companion.decodeHex

/** `artwork_type { id: 7 }` (color logo) as IGDB encodes it, which igdbclient keeps as an unknown field. */
private val COLOR_LOGO_TYPE = "52020807".decodeHex()

class GameMediaTest : StringSpec({
    "viewer images exclude videos and logos and preserve the carousel image order" {
        val game = Game(
            videos = listOf(GameVideo(video_id = "video")),
            artworks = listOf(
                Artwork(image_id = "artwork"),
                Artwork(image_id = "logo", unknownFields = COLOR_LOGO_TYPE),
            ),
            screenshots = listOf(Screenshot(image_id = "first"), Screenshot(image_id = "second"))
        )

        gameMediaImageIds(game) shouldBe listOf("artwork", "first", "second")
        gameMediaImageIds(game.copy(videos = emptyList())) shouldBe gameMediaImageIds(game)
    }

    "video-only games have no viewer pages" {
        gameMediaImageIds(Game(videos = listOf(GameVideo(video_id = "video")))) shouldBe emptyList()
    }
    "website labels preserve alias matching and the domain fallback" {
        websiteInfo("https://store.steampowered.com/app/1").label shouldBe "Steam"
        websiteInfo("https://steam.com").label shouldBe "Steam"
        websiteInfo("https://xbox.com").label shouldBe "Xbox"
        websiteInfo("https://example.wikia.org").label shouldBe "Fandom"
        websiteInfo("https://www.example.org/game").label shouldBe "Example.org"
        websiteInfo("https://store.steampowered.com/app/1").isStore shouldBe true
        websiteInfo("https://reddit.com/r/game").isStore shouldBe false
    }
})
