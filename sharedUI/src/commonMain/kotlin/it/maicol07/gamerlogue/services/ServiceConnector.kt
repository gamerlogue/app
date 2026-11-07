package it.maicol07.gamerlogue.services

import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * A game as seen on an external store: its store-specific [uid] (Steam appid, GOG/Epic product id,
 * PSN concept id, Xbox title id) and a human [name] used for the IGDB name-fallback match. Trackers
 * also deliver the game's play [status] (scripts emit the [GameLibraryStatus] entry name). A
 * [gameId] is an explicit IGDB id, read from tracker metadata or echoed by a [ServiceConnector.statusWrite] result.
 */
@Serializable
data class ExternalGameRef(
    val uid: String,
    val name: String = "",
    val status: GameLibraryStatus? = null,
    val gameId: Int? = null,
    /** The game's IGDB slug, when the service links to IGDB: matched exactly instead of by name. */
    val igdbSlug: String? = null,
    /** The game's Steam app id, when the service knows it: matched exactly via IGDB's Steam mapping. */
    val steamAppId: String? = null,
)

/**
 * One status to write onto a tracker: the IGDB [gameId] and [name] of a Gamerlogue game, its tracker
 * [uid] when it is already there (null = add it, found by [name] or id), the tracker's current status
 * [from] (null when absent; trackers that toggle shelves need it to clear the old one) and the target [to].
 * [platformIds] are the IGDB platforms to add a missing game on, for trackers that need one (empty = unknown).
 */
@Serializable
data class StatusChange(
    val gameId: Int,
    val name: String,
    val uid: String?,
    val from: GameLibraryStatus?,
    val to: GameLibraryStatus,
    val platformIds: List<Int> = emptyList(),
    /** IGDB platform names and abbreviations for matching a tracker's own platform catalog. */
    val platformNames: Map<Int, List<String>> = emptyMap(),
    /** Ownership for a new library entry; updates keep the remote entry's ownership. */
    val owned: Boolean = false,
)

/**
 * The logged-in user's identity on a store: their [username], optional [avatarUrl] and [profileUrl]
 * (a public page for the account). Shown per connected service in the linked-services list.
 */
@Serializable
data class ServiceProfile(val username: String, val avatarUrl: String? = null, val profileUrl: String? = null)

/**
 * One automation step: navigate the WebView to [url], then run [script].
 *
 * Scripts push their result to Kotlin through the WebView JS bridge (so it works even where
 * `evaluateJavascript` can't return a value, e.g. desktop/CEF). A [SyncScripts.wrap]ped body just
 * assigns the `out` array of `{uid,name}`; the wrapper handles the bridge call and bridge readiness.
 * With [reload] false, an already matching page is reused, retaining its per-flow JavaScript state.
 */
data class WebStep(val url: String, val script: String, val reload: Boolean = true)

/**
 * How one store read resolves. Every read is EITHER same-origin in the WebView OR off-WebView from
 * Kotlin — a connector never needs both for one operation, so this is a two-case choice, not two
 * parallel methods + a flag.
 *
 * - [Web]: run [Web.step] in the WebView and [Web.parse] its bridge result.
 * - [Api]: grab a credential in the WebView (via [Api.credentialStep]), then [Api.fetch] from Kotlin
 *   (no browser CORS); falls back to [Api.default] on a blank credential or a failed fetch.
 */
sealed interface DataSource<T> {
    data class Web<T>(val step: WebStep, val parse: (String?) -> T) : DataSource<T>
    data class Api<T>(
        val credentialStep: WebStep,
        val default: T,
        val fetch: suspend (credential: String) -> T,
    ) : DataSource<T>
}

