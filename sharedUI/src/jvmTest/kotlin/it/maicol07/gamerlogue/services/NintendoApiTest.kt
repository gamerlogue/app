package it.maicol07.gamerlogue.services

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NintendoApiTest : StringSpec({
    val body = """
        {"response":{"docs":[
          {"title":"Graveyard Keeper","nsuid_txt":["70010000013396"]},
          {"title":"Core Keeper – Nintendo Switch™ 2 Edition","nsuid_txt":["70010000115436"]},
          {"title":"Core Keeper™","nsuid_txt":["70010000073034"]},
          {"title":"Xenoblade Chronicles X: Definitive Edition","nsuid_txt":["70010000088833"]},
          {"title":"Pokémon Legends: Z-A","nsuid_txt":["70010000094235"]},
          {"title":"Hades II","nsuid_txt":["70010000096609"]}
        ]}}
    """.trimIndent()

    "only a title equal once symbols and case are set aside matches" {
        parseTitleId(body, "core keeper") shouldBe "70010000073034"
    }

    "a word fewer matches nothing" {
        parseTitleId(body, "Keeper") shouldBe null
    }

    "a title an accent or typo away matches" {
        parseTitleId(body, "Pokemon Legends: Z-A") shouldBe "70010000094235"
    }

    "a word or number more is another game" {
        parseTitleId(body, "Xenoblade Chronicles: Definitive Edition") shouldBe null
        parseTitleId(body, "Hades") shouldBe null
        parseTitleId(body, "Hades III") shouldBe null
    }
})
