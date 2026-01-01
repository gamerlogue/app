import it.maicol07.gamerlogue.auth.AuthState
import it.maicol07.gamerlogue.auth.AuthTokenProvider

class WasmAuthTokenProvider : AuthTokenProvider {
    override fun getToken(): String? = null
    override fun setToken(token: String?) {
        AuthState.token = token
    }
}
