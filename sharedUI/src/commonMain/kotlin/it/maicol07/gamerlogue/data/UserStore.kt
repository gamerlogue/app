package it.maicol07.gamerlogue.data

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import it.maicol07.spraypaintkt.JsonApiSingleResponse
import org.koin.core.annotation.Single

@Single
class UserStore(private val settings: ObservableSettings) {
    companion object {
        private const val USER_KEY = "user_data"
    }

    fun saveUser(user: User) {
        val json = user.toJsonApiString()
        settings[USER_KEY] = json
    }

    fun getUser(): User? {
        val json = settings.getStringOrNull(USER_KEY) ?: return null
        val user = User()
        user.fromJsonApiResponse(JsonApiSingleResponse.fromJsonApiString(json))
        return user
    }

    fun clear() {
        settings.remove(USER_KEY)
    }
}

