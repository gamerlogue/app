package it.maicol07.gamerlogue.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import co.touchlab.kermit.Logger
import com.parkwoocheol.composewebview.ComposeWebView
import com.parkwoocheol.composewebview.WebViewController
import com.parkwoocheol.composewebview.WebViewState
import com.parkwoocheol.composewebview.client.onConsoleMessage
import com.parkwoocheol.composewebview.client.rememberWebChromeClient
import com.parkwoocheol.composewebview.rememberWebViewController
import com.parkwoocheol.composewebview.rememberWebViewJsBridge
import com.parkwoocheol.composewebview.rememberWebViewState
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__import_no_match
import gamerlogue.sharedui.generated.resources.settings__open_store
import gamerlogue.sharedui.generated.resources.settings__service_login_required
import gamerlogue.sharedui.generated.resources.settings__service_phase_logged_in
import gamerlogue.sharedui.generated.resources.settings__service_phase_reading
import gamerlogue.sharedui.generated.resources.settings__service_working
import gamerlogue.sharedui.generated.resources.settings__wishlist_already_present
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_confirm
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_off_platform
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_skip
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import it.maicol07.gamerlogue.extensions.openURL
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
import org.jetbrains.compose.resources.stringResource

/**
 * Drives one store automation flow inside a WebView.
 *
 * The WebView is shown only while the user needs to sign in; once logged in it's covered by a
 * progress/log overlay (it stays alive underneath to run the injected scripts). Results come back
 * through the JS bridge, so reads work even where `evaluateJavascript` can't return a value (CEF).
 * When the [flow] returns the WebView closes.
 */
interface WebSession {
    val currentUrl: String?
    suspend fun awaitLogin(connector: ServiceConnector)
    suspend fun run(step: WebStep): List<ExternalGameRef>

    /**
     * Navigate to [url] and reveal the WebView with a "Continue" button so the user can interactively
     * sign into a second (store) origin whose first-party session the DOM ops need; suspends until they
     * tap Continue. Used when [ServiceConnector.storeLoginUrl] is set (e.g. Xbox: the Microsoft Store
     * wishlist origin, which the login.live.com token session doesn't cover).
     */
    suspend fun awaitStoreLogin(url: String)

    /**
     * Show an in-WebView checklist of [games] about to be pushed to the store wishlist and suspend
     * until the user picks which to send (empty if they skip). No-op (returns empty) when [games] is
     * empty. Used to preview the outgoing direction before writing to the store.
     */
    suspend fun confirmPush(games: List<LibrarySync.OutgoingGame>): List<LibrarySync.OutgoingGame>
}