/**
 * How wishlist writes are performed. [Batch] sends all refs in a single step (stores with a write
 * endpoint, e.g. Steam/GOG); [PerGame] opens each game's store page and acts on it (PSN/Xbox/Epic);
 * [PerGameResolved] first loads an intermediate page ([PerGameResolved.resolve]) whose result's first uid is the real
 * product URL, then acts on that (Nintendo: the IGDB URL is a www.nintendo.com page whose eShop link
 * carries the numeric title id needed to reach the ec.nintendo.com product page with the add button);
 * [SearchByName] searches the store by title and acts on the matching result (Ubisoft: games are
 * name-matched with no IGDB store URL, so there's no `storeUrl` to open a product page with);
 * [ById] writes by IGDB id directly.
 */
sealed interface WishlistWrite {
    data class Batch(val step: (refs: List<ExternalGameRef>) -> WebStep) : WishlistWrite
    data class PerGame(val step: (storeUrl: String) -> WebStep?) : WishlistWrite
    data class PerGameResolved(
        val resolve: (storeUrl: String) -> WebStep,
        val step: (resolvedUrl: String) -> WebStep?,
    ) : WishlistWrite

    data class SearchByName(val step: (name: String) -> WebStep) : WishlistWrite

    /** Sends all games in a single step keyed by their IGDB id, for trackers whose game ids are IGDB ids
     *  ([IgdbKey.GAME_ID], e.g. Backloggd) — no store page or search needed. */
    data class ById(val step: (gameIds: List<Int>) -> WebStep) : WishlistWrite

    /** A free-text tracker needs the game's name and platforms rather than a store URL. */
    data class ByGame(val step: (games: List<LibrarySync.OutgoingGame>) -> WebStep) : WishlistWrite

    /** Whether writes reach a game through its store page, so pushing needs each game's store URL. */
    val needsStorePage: Boolean get() = this is Batch || this is PerGame || this is PerGameResolved
}

/** Web read delivering a `[{uid,name}, …]` list (owned games / wishlist). */
internal fun webRefs(step: WebStep): DataSource<List<ExternalGameRef>> = DataSource.Web(step, ::parseRefsJson)

/** Web read delivering a `{username, avatarUrl, profileUrl}` profile. */
internal fun webProfile(step: WebStep): DataSource<ServiceProfile?> = DataSource.Web(step, ::parseProfileJson)

/** API read of the user's games; empty on a blank credential or a failed fetch. */
internal fun apiRefs(
    credentialStep: WebStep,
    fetch: suspend (credential: String) -> List<ExternalGameRef>,
): DataSource<List<ExternalGameRef>> = DataSource.Api(credentialStep, emptyList(), fetch)

/** API read of the user's profile; null on a blank credential or a failed fetch. */
internal fun apiProfile(
    credentialStep: WebStep,
    fetch: suspend (credential: String) -> ServiceProfile?,
): DataSource<ServiceProfile?> = DataSource.Api(credentialStep, null, fetch)

/**
 * The single per-service object: identity ([service]), IGDB mapping metadata ([externalGameSource],
 * [storeUrl], [uidFromUrl]) and the WebView automation recipe (login + read/write [DataSource]s).
 *
 * We call no official API from the browser: the user logs into the store in a WebView and we drive
 * that authenticated, same-origin session with injected JavaScript (or, for [DataSource.Api] reads,
 * grab a credential there and fetch from Kotlin). Subclasses override only what differs from the
 * sensible defaults below (most just need URLs + the read/write sources).
 */
