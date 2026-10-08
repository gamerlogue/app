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

    "reads a credential delivered as a quoted string" {
        parseCredentialJson("\"npsso-token\"") shouldBe "npsso-token"
    }

    "reads a credential whose own payload is JSON (Ubisoft's ticket + session id)" {
        val json = """{"ticket":"t","sessionId":"s"}"""
        // The bridge quotes and escapes it on the way out, as it does for any string.
        parseCredentialJson("\"{\\\"ticket\\\":\\\"t\\\",\\\"sessionId\\\":\\\"s\\\"}\"") shouldBe json
    }

    "a missing credential is blank, not the wrapper's empty-array sentinel" {
        parseCredentialJson("[]") shouldBe ""
        parseCredentialJson(null) shouldBe ""
        parseCredentialJson("") shouldBe ""
    }
})
