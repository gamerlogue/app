package it.maicol07.gamerlogue.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import java.awt.Desktop
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val LoginTimeoutMinutes = 5L
private const val ServerStopDelaySeconds = 1L

class JvmAuthenticationHandler(
    authProvider: AuthTokenProvider,
    authClient: HttpClient,
) : AuthenticationHandler(authProvider, authClient) {
    private class Flow(
        val server: HttpServer,
        val executor: ExecutorService,
    )

    private var flow: Flow? = null

    @Synchronized
    private fun stopFlow(delaySeconds: Long = 0) {
        val current = flow ?: return
        flow = null
        current.server.stop(delaySeconds.toInt())
        current.executor.shutdown()
    }

    @Synchronized
    private fun startFlow(proof: LoginProof): Int {
        stopFlow()
        val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "auth-callback").apply { isDaemon = true }
        }

        server.createContext("/callback") { exchange ->
            val query = exchange.requestURI.query
            val belongsToFlow = query != null && callbackMatchesState(query, proof.state)
            val success = if (belongsToFlow) {
                try {
                    runBlocking { exchangeCallback(query, proof) }
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
            } else {
                false
            }

            val response = if (success) {
                "Login successful. You can close this window."
            } else {
                "Login failed. The callback is invalid or expired."
            }
            val responseCode = if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest

            try {
                exchange.sendResponseHeaders(responseCode.value, response.encodeToByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.encodeToByteArray()) }
            } catch (e: IOException) {
                Logger.e(e) { "Error writing the authentication callback response" }
            } finally {
                if (belongsToFlow) stopFlow(delaySeconds = ServerStopDelaySeconds)
            }
        }

        server.executor = executor
        server.start()
        flow = Flow(server, executor)
        watchdog.schedule({ stopFlow() }, LoginTimeoutMinutes, TimeUnit.MINUTES)
        return server.address.port
    }

    override fun login() {
        try {
            val proof = generateLoginProof()
            val port = startFlow(proof)
            val authUrl = getAuthUrl("http://localhost:$port/callback", proof)

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

    override suspend fun handleCallback(query: String): Boolean = false

    private companion object {
        val watchdog = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "auth-timeout").apply { isDaemon = true }
        }
    }
}

private fun generateLoginProof(): LoginProof {
    val random = SecureRandom()
    return createLoginProof(
        verifierEntropy = ByteArray(32).also(random::nextBytes),
        stateEntropy = ByteArray(32).also(random::nextBytes),
        sha256 = { MessageDigest.getInstance("SHA-256").digest(it) },
    )
}

@Composable
actual fun rememberAuthenticationHandler(): AuthenticationHandler {
    val authProvider = koinInject<AuthTokenProvider>()
    val authClient = koinInject<HttpClient>(qualifier = named("AuthHttpClient"))
    return remember(authProvider, authClient) { JvmAuthenticationHandler(authProvider, authClient) }
}
