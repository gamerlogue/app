package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.maicol07.gamerlogue.data.User

abstract class AuthTokenProvider {
    var accessToken by mutableStateOf<String?>(null)
        private set
    var currentUserId by mutableStateOf<String?>(null)
        private set
    var currentUser by mutableStateOf<User?>(null)

    protected abstract fun loadToken(): String?
    protected abstract fun saveToken(token: String?)
    protected abstract fun loadUserId(): String?
    protected abstract fun saveUserId(userId: String?)

    fun updateToken(token: String?) {
        saveToken(token)
        accessToken = token
    }

    fun updateUserId(userId: String?) {
        saveUserId(userId)
        currentUserId = userId
    }

    open fun restore() {
        accessToken = loadToken()
        currentUserId = loadUserId()
    }
}