/** Coarse phases surfaced to the on-screen progress log. */
enum class SyncPhase { LOGGED_IN, READING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceWebView(
    initialUrl: String,
    onClose: () -> Unit,
    flow: suspend (WebSession) -> Unit,
) {
    val controller = rememberWebViewController()
    val state = rememberWebViewState(initialUrl)
    val bridge = rememberWebViewJsBridge(
        jsObjectName = SyncScripts.BRIDGE_OBJECT,
        nativeInterfaceName = SyncScripts.BRIDGE_NATIVE,
    )
    val session = remember(controller, state) { WebSessionImpl(controller, state) }
    val chromeClient = rememberWebChromeClient {
        onConsoleMessage { _, message ->
            Logger.i(tag = "ServiceWebView") { "JS: ${message.message}" }
            false
        }
    }

    LaunchedEffect(bridge, session) {
        bridge.register<String, Boolean>(SyncScripts.RESULT_METHOD) { json ->
            session.deliver(parseRefsJson(json))
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

    val loginDone = session.loginDone
    val url = session.currentUrl
    val confirming = session.pendingConfirm
    val manualLogin = session.awaitingManualLogin
    // Reveal the WebView for the first login (login pages only) OR whenever we're waiting on the user to
    // sign into a second store origin (any URL — the store page degrades to logged-out, never a /login).
    val showWebView = confirming == null &&
        (manualLogin || (!loginDone && (url == null || url.contains("login"))))

    Box(Modifier.fillMaxSize()) {
        // The progress panel is the base layer; the WebView sits on top only when login is needed.
        // While working, the WebView is shrunk to 1dp (kept alive to run the injected scripts) rather
        // than overlaid — on desktop the CEF browser is a native component that draws above Compose,
        // so an overlay can't hide it.
        if (confirming != null) {
            PushChecklist(
                games = confirming,
                onConfirm = session::resolveConfirm,
                onSkip = { session.resolveConfirm(emptyList()) },
            )
        } else if (!showWebView) {
            Surface(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(Res.string.settings__service_working), style = MaterialTheme.typography.titleMedium)
                    session.log.forEach { phase ->
                        Text(
                            phase.label(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        ComposeWebView(
            state = state,
            controller = controller,
            jsBridge = bridge,
            chromeClient = chromeClient,
            onCreated = ::configureServiceWebView,
            modifier = if (showWebView) Modifier.fillMaxSize() else Modifier.size(1.dp),
        )

        if (showWebView) {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            ) {
                Text(
                    stringResource(Res.string.settings__service_login_required),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center,
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.CloseW500Rounded, contentDescription = "Close")
            }
            // Second-origin store login: the user signs in on the page, then taps Continue to proceed.
            if (manualLogin) {
                Button(
                    onClick = session::resolveManualLogin,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                ) {
                    Text(stringResource(Res.string.settings__wishlist_push_confirm))
                }
            }
        }
    }
}

/** Outgoing-direction preview: pick which Gamerlogue backlog games to add to the store wishlist. */
@Composable
private fun PushChecklist(
    games: List<LibrarySync.OutgoingGame>,
    onConfirm: (List<LibrarySync.OutgoingGame>) -> Unit,
    onSkip: () -> Unit,
) {
    // Pushable rows need both a store page and to release on this platform; the rest are shown disabled.
    val selected = remember(games) {
        mutableStateMapOf<String, Boolean>().apply {
            games.forEach { put(it.uid, it.storeUrl != null && it.onPlatform && !it.alreadyOnWishlist) }
        }
    }
    val onPlatform = games.filter { it.onPlatform }
    val offPlatform = games.filter { !it.onPlatform }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text(
                stringResource(Res.string.settings__wishlist_push_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(onPlatform) { game -> PushRow(game, selected[game.uid] == true) { selected[game.uid] = it } }
                if (offPlatform.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.settings__wishlist_push_off_platform),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(offPlatform) { game -> PushRow(game, selected[game.uid] == true) { selected[game.uid] = it } }
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.settings__wishlist_push_skip))
                }
                Button(
                    onClick = { onConfirm(games.filter { selected[it.uid] == true }) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.settings__wishlist_push_confirm))
                }
            }
        }
    }
}

/** One outgoing-preview row. Selectable only when matched (has a store page) and on-platform; an
 *  unmatched on-platform game shows "no match", an off-platform game has no store link. */
@Composable
private fun PushRow(game: LibrarySync.OutgoingGame, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val pushable = game.storeUrl != null && game.onPlatform && !game.alreadyOnWishlist
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = pushable)
        val cover = game.coverImageId
        if (cover != null) {
            RemoteImage(
                url = igdbImageUrl(cover, IgdbImageSize.COVER_SMALL),
                contentDescription = game.name,
                modifier = Modifier.padding(horizontal = 8.dp)
                    .size(width = 32.dp, height = 43.dp).clip(RoundedCornerShape(4.dp)),
            )
        }
        val color = if (pushable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(game.name, color = color)
            val subtitle = when {
                game.alreadyOnWishlist -> Res.string.settings__wishlist_already_present
                game.onPlatform && game.storeUrl == null -> Res.string.settings__import_no_match
                else -> null
            }
            if (subtitle != null) {
                Text(
                    stringResource(subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (game.storeUrl != null) {
            IconButton(onClick = { uriHandler.openURL(game.storeUrl) }) {
                Icon(Icons.OpenInNewW500Rounded, contentDescription = stringResource(Res.string.settings__open_store))
            }
        }
    }
}

@Composable
private fun SyncPhase.label() = stringResource(
    when (this) {
        SyncPhase.LOGGED_IN -> Res.string.settings__service_phase_logged_in
        SyncPhase.READING -> Res.string.settings__service_phase_reading
    }
)

private class WebSessionImpl(
    private val controller: WebViewController,
    private val state: WebViewState,
) : WebSession {
    private var pending: CompletableDeferred<List<ExternalGameRef>>? = null
    private var confirm: CompletableDeferred<List<LibrarySync.OutgoingGame>>? = null
    private var manualLogin: CompletableDeferred<Unit>? = null

    // Observed by the composable to drive the WebView/overlay and the progress log.
    var loginDone by mutableStateOf(false)
        private set
    var pendingConfirm by mutableStateOf<List<LibrarySync.OutgoingGame>?>(null)
        private set
    var awaitingManualLogin by mutableStateOf(false)
        private set
    val log = mutableStateListOf<SyncPhase>()

    override val currentUrl: String? get() = state.lastLoadedUrl

    fun deliver(refs: List<ExternalGameRef>) {
        pending?.complete(refs)
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

    /** Called by the checklist overlay with the user's selection. */
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
        val reached = withTimeoutOrNull(LOGIN_TIMEOUT) {
            while (true) {
                val url = state.lastLoadedUrl
                if (url != null && connector.isLoggedIn(url)) return@withTimeoutOrNull true
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

    override suspend fun run(step: WebStep): List<ExternalGameRef> {
        log.add(SyncPhase.READING)
        val deferred = CompletableDeferred<List<ExternalGameRef>>()
        pending = deferred
        Logger.i(tag = TAG) { "run: loadUrl ${step.url}" }
        controller.loadUrl(step.url)
        awaitLoaded()
        controller.evaluateJavascript(step.script) {} // fire-and-forget; result via the bridge
        Logger.i(tag = TAG) { "run: injected script, awaiting bridge result…" }
        val refs = withTimeoutOrNull(SCRIPT_TIMEOUT) { deferred.await() } ?: emptyList()
        pending = null
        Logger.i(tag = TAG) { "run: got ${refs.size} refs" }
        return refs
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
