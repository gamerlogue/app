package it.maicol07.gamerlogue.extensions

import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game_category__dlc
import gamerlogue.sharedui.generated.resources.game_category__pack_addon
import gamerlogue.sharedui.generated.resources.game_status__early_access
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.extensions.igdb.gameCategoryStringResource
import it.maicol07.gamerlogue.extensions.igdb.gameStatusStringResource

class GameMetadataNamesTest : StringSpec({
    "modern API labels and legacy enum names use the same translations" {
        gameStatusStringResource("Early Access") shouldBe Res.string.game_status__early_access
        gameStatusStringResource("EARLY_ACCESS") shouldBe Res.string.game_status__early_access
        gameCategoryStringResource("DLC / Addon") shouldBe Res.string.game_category__dlc
        gameCategoryStringResource("DLC_ADDON") shouldBe Res.string.game_category__dlc
        gameCategoryStringResource("Pack / Addon") shouldBe Res.string.game_category__pack_addon
        gameCategoryStringResource("PACK") shouldBe Res.string.game_category__pack_addon
        gameStatusStringResource("Future status") shouldBe null
        gameCategoryStringResource("Future type") shouldBe null
    }
})
