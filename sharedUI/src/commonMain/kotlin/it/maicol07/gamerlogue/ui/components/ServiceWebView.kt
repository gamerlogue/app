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
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import gamerlogue.sharedui.generated.resources.settings__service_phase_profile
import gamerlogue.sharedui.generated.resources.settings__service_phase_reading
import gamerlogue.sharedui.generated.resources.settings__service_phase_writing
import it.maicol07.gamerlogue.services.DataSource
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.unwrapBridgeReply
import it.maicol07.gamerlogue.services.configureServiceWebView
import it.maicol07.gamerlogue.services.parseCredentialJson
import it.maicol07.gamerlogue.services.parseWriteResult
import it.maicol07.gamerlogue.services.webViewNestedScrollModifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Drives one store automation flow inside a WebView.
 *
 * The WebView is a single instance kept alive across the whole flow so its session/cookies and the
 * injected scripts survive login → work. It's created and hosted by [rememberServiceWebViewHost];
 * the caller (ServiceSyncView) reads the session's observable flags to decide when the WebView
 * needs to be interactive (login) versus visible-but-passive (working), and renders the WebView slot
 * wherever it wants (e.g. inside a bottom sheet). When the flow returns the host calls `onClose`.
 */
interface WebSession {
    val currentUrl: String?
    suspend fun awaitLogin(connector: ServiceConnector)

    /** Report [done] of [total] games handled so far (0 = total not known) for the progress bar, from a
     *  Kotlin-side loop of steps; scripts report through the bridge instead. */
    fun reportProgress(done: Int, total: Int)

    fun beginBatch(index: Int, count: Int, completed: Int, total: Int, size: Int) = reportProgress(completed, total)

    fun finishBatch(done: Int, total: Int) = reportProgress(done, total)

    /** Leave batch mode once a batched write ends, so later progress isn't shown as part of a stale batch. */
    fun endBatch() = Unit

    /** Run a write [step] in the WebView; the returned refs are the script's confirmation, if any. */
    suspend fun run(step: WebStep): List<ExternalGameRef>

    /**
     * Resolve a read [source]: a [DataSource.Web] runs its script same-origin and parses the bridge
     * result; a [DataSource.Api] grabs a credential in the WebView then fetches from Kotlin, falling
     * back to the source's default on a blank credential or a failed fetch. [phase] is what the progress log
     * shows meanwhile (the games or the profile).
     */
    suspend fun <T> read(source: DataSource<T>, phase: SyncPhase): T

    /**
     * Show a checklist of [games] about to be pushed to the store wishlist and suspend until the user
     * picks which to send (empty if they skip). No-op (returns empty) when [games] is empty. Used to
     * preview the outgoing direction before writing to the store.
     */
    suspend fun confirmPush(games: List<LibrarySync.OutgoingGame>): List<LibrarySync.OutgoingGame>
}

/** Track foreground/background so a paused WebView doesn't look like a stuck script (see [ServiceWebViewSession.hostActive]). */
@Composable
private fun TrackHostActive(session: ServiceWebViewSession) = LifecycleResumeEffect(session) {
    session.hostActive = true
    onPauseOrDispose { session.hostActive = false }
}

/** [done] of [total] games handled in the current phase; [total] 0 = not known yet. */
data class StepProgress(val done: Int, val total: Int)

data class BatchProgress(val index: Int, val count: Int, val completed: Int, val total: Int, val step: StepProgress) {
    val overall: StepProgress get() = StepProgress(completed + step.done, total)

    fun withDone(done: Int): BatchProgress = copy(step = step.copy(done = done.coerceIn(0, step.total)))
}

/** What the progress bars show: the games handled in the phase and, while writing in batches, the current batch. */
data class ProgressState(val progress: StepProgress? = null, val batch: BatchProgress? = null) {
    fun begin(index: Int, count: Int, completed: Int, total: Int, size: Int) =
        ProgressState(StepProgress(completed, total), BatchProgress(index, count, completed, total, StepProgress(0, size)))

    fun report(done: Int, total: Int): ProgressState =
        batch?.withDone(done)?.let { ProgressState(it.overall, it) } ?: copy(progress = StepProgress(done, total))

    fun finish(done: Int, total: Int) =
        ProgressState(StepProgress(done, total), batch?.let { it.withDone(done - it.completed) })

    fun end() = copy(batch = null)

    /** A new phase starts counting from scratch; repeated steps of one phase (per-game writes) keep it. */
    fun nextStep(phaseChanged: Boolean, writing: Boolean) =
        if (phaseChanged && (!writing || batch == null)) ProgressState() else this
}

