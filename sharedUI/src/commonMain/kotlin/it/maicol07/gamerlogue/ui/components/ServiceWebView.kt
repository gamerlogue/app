package it.maicol07.gamerlogue.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.parkwoocheol.composewebview.ComposeWebView
import com.parkwoocheol.composewebview.WebViewController
import com.parkwoocheol.composewebview.WebViewState
import com.parkwoocheol.composewebview.client.onConsoleMessage
import com.parkwoocheol.composewebview.client.onProgressChanged
import com.parkwoocheol.composewebview.client.rememberWebChromeClient
import com.parkwoocheol.composewebview.rememberWebViewController
import com.parkwoocheol.composewebview.rememberWebViewJsBridge
import com.parkwoocheol.composewebview.rememberWebViewState
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__service_phase_logged_in
import gamerlogue.sharedui.generated.resources.settings__service_phase_reading
import it.maicol07.gamerlogue.services.DataSource
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.configureServiceWebView
import it.maicol07.gamerlogue.services.parseRefsJson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

/**
 * Drives one store automation flow inside a WebView.
 *
 * The WebView is a single instance kept alive across the whole flow so its session/cookies and the
 * injected scripts survive login → work. It's created and hosted by [rememberServiceWebViewHost];
 * the caller (ServiceSyncScreen) reads the session's observable flags to decide when the WebView
 * needs to be interactive (login) versus visible-but-passive (working), and renders the WebView slot
 * wherever it wants (e.g. inside a bottom sheet). When the [flow] returns the host calls `onClose`.
 */
interface WebSession {
    val currentUrl: String?
    suspend fun awaitLogin(connector: ServiceConnector)

    /** Run a write [step] in the WebView; the returned refs are the script's confirmation, if any. */
    suspend fun run(step: WebStep): List<ExternalGameRef>

    /**
     * Resolve a read [source]: a [DataSource.Web] runs its script same-origin and parses the bridge
     * result; a [DataSource.Api] grabs a credential in the WebView then fetches from Kotlin, falling
     * back to the source's default on a blank credential or a failed fetch.
     */
    suspend fun <T> read(source: DataSource<T>): T

    /**
     * Navigate to [url] and reveal the WebView with a "Continue" button so the user can interactively
     * sign into a second (store) origin whose first-party session the DOM ops need; suspends until they
     * tap Continue. Used when [ServiceConnector.storeLoginUrl] is set (e.g. Xbox: the Microsoft Store
     * wishlist origin, which the login.live.com token session doesn't cover).
     */
    suspend fun awaitStoreLogin(url: String)

    /**
     * Show a checklist of [games] about to be pushed to the store wishlist and suspend until the user
     * picks which to send (empty if they skip). No-op (returns empty) when [games] is empty. Used to
     * preview the outgoing direction before writing to the store.
     */
    suspend fun confirmPush(games: List<LibrarySync.OutgoingGame>): List<LibrarySync.OutgoingGame>
}

/** Coarse phases surfaced to the on-screen progress log. */
enum class SyncPhase { LOGGED_IN, READING }

@Composable
fun SyncPhase.label() = stringResource(
    when (this) {
        SyncPhase.LOGGED_IN -> Res.string.settings__service_phase_logged_in
        SyncPhase.READING -> Res.string.settings__service_phase_reading
    }
)

/** The single WebView instance plus the composable slot that renders it. */
class ServiceWebViewHost internal constructor(
    val session: ServiceWebViewSession,
    val webView: @Composable (Modifier) -> Unit,
)

/**
 * Sets up the WebView (controller, state, JS bridge) once, wires the result bridge and runs [flow],
 * calling [onClose] when it returns. Returns a [ServiceWebViewHost] whose [ServiceWebViewHost.session]
 * exposes the flow's observable state and whose [ServiceWebViewHost.WebView] renders the live WebView
 * into a caller-provided slot.
 */
