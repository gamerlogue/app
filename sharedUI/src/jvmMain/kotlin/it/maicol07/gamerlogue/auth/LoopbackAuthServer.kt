package it.maicol07.gamerlogue.auth

import co.touchlab.kermit.Logger
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Owns the temporary localhost server used by the desktop PKCE callback. */
internal class LoopbackAuthServer(
    private val exchangeCallback: suspend (String, PendingLogin) -> Boolean,
) {
    private var server: HttpServer? = null

    @Throws(IOException::class)
    @Synchronized
    fun start(proof: PkceLoginAttempt): Int {
        stop()
        val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        server.createContext("/callback") { handleCallback(it, proof) }

        // A null executor makes HttpServer dispatch on its own thread; the handler blocks on the token
        // exchange either way, so a dedicated single-thread pool bought nothing.
        server.start()
        this.server = server
        watchdog.schedule({ stop() }, LOGIN_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        return server.address.port
    }

    @Synchronized
    fun stop(delaySeconds: Long = 0) {
        val current = server ?: return
        server = null
        current.stop(delaySeconds.toInt())
    }

    private fun handleCallback(exchange: HttpExchange, proof: PkceLoginAttempt) {
        val callbackQuery = exchange.requestURI.query
            ?.takeIf { callbackMatchesState(it, proof.pending.state) }
        val success = callbackQuery?.let { exchangeToken(it, proof.pending) } ?: false
        writeResponse(exchange, success)
        if (callbackQuery != null) stop(delaySeconds = SERVER_STOP_DELAY_SECONDS)
    }

    private fun exchangeToken(query: String, pending: PendingLogin): Boolean = try {
        runBlocking { exchangeCallback(query, pending) }
    } catch (e: ResponseException) {
        Logger.e(e) { "Token exchange failed with HTTP ${e.response.status.value}" }
        false
    } catch (e: SerializationException) {
        Logger.e(e) { "Token exchange returned an invalid response" }
        false
    } catch (e: IllegalArgumentException) {
        Logger.e(e) { "Token exchange returned invalid session data" }
        false
    } catch (e: IOException) {
        Logger.e(e) { "Token exchange could not reach the server" }
        false
    }

    private fun writeResponse(exchange: HttpExchange, success: Boolean) {
        val response = if (success) {
            "Login successful. You can close this window."
        } else {
            "Login failed. The callback is invalid or expired."
        }.encodeToByteArray()
        val status = if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest

        try {
            exchange.sendResponseHeaders(status.value, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        } catch (e: IOException) {
            Logger.e(e) { "Error writing the authentication callback response" }
        }
    }

    private companion object {
        const val LOGIN_TIMEOUT_MINUTES = 5L
        const val SERVER_STOP_DELAY_SECONDS = 1L

        val watchdog: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "auth-timeout").apply { isDaemon = true }
        }
    }
}