/** The user did not finish signing in to the store within the allowed time. */
class LoginTimeoutException(message: String) : IllegalStateException(message)

/** Coarse phases surfaced to the on-screen progress log. */
enum class SyncPhase { LOGGED_IN, READING, READING_PROFILE, WRITING }

@Composable
fun SyncPhase.label() = stringResource(
    when (this) {
        SyncPhase.LOGGED_IN -> Res.string.settings__service_phase_logged_in
        SyncPhase.READING -> Res.string.settings__service_phase_reading
        SyncPhase.READING_PROFILE -> Res.string.settings__service_phase_profile
        SyncPhase.WRITING -> Res.string.settings__service_phase_writing
    }
)

/** WebView page-load progress value (0..100) meaning fully loaded. */
private const val PROGRESS_COMPLETE = 100

/** The single WebView instance plus the composable slot that renders it. */
class ServiceWebViewHost internal constructor(
    val session: ServiceWebViewSession,
    val webView: @Composable (Modifier) -> Unit,
)

/**
 * Sets up the WebView (controller, state, JS bridge) once, wires the result bridge and runs [flow],
 * calling [onClose] when it returns. Returns a [ServiceWebViewHost] whose [ServiceWebViewHost.session]
 * exposes the flow's observable state and whose [ServiceWebViewHost.webView] renders the live WebView
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
        bridge.register<JsonElement, Boolean>(SyncScripts.PROGRESS_METHOD) { json ->
            val count = json.jsonObject
            session.reportProgress(
                count["done"]?.jsonPrimitive?.intOrNull ?: 0,
                count["total"]?.jsonPrimitive?.intOrNull ?: 0,
            )
            true
        }
    }

    TrackHostActive(session)

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
                // Bridge nested scroll so the page scrolls within the sheet, which stays draggable.
                modifier = Modifier.fillMaxSize().webViewNestedScrollModifier(),
            )
            // The WebView surface paints black until the page's first frame; cover it with a progress
            // indicator while it loads so the user sees progress instead of a black screen.
            if (progress < PROGRESS_COMPLETE) {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { progress / PROGRESS_COMPLETE.toFloat() })
                    }
                }
            }
        }
    }

    return ServiceWebViewHost(session, webView)
}

/**
 * Backs [rememberServiceWebViewHost]: the [WebSession] implementation whose observable flags
 * ([loginRequired], [pendingConfirm], [log]) drive the hosting screen.
 */
