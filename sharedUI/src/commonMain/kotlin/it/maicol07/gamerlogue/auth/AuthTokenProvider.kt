package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import it.maicol07.gamerlogue.data.User

// Common expect/actual provider for auth token
interface AuthTokenProvider {
    fun getToken(): String?
    fun setToken(token: String?)
    fun getUserId(): String?
    fun setUserId(userId: String?)
}

val LocalAuthTokenProvider = staticCompositionLocalOf<AuthTokenProvider> {
    error("No AuthTokenProvider provided")
}

// Simple in-memory state in common; platform actual persists
object AuthState {
    var token by mutableStateOf<String?>(null)
    var userId by mutableStateOf<String?>(null)
    var currentUser by mutableStateOf<User?>(null)
}