abstract class ServiceConnector(
    val service: ExternalService,
    /** Host that identifies a logged-in page of this store. */
    private val host: String,
    /** IGDB `external_games.external_game_source` id for this store. */
    val externalGameSource: Int,
    /** Template (with `{id}`) for the IGDB-indexed store URL, or null if this store has no mapping. */
    private val storeUrlTemplate: String? = null,
) {
    /** IGDB `platform_family` id this store belongs to (PlayStation=1, Xbox=2, …), or null for PC
     *  stores (Steam/GOG/Epic) where games have no single family — used to flag wishlist pushes for
     *  games that don't release on this platform. */
    open val platformFamily: Int? = null

    /** Page where the user signs in (defaults to the store root). */
    open val loginUrl: String get() = "https://$host/"

    /** Origins whose WebView cookies hold this store's authenticated session; cleared on disconnect so
     *  the next connect starts logged out. Defaults to the login origin; override when the session
     *  spans more hosts (e.g. Steam's community domain, PSN's Sony account domains). */
    open val sessionUrls: List<String> get() = listOf(loginUrl)

    /** True once the store session is established. Defaults to a URL check (on a store host, off any
     *  login page); override for stores where the URL alone is ambiguous and the session must be read
     *  from cookies instead (e.g. PSN, whose post-login URL is just the store host). */
    open suspend fun isLoggedIn(currentUrl: String): Boolean =
        currentUrl.contains(host) && LOGIN_MARKERS.none { currentUrl.contains(it) }

    /** JS injected once on the login landing page to kick off the store's sign-in flow (e.g. click the
     *  header sign-in button). Null (default) = the sign-in page is reached directly, no trigger needed.
     *  Fire-and-forget (no bridge result); must be a no-op when its target element isn't present. */
    @Suppress("SameReturnValue")
    open val loginTriggerScript: String? get() = null

    /** Match IGDB `external_games` on the numeric `uid` field instead of `url` — for stores whose
     *  IGDB url is slug-based and can't be rebuilt from the store uid (e.g. GOG). */
    open val idMatchesUid: Boolean = false

    /** Which IGDB value the service's game uid is, when it is one (Backloggd: the game id; Stash: the
     *  cover image id), so refs match exactly with no store-id lookup table. Null = a store-specific id. */
    open val igdbKey: IgdbKey? = null

    /** The service has no sign-in: the user gives their public username instead (Stash), stored under
     *  [ExternalService.settingsKey] [HANDLE_KEY] before the connect flow opens their profile page. */
    open val needsHandle: Boolean = false

    /** The IGDB-indexed store URL for [uid], or null when this service has no URL mapping. */
    fun storeUrl(uid: String): String? = storeUrlTemplate?.replace("{id}", uid)

    /** Extract the store uid back out of an IGDB `url` (inverse of [storeUrl]). */
    open fun uidFromUrl(url: String): String? = null

    /** For [WishlistWrite.SearchByName] connectors with no store URL to confirm a game belongs on this
     *  store: whether [game] plausibly does, so an unrelated backlog game isn't searched for and pushed
     *  onto a store it was never published on (e.g. Ubisoft: check IGDB's involved companies). Ignored
     *  by connectors that always have a store URL to confirm the match instead. */
    open fun matchesPublisher(game: Game): Boolean = true

    // --- Reads + wishlist write. One member per concern; each connector picks Web or Api per read. ---

    /** The user's owned/played games (every connector reads these). */
    abstract val ownedGames: DataSource<List<ExternalGameRef>>

    /** The user's store wishlist, or null if this store exposes none. */
    open val wishlist: DataSource<List<ExternalGameRef>>?
        @Suppress("SameReturnValue")
        get() = null

    /** The signed-in user's profile, or null if this store exposes none. */
    open val profile: DataSource<ServiceProfile?>?
        @Suppress("SameReturnValue")
        get() = null

    /** How to push backlog games onto the store wishlist, or null if writes aren't supported. */
    open val wishlistWrite: WishlistWrite?
        @Suppress("SameReturnValue")
        get() = null

    /** Statuses this tracker can store faithfully — a game whose Gamerlogue status is outside it isn't
     *  synced either way, or the two sides would keep "disagreeing". Empty for import-only services. */
    open val syncStatuses: Set<GameLibraryStatus> = emptySet()

    /** Whether [statusWrite] can add a game the tracker doesn't have yet; when not, the sync only updates the
     *  games already there (HowLongToBeat: no reliable way to find a game by IGDB identity). */
    open val canAddGames: Boolean = true

    /** Writes [StatusChange]s onto the tracker in one step, delivering a ref (tracker uid + [ExternalGameRef.gameId])
     *  per game written; null when the service can only be imported from. */
    open val statusWrite: ((List<StatusChange>) -> WebStep)?
        @Suppress("SameReturnValue")
        get() = null

    /** Adjust an IGDB store URL before navigating to it for a push (e.g. strip the locale segment). */
    open fun normalizePushUrl(url: String): String = url

    companion object {
        const val HANDLE_KEY = "handle"
        private val LOGIN_MARKERS = listOf("/login", "signin")
    }
}

