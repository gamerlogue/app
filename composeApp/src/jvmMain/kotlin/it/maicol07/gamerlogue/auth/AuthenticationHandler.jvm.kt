package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sun.net.httpserver.HttpServer
import it.maicol07.gamerlogue.BuildConfig
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.Executors

class JvmAuthenticationHandler : AuthenticationHandler {
    override fun login() {
        try {
            val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
            val port = server.address.port

            server.createContext("/callback") { exchange ->
                try {
                    val query = exchange.requestURI.query
                    val token = query?.split("&")
                        ?.find { it.startsWith("token=") }
                        ?.substringAfter("token=")
                    val userId = query?.split("&")
                        ?.find { it.startsWith("user_id=") }
                        ?.substringAfter("user_id=")

                    if (token != null && userId != null) {
                        val response = "Login successful! You can close this window."
                        exchange.sendResponseHeaders(200, response.length.toLong())
                        exchange.responseBody.use { it.write(response.toByteArray()) }

                        // Decode token if needed
                        val decodedToken = URLDecoder.decode(token, "UTF-8")

                        // Save token
                        val provider = JvmAuthTokenProvider()
                        provider.setToken(decodedToken)
                        provider.setUserId(userId)
                    } else {
                        val response = "Login failed! Token not found."
                        exchange.sendResponseHeaders(400, response.length.toLong())
                        exchange.responseBody.use { it.write(response.toByteArray()) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    // Stop server after a short delay
                    Thread {
                        try {
                            Thread.sleep(1000)
                            server.stop(0)
                        } catch (e: InterruptedException) {
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
            val authUrl = "${BuildConfig.GAMERLOGUE_URL}/sanctum/token?token_name=Gamerlogue&redirect_uri=$redirectUri"

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
actual fun rememberAuthenticationHandler(): AuthenticationHandler {
    return remember { JvmAuthenticationHandler() }
}