class ServiceWebViewSession internal constructor(
    private val controller: WebViewController,
    private val state: WebViewState,
) : WebSession {
    private var pending: CompletableDeferred<String?>? = null
    private var confirm: CompletableDeferred<List<LibrarySync.OutgoingGame>>? = null

    var loginDone by mutableStateOf(false)
        private set
    var pendingConfirm by mutableStateOf<List<LibrarySync.OutgoingGame>?>(null)
        private set
    var awaitingLogin by mutableStateOf(false)
        private set
    val log = mutableStateListOf<SyncPhase>()

    private var progressState by mutableStateOf(ProgressState())

    /** Games handled so far in the current phase, or null before any report. */
    val progress: StepProgress? get() = progressState.progress

    val batchProgress: BatchProgress? get() = progressState.batch

    override fun beginBatch(index: Int, count: Int, completed: Int, total: Int, size: Int) {
        progressState = progressState.begin(index, count, completed, total, size)
    }

    override fun finishBatch(done: Int, total: Int) {
        progressState = progressState.finish(done, total)
    }

    override fun endBatch() {
        progressState = progressState.end()
    }

    /** When the running script last showed signs of life (start or a progress report). */
    private var lastActivity = TimeSource.Monotonic.markNow()

    override fun reportProgress(done: Int, total: Int) {
        progressState = progressState.report(done, total)
        lastActivity = TimeSource.Monotonic.markNow()
    }

    /**
     * Whether the screen hosting the WebView is in the foreground. Paused (device locked, app in the
     * background), the WebView suspends the page's JavaScript, so a waiting script isn't stuck — it just
     * can't run: the idle timeout doesn't count meanwhile and starts afresh on resume.
     */
    var hostActive: Boolean = true
        set(value) {
            field = value
            if (value) lastActivity = TimeSource.Monotonic.markNow()
        }

    override val currentUrl: String? get() = state.lastLoadedUrl

    /**
     * The WebView must be interactive while we're waiting for the user to sign in — the whole [awaitLogin]
     * window (incl. 2FA / passkey "choose another method" pages). It is NOT
     * keyed off the URL: multistep logins (e.g. Nintendo's 2FA) leave the "login"/"signin" path, which
     * used to flip the WebView to passive "working" mid-login and block the user from finishing sign-in.
     */
    val loginRequired: Boolean
        get() = awaitingLogin

    /** Id of the step currently running; replies echoing another id (a timed-out script's) are dropped. */
    private var stepId = 0

    internal fun deliver(json: String?) {
        unwrapBridgeReply(json, stepId)?.let { pending?.complete(it) }
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

    override suspend fun awaitLogin(connector: ServiceConnector) {
        awaitingLogin = true
        try {
            awaitLoginLoop(connector)
        } finally {
            awaitingLogin = false
        }
    }

    private suspend fun awaitLoginLoop(connector: ServiceConnector) {
        Logger.i(tag = TAG) { "awaitLogin(${connector.service}) — current=${state.lastLoadedUrl}" }
        val reached = withTimeoutOrNull(LOGIN_TIMEOUT.milliseconds) {
            // Null once injected (or when the store needs none).
            var trigger = connector.loginTriggerScript
            while (true) {
                val url = state.lastLoadedUrl
                if (url != null && connector.isLoggedIn(url)) return@withTimeoutOrNull true
                // Once the landing page has loaded, kick off the store's sign-in flow (e.g. PSN: click
                // the header sign-in button). Fire-and-forget and idempotent, so injecting once is enough.
                if (trigger != null && url != null && !state.isLoading) {
                    controller.evaluateJavascript(trigger) {}
                    trigger = null
                }
                delay(POLL_INTERVAL.milliseconds)
            }
            @Suppress("UNREACHABLE_CODE")
            false
        }
        Logger.i(tag = TAG) { "awaitLogin done reached=$reached url=${state.lastLoadedUrl}" }
        if (reached != true) throw LoginTimeoutException("Login to ${connector.service} timed out")
        loginDone = true
        log.add(SyncPhase.LOGGED_IN)
    }

    override suspend fun run(step: WebStep): List<ExternalGameRef> = parseWriteResult(runStep(step, SyncPhase.WRITING))

    override suspend fun <T> read(source: DataSource<T>, phase: SyncPhase): T = when (source) {
        is DataSource.Web -> source.parse(runStep(source.step, phase))
        is DataSource.Api -> {
            // The credential step delivers the credential as a plain string (e.g. the PSN npsso cookie).
            val credential = parseCredentialJson(runStep(source.credentialStep, phase))
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
    private suspend fun runStep(step: WebStep, phase: SyncPhase): String? {
        progressState = progressState.nextStep(log.lastOrNull() != phase, phase == SyncPhase.WRITING)
        log.add(phase)
        val deferred = CompletableDeferred<String?>()
        Logger.i(tag = TAG) { "run: loadUrl ${step.url}" }
        if (step.reload || state.lastLoadedUrl != step.url) {
            controller.loadUrl(step.url)
            awaitLoaded()
        }
        // Only replies arriving while this step's script runs complete it: not ones from the page load.
        stepId++
        pending = deferred
        controller.evaluateJavascript("$RESET_CANCEL window.__glToken = $stepId; ${step.script}") {} // fire-and-forget; result via the bridge
        Logger.i(tag = TAG) { "run: injected script, awaiting bridge result…" }
        // An idle timeout, not a total one: a long paginated read that keeps reporting progress isn't cut off.
        lastActivity = TimeSource.Monotonic.markNow()
        try {
            while (!deferred.isCompleted && (!hostActive || lastActivity.elapsedNow() <= SCRIPT_TIMEOUT.milliseconds)) {
                delay(POLL_INTERVAL.milliseconds)
            }
        } finally {
            pending = null
        }
        val json = if (deferred.isCompleted) deferred.await() else null
        // Ask a timed-out script to stop, so it doesn't keep acting on the page in the next step.
        if (json == null) controller.evaluateJavascript(SET_CANCEL) {}
        Logger.i(tag = TAG) { "run: got bridge result=${json != null}" }
        return json
    }

    private suspend fun awaitLoaded() {
        delay(POLL_INTERVAL.milliseconds)
        withTimeoutOrNull(LOAD_TIMEOUT.milliseconds) {
            while (state.isLoading) delay(POLL_INTERVAL.milliseconds)
        }
        delay(SETTLE_DELAY.milliseconds)
    }

    private companion object {
        const val TAG = "ServiceWebView"
        const val POLL_INTERVAL = 300L
        const val SETTLE_DELAY = 600L
        const val LOAD_TIMEOUT = 30_000L
        const val SCRIPT_TIMEOUT = 25_000L
        const val LOGIN_TIMEOUT = 300_000L
        const val RESET_CANCEL = "window.__glCancel = false;"
        const val SET_CANCEL = "window.__glCancel = true;"
    }
}
