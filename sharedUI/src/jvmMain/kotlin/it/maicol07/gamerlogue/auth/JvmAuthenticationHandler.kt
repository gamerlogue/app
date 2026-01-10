package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import com.sun.net.httpserver.HttpServer
import io.ktor.http.HttpStatusCode
import org.koin.compose.koinInject
import java.awt.Desktop
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.util.concurrent.Executors

private const val ServerStopDelay = 1000L

class JvmAuthenticationHandler(authProvider: AuthTokenProvider) : AuthenticationHandler(authProvider) {
    override fun login() {
        try {
            val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
            val port = server.address.port

            server.createContext("/callback") { exchange ->
                try {
                    var response: String
                    var responseCode: HttpStatusCode
                    val query = exchange.requestURI.query
                    if (query != null) {
                        val success = handleCallback(query) {
                            try {
                                URLDecoder.decode(it, "UTF-8")
                            } catch (e: UnsupportedEncodingException) {
                                Logger.e(e) { "Error decoding callback query parameter" }
                                null
                            }
                        }

                        response = if (success) "Login successful! You can close this window." else "Login failed! Token not found."
                        responseCode = if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest
                    } else {
                        response = "Login failed! Token not found."
                        responseCode = HttpStatusCode.BadRequest
                    }
                    exchange.sendResponseHeaders(responseCode.value, response.length.toLong())
                    exchange.responseBody.use { it.write(response.toByteArray()) }
                } catch (e: IOException) {
                    Logger.e(e) { "Error handling authentication callback" }
                } finally {
                    // Stop server after a short delay
                    Thread {
                        try {
                            Thread.sleep(ServerStopDelay)
                            server.stop(0)
                        } catch (_: InterruptedException) {
                            // Ignore
                        }
                    }.start()
                }
            }

            server.executor = Executors.newSingleThreadExecutor()
            server.start()

            val redirectUri = "http://localhost:$port/callback"
            // Assuming the auth server accepts redirect_uri parameter.
            // If not, and it forces deep link, this won't work without OS registration.
            val authUrl = getAuthUrl(redirectUri)

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            }
        } catch (e: IOException) {
            Logger.e(e) { "Error starting authentication server" }
        } catch (e: URISyntaxException) {
            Logger.e(e) { "Invalid authentication URL" }
        }
    }
}

@Composable
actual fun rememberAuthenticationHandler(): AuthenticationHandler {
    val authProvider = koinInject<AuthTokenProvider>()
    return remember { JvmAuthenticationHandler(authProvider) }
}
