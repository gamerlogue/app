package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.parseUrl
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.core.DEFAULT_SERVER_URL
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.core.igdbApiUrlFor
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.KoinViewModel
import kotlin.coroutines.cancellation.CancellationException

/** Outcome of checking one URL; the probe is advisory and never blocks saving a well-formed URL. */
enum class UrlCheck { NOT_CHECKED, CHECKING, INVALID, REACHABLE, UNEXPECTED_RESPONSE, UNREACHABLE }

/**
 * Edits the Gamerlogue instance and the IGDB endpoint the app talks to.
 *
 * The fields are a draft until [save]: a half-typed URL must not start routing requests. Switching
 * instance signs the user out first, on the old instance, since its tokens mean nothing to the new one.
 */
@KoinViewModel
class ServerSettingsViewModel(
    private val preferences: AppPreferences,
    private val authProvider: AuthTokenProvider,
    private val authHandler: AuthenticationHandler,
) : StateViewModel<ServerSettingsViewModel.UiState>(
    UiState(
        serverUrl = preferences.serverUrl.value,
        igdbApiUrlOverride = preferences.igdbApiUrlOverride.value,
        savedServerUrl = preferences.serverUrl.value,
        savedIgdbApiUrlOverride = preferences.igdbApiUrlOverride.value,
    )
) {
    data class UiState(
        val serverUrl: String,
        /** Null while IGDB follows the server. */
        val igdbApiUrlOverride: String?,
        val savedServerUrl: String,
        val savedIgdbApiUrlOverride: String?,
        val serverCheck: UrlCheck = UrlCheck.NOT_CHECKED,
        val igdbCheck: UrlCheck = UrlCheck.NOT_CHECKED,
    ) {
        val igdbApiUrl get() = igdbApiUrlOverride ?: igdbApiUrlFor(normalized(serverUrl))
        val isDirty get() = normalized(serverUrl) != savedServerUrl ||
            igdbApiUrlOverride?.let(::normalized) != savedIgdbApiUrlOverride
        val isDefault get() = savedServerUrl == DEFAULT_SERVER_URL && savedIgdbApiUrlOverride == null
    }

    // Deliberately not the JSON:API client: its response validator drops the session on a 401, which a
    // probe of some unrelated host must never trigger. No retries either, a check should answer fast.
    private val probeClient = HttpClient {
        install(HttpTimeout) { requestTimeoutMillis = PROBE_TIMEOUT_MILLIS }
    }

    init {
        check()
    }

    fun setServerUrl(url: String) = update { copy(serverUrl = url, serverCheck = UrlCheck.NOT_CHECKED) }

    fun setIgdbFollowsServer(follows: Boolean) = update {
        copy(igdbApiUrlOverride = if (follows) null else igdbApiUrl, igdbCheck = UrlCheck.NOT_CHECKED)
    }

    fun setIgdbApiUrl(url: String) = update { copy(igdbApiUrlOverride = url, igdbCheck = UrlCheck.NOT_CHECKED) }

    /** Whether [save] would switch instance while signed in, which the screen confirms first. */
    fun saveSignsOut() = normalized(state.serverUrl) != state.savedServerUrl && authProvider.session.value.isAuthenticated

    /** Whether [restoreDefaults] would switch instance while signed in. */
    fun restoreSignsOut() = state.savedServerUrl != DEFAULT_SERVER_URL && authProvider.session.value.isAuthenticated

    fun check() {
        val server = normalized(state.serverUrl)
        val igdb = normalized(state.igdbApiUrl)
        update {
            copy(
                serverCheck = if (isValidUrl(server)) UrlCheck.CHECKING else UrlCheck.INVALID,
                igdbCheck = if (isValidUrl(igdb)) UrlCheck.CHECKING else UrlCheck.INVALID,
            )
        }
        if (isValidUrl(server)) {
            viewModelScope.launch {
                val result = probe { probeClient.isGamerlogue(server) }
                update { copy(serverCheck = result) }
            }
        }
        if (isValidUrl(igdb)) {
            viewModelScope.launch {
                val result = probe { probeClient.isIgdb(igdb) }
                update { copy(igdbCheck = result) }
            }
        }
    }

    fun save() {
        val server = normalized(state.serverUrl)
        val igdbOverride = state.igdbApiUrlOverride?.let(::normalized)
        if (!isValidUrl(server) || (igdbOverride != null && !isValidUrl(igdbOverride))) {
            check()
            return
        }
        persist(server, igdbOverride)
    }

    fun restoreDefaults() {
        update { copy(serverUrl = DEFAULT_SERVER_URL, igdbApiUrlOverride = null) }
        persist(DEFAULT_SERVER_URL, null)
    }

    private fun persist(server: String, igdbOverride: String?) = viewModelScope.launch {
        if (server != state.savedServerUrl && authProvider.session.value.isAuthenticated) {
            // A failed sign-out is reported by safeRequest; the instance stays as it was, so tokens of
            // the old instance are never sent to the new one.
            if (safeRequest { authHandler.logout() }.isErr) return@launch
        }
        // The default is stored as an absence, so a restored installation keeps following future defaults.
        preferences.setServerUrl(server.takeUnless { it == DEFAULT_SERVER_URL })
        preferences.setIgdbApiUrlOverride(igdbOverride)
        update {
            copy(
                serverUrl = server,
                igdbApiUrlOverride = igdbOverride,
                savedServerUrl = server,
                savedIgdbApiUrlOverride = igdbOverride,
            )
        }
        check()
    }

    override fun onCleared() = probeClient.close()
}

/** Gamerlogue publishes its OpenAPI document, whose title tells it apart from any other web server. */
private suspend fun HttpClient.isGamerlogue(server: String): Boolean {
    val response = get("$server/api/docs.jsonopenapi")
    return response.isOk() &&
        probeJson.parseToJsonElement(response.bodyAsText()).jsonObject["info"]?.jsonObject?.get("title")
            ?.jsonPrimitive?.content == GAMERLOGUE_API_TITLE
}

/** The smallest real IGDB query: any working endpoint answers it with a JSON array. */
private suspend fun HttpClient.isIgdb(igdb: String): Boolean {
    val response = post("$igdb/games") { setBody("fields id; limit 1;") }
    return response.isOk() && probeJson.parseToJsonElement(response.bodyAsText()) is JsonArray
}

@Suppress("TooGenericExceptionCaught") // Classifying any failure as "unreachable" is the point of a probe.
private suspend fun probe(request: suspend () -> Boolean): UrlCheck = try {
    if (request()) UrlCheck.REACHABLE else UrlCheck.UNEXPECTED_RESPONSE
} catch (e: CancellationException) {
    throw e
} catch (e: IllegalArgumentException) {
    // Malformed JSON (SerializationException) or a non-object where one was expected.
    UrlCheck.UNEXPECTED_RESPONSE.also { Logger.d(e) { "Unexpected probe response" } }
} catch (e: Exception) {
    UrlCheck.UNREACHABLE.also { Logger.d(e) { "Probe failed" } }
}

private fun HttpResponse.isOk() = status == HttpStatusCode.OK

private const val PROBE_TIMEOUT_MILLIS = 10_000L
private const val GAMERLOGUE_API_TITLE = "Gamerlogue API"
private val probeJson = Json { ignoreUnknownKeys = true }

private fun normalized(url: String) = url.trim().trimEnd('/')

private fun isValidUrl(url: String) =
    (url.startsWith("http://") || url.startsWith("https://")) && !parseUrl(url)?.host.isNullOrBlank()
