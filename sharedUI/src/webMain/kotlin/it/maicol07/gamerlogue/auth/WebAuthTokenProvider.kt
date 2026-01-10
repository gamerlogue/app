package it.maicol07.gamerlogue.auth

class WebAuthTokenProvider : BaseAuthTokenProvider() {
    override fun loadToken(): String? = null
    override fun saveToken(token: String?) {
        // No persistence
    }
    override fun loadUserId(): String? = null
    override fun saveUserId(userId: String?) {
        // No persistence
    }
}
