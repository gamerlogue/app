package it.maicol07.gamerlogue.auth

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

class JvmAuthenticationHandler(
    authProvider: AuthTokenProvider,
    authClient: HttpClient,
) : NativeAuthenticationHandler(authProvider, authClient) {
    private val loginServer = LoopbackAuthServer(::exchangeCallback)

    override fun launchLogin(attempt: PkceLoginAttempt) {
        try {
            val port = loginServer.start(attempt)
            val authUrl = buildAuthUrl("http://localhost:$port/callback", attempt)

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            } else {
                Logger.e { "No desktop browser available to open the authentication URL" }
                loginServer.stop()
            }
        } catch (e: IOException) {
            Logger.e(e) { "Error starting authentication server" }
            loginServer.stop()
        } catch (e: URISyntaxException) {
            Logger.e(e) { "Invalid authentication URL" }
            loginServer.stop()
        }
    }
}