/** An IGDB value a service's game uid can be ([ServiceConnector.igdbKey]). */
enum class IgdbKey { GAME_ID, COVER_IMAGE_ID }

// IGDB platform ids for PC stores (Steam/GOG/Epic/Ubisoft): platform_family is null for these
// AND for several unrelated platforms (Android, iOS, Web, DOS, Arcade, …), so an explicit allowlist
// is used instead of "family == null" to avoid tagging a cross-platform title with a mobile/web id.
private val PcPlatformIds = setOf(6, 3, 14) // PC (Windows), Linux, Mac

/**
 * IGDB platform ids [game] releases on, restricted to [ServiceConnector.platformFamily] (or, for PC
 * stores with a null family, to [PcPlatformIds]) — used to autofill `LibraryEntry.platformsIds` on
 * import, since a store [ExternalGameRef] carries no platform info of its own.
 */
fun ServiceConnector.platformIdsFor(game: Game): List<Int> {
    // A tracker spans every platform, so it says nothing about which one the user plays on.
    if (service.category == ServiceCategory.TRACKER) return emptyList()
    val family = platformFamily
    return game.platforms
        .filter { platform ->
            if (family == null) {
                platform.id.toInt() in PcPlatformIds
            } else {
                platform.platform_family?.id?.toInt() == family
            }
        }
        .map { it.id.toInt() }
}

/** JSON array literal of the refs' uids, e.g. `["12","34"]`, for embedding in an injected script. */
internal fun List<ExternalGameRef>.uidJsonArray(): String =
    joinToString(prefix = "[", postfix = "]") { "\"${it.uid.replace("\"", "")}\"" }

private val resultJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    // An unknown status from a tracker script falls back to null instead of failing the whole list.
    coerceInputValues = true
}

/** Unwrap one optional layer of JSON-string quoting some transports add around the bridge result. */
internal fun cleanJsResult(raw: String?): String? {
    val s = raw?.trim()?.takeUnless { it.isEmpty() || it == "null" } ?: return null
    return runCatching { resultJson.decodeFromString<String>(s) }.getOrDefault(s)
}

/**
 * Unwrap a `{token, out}` bridge reply: the `out` JSON of the reply for step [token], or null for a reply
 * from another (earlier, timed-out) step or a malformed one.
 */
internal fun unwrapBridgeReply(json: String?, token: Int): String? {
    val reply = json?.let { runCatching { resultJson.parseToJsonElement(it) as? JsonObject }.getOrNull() } ?: return null
    return reply["out"]?.toString().takeIf { (reply["token"] as? JsonPrimitive)?.content == token.toString() }
}

/** A store read that failed (timeout, script error, unreadable payload) rather than found nothing. */
class ServiceReadException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

private fun decodeRefs(element: JsonElement): List<ExternalGameRef> = resultJson.decodeFromJsonElement(element)

/**
 * Parse the `[{uid,name}, …]` JSON a connector script delivers through the WebView JS bridge. A failure (no
 * result, an `{error}` payload, invalid JSON or a malformed ref) throws [ServiceReadException]: it must never
 * look like an empty library, which sync would treat as authoritative. Only a successful `[]` is empty.
 */
@Suppress("ThrowsCount")
internal fun parseRefsJson(raw: String?): List<ExternalGameRef> {
    val s = cleanJsResult(raw) ?: throw ServiceReadException("Service read timed out")
    try {
        val element = resultJson.parseToJsonElement(s)
        if (element is JsonObject) {
            throw ServiceReadException(
                (element["error"] as? JsonPrimitive)?.content ?: "Unexpected service read result",
            )
        }
        return decodeRefs(element)
    } catch (e: SerializationException) {
        throw ServiceReadException("Unreadable service read result: ${e.message}", e)
    }
}

