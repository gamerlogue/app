package it.maicol07.gamerlogue.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus

class WebResultTest : StringSpec({
    "failed writes preserve confirmations before the failure" {
        val error = shouldThrow<ScriptWriteException> {
            parseWriteResult("""{"error":"HTTP 429","written":[{"uid":"10","gameId":1}]}""")
        }
        error.message shouldBe "HTTP 429"
        error.written.single().uid shouldBe "10"
        error.written.single().gameId shouldBe 1
    }

    "a missing write result is a failure, not an empty successful push" {
        shouldThrow<ScriptWriteException> { parseWriteResult(null) }
    }

    "successful writes still accept a quoted bridge result" {
        parseWriteResult("\"[{\\\"uid\\\":\\\"10\\\"}]\"").single().uid shouldBe "10"
    }

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

    "reads a tracker status, and an unknown one becomes null instead of dropping the list" {
        val refs = parseRefsJson("""[{"uid":"1","status":"PLAYING"},{"uid":"2","status":"wishlisted"}]""")
        refs.map { it.status } shouldContainExactly listOf(GameLibraryStatus.PLAYING, null)
    }

    "status writes reach scripts with every field, defaults included" {
        val json = listOf(StatusChange(1, "Game", uid = null, from = null, to = GameLibraryStatus.PLAYING)).jsonArray()
        json.contains("\"platformIds\":[]") shouldBe true
    }

    "a blank avatar or profile page from a script means none" {
        val profile = parseProfileJson("""{"username":"me","avatarUrl":"","profileUrl":" "}""")
        profile?.avatarUrl shouldBe null
        profile?.profileUrl shouldBe null
    }

    "a bridge reply is unwrapped only for the current step token" {
        unwrapBridgeReply("""{"token":3,"out":[{"uid":"1"}]}""", 3) shouldBe """[{"uid":"1"}]"""
        unwrapBridgeReply("""{"token":2,"out":[]}""", 3) shouldBe null
        unwrapBridgeReply("""{"out":[]}""", 3) shouldBe null
        unwrapBridgeReply("[]", 3) shouldBe null
        unwrapBridgeReply(null, 3) shouldBe null
    }

    "a successful empty array is an empty read" {
        parseRefsJson("[]") shouldBe emptyList()
    }

    "a missing, empty or unparseable read result is a failure, never an empty library" {
        shouldThrow<ServiceReadException> { parseRefsJson(null) }
        shouldThrow<ServiceReadException> { parseRefsJson("") }
        shouldThrow<ServiceReadException> { parseRefsJson("not json") }
    }

    "an error payload from the script fails the read with its message" {
        val error = shouldThrow<ServiceReadException> { parseRefsJson("""{"error":"HTTP 503","written":[]}""") }
        error.message shouldBe "HTTP 503"
        shouldThrow<ServiceReadException> { parseRefsJson("""{"unexpected":true}""") }
    }

    "one malformed ref fails the whole read instead of yielding a partial list" {
        shouldThrow<ServiceReadException> { parseRefsJson("""[{"uid":"1"},{"name":"no uid"}]""") }
    }

    "an unexpected write payload becomes a write exception with nothing confirmed" {
        shouldThrow<ScriptWriteException> { parseWriteResult("not json") }.written shouldBe emptyList()
        shouldThrow<ScriptWriteException> { parseWriteResult("""{"unexpected":true}""") }.written shouldBe emptyList()
        shouldThrow<ScriptWriteException> { parseWriteResult("""[{"name":"no uid"}]""") }.written shouldBe emptyList()
    }

    "a failed write with a malformed written list keeps the error and confirms nothing" {
        val error = shouldThrow<ScriptWriteException> { parseWriteResult("""{"error":"boom","written":[{"name":"x"}]}""") }
        error.message shouldBe "boom"
        error.written shouldBe emptyList()
    }

    "a successful empty write is an empty list" {
        parseWriteResult("[]") shouldBe emptyList()
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
