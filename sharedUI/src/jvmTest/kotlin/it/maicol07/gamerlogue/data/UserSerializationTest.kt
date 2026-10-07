package it.maicol07.gamerlogue.data

import com.russhwolf.settings.PreferencesSettings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import it.maicol07.spraypaintkt.JsonApiResource
import it.maicol07.spraypaintkt.JsonApiSingleResponse
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import java.util.prefs.Preferences

class UserSerializationTest : StringSpec({
    "user accepts the backend JSON API resource type" {
        val user = User()
        user.fromJsonApi(
            JsonApiResource(
                id = "42",
                type = "User",
                attributes = mapOf("nickname" to JsonPrimitive("player"))
            )
        )

        user.id shouldBe "42"
        user.nickname shouldBe "player"
        User.endpoint shouldBe "users"
    }

    "user cache migrates the legacy type without losing the profile" {
        val preferences = Preferences.userRoot().node("gamerlogue-tests/${UUID.randomUUID()}")
        try {
            val settings = PreferencesSettings(preferences)
            val store = UserStore(settings)
            store.getUser().shouldBeNull()
            settings.putString(
                "user_data",
                """{"data":{"type":"user","id":"42","attributes":{"nickname":"player"}}}"""
            )

            val user = store.getUser()!!
            user.id shouldBe "42"
            user.nickname shouldBe "player"
            JsonApiSingleResponse.fromJsonApiString(settings.getString("user_data", "")).data?.type shouldBe "User"
            store.getUser()?.nickname shouldBe "player"

            settings.putString("user_data", """{"data":{"type":"Other","id":"42"}}""")
            shouldThrow<SerializationException> { store.getUser() }
        } finally {
            preferences.removeNode()
        }
    }
})