internal class ScriptWriteException(
    message: String,
    val written: List<ExternalGameRef>,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

/** A failed write retains the confirmed prefix so it can be checkpointed before surfacing the error. */
@Suppress("ThrowsCount")
internal fun parseWriteResult(raw: String?): List<ExternalGameRef> {
    val value = cleanJsResult(raw) ?: throw ScriptWriteException("Tracker write timed out", emptyList())
    try {
        val result = resultJson.parseToJsonElement(value)
        if (result is JsonObject) {
            val written = try {
                result["written"]?.let(::decodeRefs).orEmpty()
            } catch (ignored: SerializationException) {
                emptyList()
            }
            throw ScriptWriteException(
                (result["error"] as? JsonPrimitive)?.content ?: "Unexpected tracker write result",
                written,
            )
        }
        return decodeRefs(result)
    } catch (e: SerializationException) {
        throw ScriptWriteException("Unreadable tracker write result: ${e.message}", emptyList(), e)
    }
}

/** Parse the `{username, avatarUrl, profileUrl}` JSON a [ServiceConnector.profile] script delivers
 *  through the bridge. Null on error or an empty username. */
internal fun parseProfileJson(raw: String?): ServiceProfile? {
    val s = cleanJsResult(raw) ?: return null
    return runCatching { resultJson.decodeFromString<ServiceProfile>(s) }
        .getOrNull()?.takeIf { it.username.isNotBlank() }
        // Scripts send "" for a missing avatar/page; that is "none", not an image URL to load.
        ?.let { it.copy(avatarUrl = it.avatarUrl?.ifBlank { null }, profileUrl = it.profileUrl?.ifBlank { null }) }
}

/**
 * Parse the credential a [DataSource.Api] credential script delivers through the bridge: a plain
 * string (`out = token`). Blank when the script found none — an empty or missing value makes the
 * wrapper fall back to its `[]` sentinel, as a thrown error does.
 */
internal fun parseCredentialJson(raw: String?): String {
    val s = cleanJsResult(raw) ?: return ""
    return if (s.startsWith("[")) "" else s
}

/** Shared JS plumbing for connectors: bridge delivery, DOM waiting helpers, desktop/CEF native polyfill. */
object SyncScripts {
    /** Injected JS object name (see [WebViewJsBridge][com.parkwoocheol.composewebview.WebViewJsBridge]). */
    const val BRIDGE_OBJECT = "GlBridge"

    /** Native-interface object name the bridge proxies to (must match the bridge's nativeInterfaceName). */
    const val BRIDGE_NATIVE = "GlBridgeNative"

    /** Bridge method the wrapped scripts call to deliver their `[{uid,name}, …]` result. */
    const val RESULT_METHOD = "glResult"

    /** Bridge method scripts call (via [HELPERS]' `__glProgress`) to report `{done, total}` games so far. */
    const val PROGRESS_METHOD = "glProgress"

    /** Default polling budget of the wait helpers: tries × interval ≈ 15s, under the Kotlin script timeout. */
    private const val DEFAULT_TRIES = 30
    private const val POLL_MS = 500

    /** Default click-then-verify budget of [HELPERS]' `__glClickUntil`. */
    private const val CLICK_ATTEMPTS = 5
    private const val CLICK_SETTLE_MS = 1200

    /**
     * DOM helpers every injected script gets, so no connector hand-rolls a poll loop.
     *
     * - `__glSleep(ms)`
     * - `__glWaitFor(find, tries?, interval?)` — polls `find()` until it returns a truthy value (a
     *   collection must be non-empty) and resolves it; on timeout resolves whatever the last call gave.
     * - `__glWaitForEl(selector, …)` / `__glWaitForAll(selector, …)` — selector shorthands.
     * - `__glVisible(selector)` — first match that is actually rendered, skipping hidden duplicates.
     * - `__glClickUntil(find, confirmed, attempts?, settle?)` — click then verify, with retry: the
     *   React/SSR stores render the button before binding its handler, so a single early click hits a
     *   dead handler and silently does nothing. Resolves `confirmed()`.
     * - `__glFetchDoc(path)` — same-origin GET parsed as an HTML document; throws an Error carrying `status` on a
     *   non-OK response. `__glDoc(path)` is the soft variant: null on a non-OK response.
     * - `__glPool(items, size, task)` — runs `task(item)` over `items` with `size` parallel workers.
     * - `__glCancelled()` — true once Kotlin timed the step out; write loops stop starting new writes.
     * - `__glProgress(done, total)` — report how many games were read/written so far for the progress
     *   bar; `total` 0 when not known yet (e.g. paginated reads). Best-effort: never throws.
     */
    val HELPERS = """
        function __glSleep(ms) { return new Promise(function(r) { setTimeout(r, ms); }); }
        async function __glFetchDoc(path) {
            let r = await fetch(path, { credentials: 'include' });
            if (!r.ok) { let e = new Error('HTTP ' + r.status + ' ' + path); e.status = r.status; throw e; }
            return new DOMParser().parseFromString(await r.text(), 'text/html');
        }
        async function __glDoc(path) {
            try { return await __glFetchDoc(path); } catch (e) { if (e.status) return null; throw e; }
        }
        function __glCancelled() { return window.__glCancel === true; }
        async function __glPool(items, size, task) {
            let queue = items.slice();
            await Promise.all(Array.from({ length: size }, async function() {
                while (queue.length && !__glCancelled()) await task(queue.shift());
            }));
        }
        function __glProgress(done, total) {
            try {
                __glEnsureNative();
                window.$BRIDGE_OBJECT.call('$PROGRESS_METHOD', { done: done, total: total || 0 });
            } catch (e) {}
        }
        async function __glWaitFor(find, tries, interval) {
            var max = tries || $DEFAULT_TRIES, every = interval || $POLL_MS;
            for (var i = 0; i < max; i++) {
                var found = find();
                // A NodeList/array counts as found only once non-empty; a single element just has to exist.
                if (found && (found.length === undefined || found.length > 0)) return found;
                await __glSleep(every);
            }
            return find();
        }
        function __glWaitForEl(selector, tries, interval) {
            return __glWaitFor(function() { return document.querySelector(selector); }, tries, interval);
        }
        function __glWaitForAll(selector, tries, interval) {
            return __glWaitFor(function() { return document.querySelectorAll(selector); }, tries, interval);
        }
        function __glVisible(selector) {
            return Array.prototype.find.call(
                document.querySelectorAll(selector),
                function(e) { return e.offsetParent !== null; }
            );
        }
        async function __glClickUntil(find, confirmed, attempts, settle) {
            var max = attempts || $CLICK_ATTEMPTS, pause = settle || $CLICK_SETTLE_MS;
            var tried = 0;
            for (var i = 0; i < max && !confirmed(); i++) {
                var el = find();
                if (!el) { await __glSleep($POLL_MS); continue; } // still rendering
                tried++;
                el.click();
                await __glSleep(pause); // let the framework handle the click and its request fire
            }
            console.log('[GL] clickUntil attempts=' + tried + ' ok=' + confirmed());
            return confirmed();
        }
    """.trimIndent()

    /**
     * Fire-and-forget script that clicks the element [finder] returns as soon as it appears, for
     * [ServiceConnector.loginTriggerScript]. That one is injected raw (it delivers no bridge result and
     * so never goes through [wrap]), hence it carries its own copy of [HELPERS]. A no-op when the
     * element never shows up, so it is safe to inject on any load during the login wait.
     */
    fun clickWhenPresent(label: String, finder: String, tries: Int = LOGIN_TRIES): String = """
        (function() {
            $HELPERS
            __glWaitFor($finder, $tries).then(function(el) {
                if (el) { console.log('[GL] $label click'); el.click(); }
            });
        })();
    """.trimIndent()

    /** Logins can sit on a slow redirect chain, so the trigger waits longer than a DOM read. */
    private const val LOGIN_TRIES = 40

    /** How often [leaveLoginWhenSignedIn] checks the session. */
    private const val SESSION_POLL_MS = 1500

    /**
     * A [ServiceConnector.loginTriggerScript] for single-page sites, whose sign-in ends with a client-side
     * route change: the WebView reports only real page loads, so the login-URL check would never see the
     * user leave the login page. Polls [isSignedIn] (a JS expression resolving to a boolean, typically the
     * site's own "current user" API) and, once true, does a real navigation to [target]. A session that
     * already exists leaves the login page straight away.
     */
    fun leaveLoginWhenSignedIn(isSignedIn: String, target: String): String = """
        (function() {
            var timer = setInterval(check, $SESSION_POLL_MS);
            async function check() {
                try {
                    if (await ($isSignedIn)) { clearInterval(timer); location.href = '$target'; }
                } catch (e) {}
            }
            check();
        })();
    """.trimIndent()

    /**
     * Wrap a body that assigns the `out` array into the bridge-delivery protocol: wait for the bridge,
     * run the (possibly async) body, then push `out` to Kotlin. Always sends something (empty on error)
     * so the Kotlin side never waits forever. [HELPERS] are in scope for the body.
     */
    fun wrap(body: String): String = """
        (function() {
            $HELPERS
            // This run's step token (set by Kotlin just before injection), echoed back so a late reply is dropped.
            var __glTok = window.__glToken;
            // Desktop/CEF re-injects window.$BRIDGE_OBJECT after each navigation but NOT the native
            // polyfill; the CEF message router persists, so re-create the polyfill here. Guarded by
            // window.cefQuery so on Android (real native interface, no cefQuery) we never overwrite it.
            function __glEnsureNative() {
                if (window.$BRIDGE_NATIVE) return;
                if (!window.cefQuery) return;
                window.$BRIDGE_NATIVE = {
                    call: function(method, data, callbackId) {
                        window.cefQuery({
                            request: JSON.stringify({ method: method, data: data, callbackId: callbackId }),
                            onSuccess: function(r) {},
                            onFailure: function(c, m) { console.log('[GL] native fail ' + m); }
                        });
                    }
                };
                console.log('[GL] native polyfill injected');
            }
            function __glSend(out) {
                try {
                    __glEnsureNative();
                    console.log('[GL] send ' + (out ? out.length : 'null'));
                    // Pass the plain value: the bridge JSON-stringifies its `data` argument itself, so
                    // pre-stringifying here would double-encode and break the native-side decode.
                    window.$BRIDGE_OBJECT.call('$RESULT_METHOD', { token: __glTok, out: out || [] });
                } catch (e) { console.log('[GL] send error ' + e); }
            }
            function __glRun() {
                (async function() {
                    let out = [];
                    try { $body } catch (e) {
                        console.log('[GL] body error ' + e);
                        __glSend({ error: String(e), written: Array.isArray(out) ? out : [] });
                        return;
                    }
                    __glSend(out);
                })();
            }
            if (window.$BRIDGE_OBJECT && window.$BRIDGE_OBJECT.call) {
                console.log('[GL] bridge ready'); __glRun();
            } else {
                // Don't wait on the bridge-ready event: the library dispatches a fixed 'AppBridgeReady'
                // (not '${BRIDGE_OBJECT}Ready') and it may have already fired before this listener runs.
                // Poll for the injected object instead — robust on SPA pages that install it late (e.g. GOG).
                console.log('[GL] waiting bridge');
                let __glTries = 0;
                const __glPoll = setInterval(function() {
                    if (window.$BRIDGE_OBJECT && window.$BRIDGE_OBJECT.call) {
                        clearInterval(__glPoll);
                        console.log('[GL] bridge ready (poll)'); __glRun();
                    } else if (++__glTries > 200) { // ~20s, under the Kotlin script timeout
                        clearInterval(__glPoll);
                        console.log('[GL] bridge never appeared'); __glSend([]);
                    }
                }, 100);
            }
        })();
    """.trimIndent()
}
