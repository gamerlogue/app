package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.maicol07.gamerlogue.data.User

abstract class BaseAuthTokenProvider : AuthTokenProvider {

    final override var accessToken by mutableStateOf<String?>(null)
        private set
    final override var currentUserId by mutableStateOf<String?>(null)
        private set
    final override var currentUser by mutableStateOf<User?>(null)

    protected abstract fun loadToken(): String?
    protected abstract fun saveToken(token: String?)
    protected abstract fun loadUserId(): String?
    protected abstract fun saveUserId(userId: String?)

    override fun updateToken(token: String?) {
        saveToken(token)
        this.accessToken = token
    }

    override fun updateUserId(userId: String?) {
        saveUserId(userId)
        this.currentUserId = userId
    }

    open fun restore() {
        this.accessToken = loadToken()
        this.currentUserId = loadUserId()
    }
}

