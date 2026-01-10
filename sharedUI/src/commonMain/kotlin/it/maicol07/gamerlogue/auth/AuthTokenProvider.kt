package it.maicol07.gamerlogue.auth

import it.maicol07.gamerlogue.data.User

// Common expect/actual provider for auth token
interface AuthTokenProvider {
    val accessToken: String?
    val currentUserId: String?
    var currentUser: User?

    fun updateToken(token: String?)
    fun updateUserId(userId: String?)
}
