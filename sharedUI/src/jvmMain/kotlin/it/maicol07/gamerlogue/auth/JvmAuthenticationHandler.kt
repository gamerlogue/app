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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** How long the loopback listener waits for the browser before giving up. */
private const val LoginTimeoutMinutes = 5L

/** Grace period so the success page reaches the browser before the listener goes down. */
private const val ServerStopDelaySeconds = 1L

class JvmAuthenticationHandler(authProvider: AuthTokenProvider) : AuthenticationHandler(authProvider) {
    /**
     * The in-flight login, if any. Everything about the loopback listener is torn down through
     * [stopFlow]: the previous implementation stopped the server only from inside the request
     * handler, so abandoning the login left the listener — and its executor's non-daemon thread —
     * alive for the rest of the process, one more per click on "login".
     */
    private class Flow(val server: HttpServer, val executor: ExecutorService)

    private var flow: Flow? = null

    @Synchronized
    private fun stopFlow(delaySeconds: Long = 0) {
        val current = flow ?: return
        flow = null
        current.server.stop(delaySeconds.toInt())
        current.executor.shutdown()
    }

    @Synchronized
    private fun startFlow(): Int {
        // One flow at a time: a second login attempt replaces the first instead of stacking on it.
        stopFlow()
        val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "auth-callback").apply { isDaemon = true }
        }

        server.createContext("/callback") { exchange ->
            try {
                val query = exchange.requestURI.query
                val success = query != null && handleCallback(query) {
                    try {
                        URLDecoder.decode(it, "UTF-8")
                    } catch (e: UnsupportedEncodingException) {
                        Logger.e(e) { "Error decoding callback query parameter" }
                        null
                    }
                }

                val response = if (success) {
                    "Login successful! You can close this window."
                } else {
                    "Login failed! Token not found."
                }
                val responseCode = if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest

                exchange.sendResponseHeaders(responseCode.value, response.length.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            } catch (e: IOException) {
                Logger.e(e) { "Error handling authentication callback" }
            } finally {
                stopFlow(delaySeconds = ServerStopDelaySeconds)
            }
        }

        server.executor = executor
        server.start()
        flow = Flow(server, executor)

        // Watchdog: the browser may never come back (user closed the tab, cancelled the login).
        watchdog.schedule({ stopFlow() }, LoginTimeoutMinutes, TimeUnit.MINUTES)

        return server.address.port
    }

    override fun login() {
        try {
            val port = startFlow()
            val redirectUri = "http://localhost:$port/callback"
            val authUrl = getAuthUrl(redirectUri)

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            } else {
                Logger.e { "No desktop browser available to open the authentication URL" }
                stopFlow()
            }
        } catch (e: IOException) {
            Logger.e(e) { "Error starting authentication server" }
            stopFlow()
        } catch (e: URISyntaxException) {
            Logger.e(e) { "Invalid authentication URL" }
            stopFlow()
        }
    }

    private companion object {
        /** Daemon so a pending timeout never keeps the JVM alive. */
        val watchdog = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "auth-timeout").apply { isDaemon = true }
        }
    }
}

@Composable
actual fun rememberAuthenticationHandler(): AuthenticationHandler {
    val authProvider = koinInject<AuthTokenProvider>()
    return remember(authProvider) { JvmAuthenticationHandler(authProvider) }
}