@Composable
fun rememberServiceWebViewHost(
    initialUrl: String,
    onClose: () -> Unit,
    flow: suspend (WebSession) -> Unit,
): ServiceWebViewHost {
    val controller = rememberWebViewController()
    val state = rememberWebViewState(initialUrl)
    val bridge = rememberWebViewJsBridge(
        jsObjectName = SyncScripts.BRIDGE_OBJECT,
        nativeInterfaceName = SyncScripts.BRIDGE_NATIVE,
    )
    val session = remember(controller, state) { ServiceWebViewSession(controller, state) }
    var progress by remember { mutableStateOf(0) }
    val chromeClient = rememberWebChromeClient {
        onConsoleMessage { _, message ->
            Logger.i(tag = "ServiceWebView") { "JS: ${message.message}" }
            false
        }
        onProgressChanged { _, p -> progress = p }
    }

    LaunchedEffect(bridge, session) {
        // Accept a JsonElement (never a typed String): the connector scripts deliver an object, array or
        // scalar, and decoding to a String would throw on the object/array shapes. Re-stringify for parsing.
        bridge.register<JsonElement, Boolean>(SyncScripts.RESULT_METHOD) { json ->
            session.deliver(json.toString())
            true
        }
    }

    LaunchedEffect(session) {
        try {
            flow(session)
        } finally {
            onClose()
        }
    }

    val webView: @Composable (Modifier) -> Unit = { modifier ->
        Box(modifier) {
            ComposeWebView(
                state = state,
                controller = controller,
                jsBridge = bridge,
                chromeClient = chromeClient,
                onCreated = ::configureServiceWebView,
                modifier = Modifier.fillMaxSize(),
            )
            // The WebView surface paints black until the page's first frame; cover it with a progress
            // indicator while it loads so the user sees progress instead of a black screen.
            if (progress < 100) {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { progress / 100f })
                    }
                }
            }
        }
    }

    return ServiceWebViewHost(session, webView)
}

/**
 * Backs [rememberServiceWebViewHost]: the [WebSession] implementation whose observable flags
 * ([loginRequired], [pendingConfirm], [awaitingManualLogin], [log]) drive the hosting screen.
 */
