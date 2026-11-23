package it.maicol07.gamerlogue.services

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NintendoApiTest : StringSpec({
    val body = """
        {"response":{"docs":[
          {"title":"Graveyard Keeper","nsuid_txt":["70010000013396"]},
          {"title":"Core Keeper – Nintendo Switch™ 2 Edition","nsuid_txt":["70010000115436"]},
          {"title":"Core Keeper™","nsuid_txt":["70010000073034"]}
        ]}}
    """.trimIndent()

    "only a title equal once symbols and case are set aside matches" {
        parseTitleId(body, "core keeper") shouldBe "70010000073034"
    }

    "a near name matches nothing" {
        parseTitleId(body, "Keeper") shouldBe null
    }
})
