package it.maicol07.gamerlogue.services

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class WebResultTest : StringSpec({
    "parses a plain JSON ref array" {
        val refs = parseRefsJson("""[{"uid":"10","name":"A"},{"uid":"20","name":"B"}]""")
        refs shouldContainExactly listOf(ExternalGameRef("10", "A"), ExternalGameRef("20", "B"))
    }

    "unwraps a JSON-string-wrapped array (double-encoded transport)" {
        val refs = parseRefsJson("\"[{\\\"uid\\\":\\\"10\\\",\\\"name\\\":\\\"A\\\"}]\"")
        refs shouldContainExactly listOf(ExternalGameRef("10", "A"))
    }

    "tolerates unknown fields and missing names" {
        val refs = parseRefsJson("""[{"uid":"5","extra":true}]""")
        refs.single().uid shouldBe "5"
    }

    "null, empty and malformed input yield no refs" {
        parseRefsJson(null) shouldBe emptyList()
        parseRefsJson("") shouldBe emptyList()
        parseRefsJson("not json") shouldBe emptyList()
    }
})
