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
        val response = JsonApiSingleResponse.fromJsonApiString(json)
        val data = response.data
        val migrated = if (data?.type == "user") response.copy(data = data.copy(type = User.resourceType)) else response
        val user = User()
        user.fromJsonApiResponse(migrated)
        if (migrated !== response) saveUser(user)
        return user
    }

    fun clear() {
        settings.remove(USER_KEY)
    }
}