class ServiceWebViewSession internal constructor(
    private val controller: WebViewController,
    private val state: WebViewState,
) : WebSession {
    private var pending: CompletableDeferred<String?>? = null
    private var confirm: CompletableDeferred<List<LibrarySync.OutgoingGame>>? = null
    private var manualLogin: CompletableDeferred<Unit>? = null

    var loginDone by mutableStateOf(false)
        private set
    var pendingConfirm by mutableStateOf<List<LibrarySync.OutgoingGame>?>(null)
        private set
    var awaitingManualLogin by mutableStateOf(false)
        private set
    val log = mutableStateListOf<SyncPhase>()

    override val currentUrl: String? get() = state.lastLoadedUrl

    /**
     * The WebView must be interactive: either the first login (the login/sign-in page is showing) or a
     * second store-origin login. Otherwise it's working and should be shown passively (peek) or hidden.
     */
    val loginRequired: Boolean
        get() = awaitingManualLogin ||
            (!loginDone && currentUrl.let { it == null || it.contains("login") || it.contains("signin") })

    internal fun deliver(json: String?) {
        pending?.complete(json)
    }

    override suspend fun confirmPush(games: List<LibrarySync.OutgoingGame>): List<LibrarySync.OutgoingGame> {
        if (games.isEmpty()) return emptyList()
        val deferred = CompletableDeferred<List<LibrarySync.OutgoingGame>>()
        confirm = deferred
        pendingConfirm = games
        val selected = deferred.await()
        pendingConfirm = null
        confirm = null
        return selected
    }

    /** Called by the checklist body with the user's selection. */
    fun resolveConfirm(selected: List<LibrarySync.OutgoingGame>) {
        confirm?.complete(selected)
    }

    override suspend fun awaitStoreLogin(url: String) {
        Logger.i(tag = TAG) { "awaitStoreLogin: loadUrl $url" }
        controller.loadUrl(url)
        awaitLoaded()
        val deferred = CompletableDeferred<Unit>()
        manualLogin = deferred
        awaitingManualLogin = true
        deferred.await()
        awaitingManualLogin = false
        manualLogin = null
        Logger.i(tag = TAG) { "awaitStoreLogin: continued at ${state.lastLoadedUrl}" }
    }

    /** Called by the Continue button once the user has signed into the store. */
    fun resolveManualLogin() {
        manualLogin?.complete(Unit)
    }

    override suspend fun awaitLogin(connector: ServiceConnector) {
        Logger.i(tag = TAG) { "awaitLogin(${connector.service}) — current=${state.lastLoadedUrl}" }
        val trigger = connector.loginTriggerScript
        val reached = withTimeoutOrNull(LOGIN_TIMEOUT.milliseconds) {
            var triggered = false
            while (true) {
                val url = state.lastLoadedUrl
                if (url != null && connector.isLoggedIn(url)) return@withTimeoutOrNull true
                // Once the landing page has loaded, kick off the store's sign-in flow (e.g. PSN: click
                // the header sign-in button). Fire-and-forget and idempotent, so injecting once is enough.
                if (!triggered && trigger != null && url != null && !state.isLoading) {
                    controller.evaluateJavascript(trigger) {}
                    triggered = true
                }
                delay(POLL_INTERVAL)
            }
            @Suppress("UNREACHABLE_CODE") false
        }
        Logger.i(tag = TAG) { "awaitLogin done reached=$reached url=${state.lastLoadedUrl}" }
        if (reached == true) {
            loginDone = true
            log.add(SyncPhase.LOGGED_IN)
        }
    }

    override suspend fun run(step: WebStep): List<ExternalGameRef> = parseRefsJson(runStep(step))

    override suspend fun <T> read(source: DataSource<T>): T = when (source) {
        is DataSource.Web -> source.parse(runStep(source.step))
        is DataSource.Api -> {
            // The credential step delivers the credential as its single ref's uid (e.g. PSN npsso).
            val credential = parseRefsJson(runStep(source.credentialStep)).firstOrNull()?.uid.orEmpty()
            if (credential.isBlank()) {
                source.default
            } else {
                runCatching { source.fetch(credential) }
                    .onFailure { Logger.w(throwable = it) { "API read failed for ${state.lastLoadedUrl}" } }
                    .getOrDefault(source.default)
            }
        }
    }

    /** Navigate + inject [step], then await its raw bridge JSON (null on timeout). */
    private suspend fun runStep(step: WebStep): String? {
        log.add(SyncPhase.READING)
        val deferred = CompletableDeferred<String?>()
        pending = deferred
        Logger.i(tag = TAG) { "run: loadUrl ${step.url}" }
        controller.loadUrl(step.url)
        awaitLoaded()
        controller.evaluateJavascript(step.script) {} // fire-and-forget; result via the bridge
        Logger.i(tag = TAG) { "run: injected script, awaiting bridge result…" }
        val json = withTimeoutOrNull(SCRIPT_TIMEOUT) { deferred.await() }
        pending = null
        Logger.i(tag = TAG) { "run: got bridge result=${json != null}" }
        return json
    }

    private suspend fun awaitLoaded() {
        delay(POLL_INTERVAL)
        withTimeoutOrNull(LOAD_TIMEOUT) {
            while (state.isLoading) delay(POLL_INTERVAL)
        }
        delay(SETTLE_DELAY)
    }

    private companion object {
        const val TAG = "ServiceWebView"
        const val POLL_INTERVAL = 300L
        const val SETTLE_DELAY = 600L
        const val LOAD_TIMEOUT = 30_000L
        const val SCRIPT_TIMEOUT = 25_000L
        const val LOGIN_TIMEOUT = 300_000L
    }
}
