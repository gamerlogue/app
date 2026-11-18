package it.maicol07.gamerlogue.services

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import co.touchlab.kermit.Logger
import it.maicol07.gamerlogue.ui.components.SyncPhase
import it.maicol07.gamerlogue.ui.components.WebSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * A [WebSession] on a WebView attached to no window, for syncs that run while the app is in the background.
 *
 * Nobody can sign in here, so [awaitLogin] only checks that the cookies still hold a session. The WebView is
 * created on the calling thread and must only be used from it, the main thread; [destroy] it when done.
 * Unlike the compose-webview session the bridge is a plain `addJavascriptInterface`, with a shim giving the
 * scripts the `GlBridge.call(method, data)` object they expect.
 */
@SuppressLint("SetJavaScriptEnabled")
class HeadlessWebSession(context: Context) : WebSession {
    private var loading = false

    @Volatile private var pending: CompletableDeferred<String?>? = null

    @Volatile private var stepId = 0

    @Volatile private var lastActivity = TimeSource.Monotonic.markNow()

    override val skips = mutableListOf<SkippedWrite>()

    private val webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // Nobody sees the page: skipping images saves data and CPU.
        settings.blockNetworkImage = true
        configureServiceWebView(this)
        addJavascriptInterface(Bridge(), SyncScripts.BRIDGE_NATIVE)
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                loading = true
            }

            override fun onPageFinished(view: WebView, url: String) {
                Logger.d(tag = TAG) { "loaded $url" }
                loading = false
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Logger.i(tag = TAG) { "JS: ${message.message()}" }
                return true
            }
        }
    }

    override val currentUrl: String? get() = webView.url

    /**
     * Like the interactive login wait, minus the user: the login page of a stored session moves on by itself (a
     * server redirect, or the [ServiceConnector.loginTriggerScript] of single-page sites), so give it
     * [LOGIN_CHECK_TIMEOUT] to do so.
     */
    override suspend fun awaitLogin(connector: ServiceConnector) {
        load(connector.loginUrl)
        connector.loginTriggerScript?.let { webView.evaluateJavascript(it, null) }
        val reached = withTimeoutOrNull(LOGIN_CHECK_TIMEOUT.milliseconds) {
            while (currentUrl?.let { connector.isLoggedIn(it) } != true) delay(POLL_INTERVAL.milliseconds)
        }
        Logger.i(tag = TAG) { "awaitLogin ${connector.service} reached=${reached != null} url=$currentUrl" }
        if (reached == null) throw LoginRequiredException(connector.service)
    }

    override fun reportProgress(done: Int, total: Int) {
        lastActivity = TimeSource.Monotonic.markNow()
    }

    override fun reportSkip(skip: SkippedWrite) {
        Logger.i(tag = TAG) { "skip ${skip.reason}: ${skip.name}" }
        skips += skip
    }

    override suspend fun run(step: WebStep): List<ExternalGameRef> = parseWriteResult(runStep(step))

    // ponytail: mirrors ServiceWebViewSession.read; share it once the background sync keeps this session.
    override suspend fun <T> read(source: DataSource<T>, phase: SyncPhase): T = when (source) {
        is DataSource.Web -> source.parse(runStep(source.step))
        is DataSource.Api -> {
            val credential = parseCredentialJson(runStep(source.credentialStep))
            if (credential.isBlank()) {
                source.default
            } else {
                runCatching { source.fetch(credential) }
                    .onFailure { Logger.w(tag = TAG, throwable = it) { "API read failed for $currentUrl" } }
                    .getOrDefault(source.default)
            }
        }
    }

    /** Nobody is there to pick: a background push sends nothing that needs confirming. */
    override suspend fun confirmPush(games: List<LibrarySync.OutgoingGame>): List<LibrarySync.OutgoingGame> =
        emptyList()

    fun destroy() = webView.destroy()

    private suspend fun load(url: String) {
        loading = true
        webView.loadUrl(url)
        withTimeoutOrNull(LOAD_TIMEOUT.milliseconds) {
            while (loading) delay(POLL_INTERVAL.milliseconds)
        }
        delay(SETTLE_DELAY.milliseconds)
    }

    /** Navigate + inject [step], then await its raw bridge JSON (null after [SCRIPT_TIMEOUT] of silence). */
    private suspend fun runStep(step: WebStep): String? {
        if (step.reload || currentUrl != step.url) load(step.url)
        val deferred = CompletableDeferred<String?>()
        stepId++
        pending = deferred
        lastActivity = TimeSource.Monotonic.markNow()
        val script = "$BRIDGE_SHIM window.__glCancel = false; window.__glToken = $stepId; ${step.script}"
        webView.evaluateJavascript(script, null)
        try {
            while (!deferred.isCompleted && lastActivity.elapsedNow() <= SCRIPT_TIMEOUT.milliseconds) {
                delay(POLL_INTERVAL.milliseconds)
            }
        } finally {
            pending = null
        }
        val json = if (deferred.isCompleted) deferred.await() else null
        if (json == null) webView.evaluateJavascript("window.__glCancel = true;", null)
        Logger.i(tag = TAG) { "step ${step.url} result=${json != null}" }
        return json
    }

    /** Called by the page on the WebView's JavaBridge thread, hence the volatile fields it touches. */
    private inner class Bridge {
        @JavascriptInterface
        fun call(method: String, data: String) {
            when (method) {
                SyncScripts.RESULT_METHOD -> unwrapBridgeReply(data, stepId)?.let { pending?.complete(it) }
                SyncScripts.PROGRESS_METHOD -> {
                    val count = Json.parseToJsonElement(data).jsonObject
                    reportProgress(
                        count["done"]?.jsonPrimitive?.intOrNull ?: 0,
                        count["total"]?.jsonPrimitive?.intOrNull ?: 0,
                    )
                }
                SyncScripts.SKIP_METHOD -> {
                    val skip = Json.parseToJsonElement(data).jsonObject
                    val reason = skip["reason"]?.jsonPrimitive?.content
                    reportSkip(
                        SkippedWrite(
                            skip["name"]?.jsonPrimitive?.content.orEmpty(),
                            SkipReason.entries.firstOrNull { it.name == reason } ?: SkipReason.REJECTED,
                        ),
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "HeadlessWebSession"
        const val POLL_INTERVAL = 300L
        const val SETTLE_DELAY = 600L
        const val LOAD_TIMEOUT = 30_000L
        const val SCRIPT_TIMEOUT = 25_000L
        const val LOGIN_CHECK_TIMEOUT = 20_000L

        /** The `GlBridge` object the scripts call, forwarding to the native interface as a JSON string. */
        const val BRIDGE_SHIM = "window.${SyncScripts.BRIDGE_OBJECT} = window.${SyncScripts.BRIDGE_OBJECT} || " +
            "{ call: function(m, d) { window.${SyncScripts.BRIDGE_NATIVE}.call(m, JSON.stringify(d === undefined ? null : d)); } };"
    }
}
