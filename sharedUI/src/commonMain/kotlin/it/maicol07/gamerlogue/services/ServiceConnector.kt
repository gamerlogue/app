package it.maicol07.gamerlogue.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A game as seen on an external store: its store-specific [uid] (Steam appid, GOG/Epic product id,
 * PSN concept id, Xbox title id) and a human [name] used for the IGDB name-fallback match.
 */
@Serializable
data class ExternalGameRef(val uid: String, val name: String = "")

/**
 * One automation step: navigate the WebView to [url], then run [script].
 *
 * Scripts push their result to Kotlin through the WebView JS bridge (so it works even where
 * `evaluateJavascript` can't return a value, e.g. desktop/CEF). A [SyncScripts.wrap]ped body just
 * assigns the `out` array of `{uid,name}`; the wrapper handles the bridge call and bridge readiness.
 */
data class WebStep(val url: String, val script: String)

/**
 * The single per-service object: identity ([service]), IGDB mapping metadata ([externalGameSource],
 * [storeUrl], [uidFromUrl]) and the WebView automation recipe (login + read/write scripts).
 *
 * We call no official API: the user logs into the store in a WebView and we drive that authenticated,
 * same-origin session with injected JavaScript. Subclasses override only what differs from the
 * sensible defaults below (most just need URLs + scripts).
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
    open fun loginUrl(): String = "https://$host/"

    /** True once [currentUrl] is a logged-in page of this store. */
    open fun isLoggedIn(currentUrl: String): Boolean =
        currentUrl.contains(host) && LOGIN_MARKERS.none { currentUrl.contains(it) }

    /** The IGDB-indexed store URL for [uid], or null when this service has no URL mapping. */
    fun storeUrl(uid: String): String? = storeUrlTemplate?.replace("{id}", uid)

    /** Extract the store uid back out of an IGDB `url` (inverse of [storeUrl]). */
    open fun uidFromUrl(url: String): String? = null

    // --- JS path (default): the WebView runs a script that returns the data same-origin. ---

    open fun readOwned(): WebStep = empty()
    open fun readWishlist(): WebStep = empty()

    /** Batch wishlist write (one request for all [refs]); for stores with a write endpoint (e.g. Steam). */
    open fun addToWishlist(refs: List<ExternalGameRef>): WebStep = empty()

    /** True if the wishlist write is done one game at a time via [wishlistPushStep] (e.g. PSN: open the
     *  store page and click its add-to-wishlist button) instead of the batch [addToWishlist]. */
    open fun pushesPerGame(): Boolean = false

    /** Per-game wishlist write step (navigate to the store page + click), given the IGDB store URL. */
    open fun wishlistPushStep(storeUrl: String): WebStep? = null

    /** Adjust an IGDB store URL before navigating to it for a push (e.g. strip the locale segment). */
    open fun normalizePushUrl(url: String): String = url

    private fun empty() = WebStep(loginUrl(), SyncScripts.wrap("out = [];"))

    // --- API path (e.g. PSN): the WebView only grabs a credential, then data is fetched off-WebView
    // from Kotlin (no CORS). A connector uses EITHER the JS path above OR this one. ---

    /** If non-null, the WebView runs this step to obtain a credential (its first ref's `uid`) used by
     *  whichever operations opt into the API path via [ownedViaApi]/[wishlistViaApi]. */
    open fun credentialStep(): WebStep? = null

    /** Per-operation: read owned games / the wishlist from Kotlin (API) instead of a WebView script. */
    open fun ownedViaApi(): Boolean = false
    open fun wishlistViaApi(): Boolean = false

    open suspend fun apiOwned(credential: String): List<ExternalGameRef> = emptyList()
    open suspend fun apiWishlist(credential: String): List<ExternalGameRef> = emptyList()
    open suspend fun apiAddToWishlist(credential: String, refs: List<ExternalGameRef>) {}

    private companion object {
        val LOGIN_MARKERS = listOf("/login", "signin")
    }
}

/** JSON array literal of the refs' uids, e.g. `["12","34"]`, for embedding in an injected script. */
internal fun List<ExternalGameRef>.uidJsonArray(): String =
    joinToString(prefix = "[", postfix = "]") { "\"${it.uid.replace("\"", "")}\"" }

private val resultJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** Unwrap one optional layer of JSON-string quoting some transports add around the bridge result. */
internal fun cleanJsResult(raw: String?): String? {
    val s = raw?.trim() ?: return null
    if (s.isEmpty() || s == "null") return null
    return runCatching { resultJson.decodeFromString<String>(s) }.getOrDefault(s)
}

/** Parse the `[{uid,name}, …]` JSON a connector script delivers through the WebView JS bridge. */
internal fun parseRefsJson(raw: String?): List<ExternalGameRef> {
    val s = cleanJsResult(raw) ?: return emptyList()
    return runCatching { resultJson.decodeFromString<List<ExternalGameRef>>(s) }.getOrDefault(emptyList())
}

/** Shared JS plumbing for connectors: bridge delivery + desktop/CEF native polyfill. */
object SyncScripts {
    /** Injected JS object name (see [WebViewJsBridge][com.parkwoocheol.composewebview.WebViewJsBridge]). */
    const val BRIDGE_OBJECT = "GlBridge"

    /** Native-interface object name the bridge proxies to (must match the bridge's nativeInterfaceName). */
    const val BRIDGE_NATIVE = "GlBridgeNative"

    /** Bridge method the wrapped scripts call to deliver their `[{uid,name}, …]` result. */
    const val RESULT_METHOD = "glResult"

    /**
     * Wrap a body that assigns the `out` array into the bridge-delivery protocol: wait for the bridge,
     * run the (possibly async) body, then push `out` to Kotlin. Always sends something (empty on error)
     * so the Kotlin side never waits forever.
     */
    fun wrap(body: String): String = """
        (function() {
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
                    window.$BRIDGE_OBJECT.call('$RESULT_METHOD', JSON.stringify(out || []));
                } catch (e) { console.log('[GL] send error ' + e); }
            }
            function __glRun() {
                (async function() {
                    let out = [];
                    try { $body } catch (e) { console.log('[GL] body error ' + e); out = []; }
                    __glSend(out);
                })();
            }
            if (window.$BRIDGE_OBJECT && window.$BRIDGE_OBJECT.call) {
                console.log('[GL] bridge ready'); __glRun();
            } else {
                console.log('[GL] waiting bridge');
                window.addEventListener('${BRIDGE_OBJECT}Ready', function() {
                    console.log('[GL] bridge event'); __glRun();
                }, { once: true });
            }
        })();
    """.trimIndent()
}
