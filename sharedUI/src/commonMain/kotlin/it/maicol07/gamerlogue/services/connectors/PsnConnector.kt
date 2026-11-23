package it.maicol07.gamerlogue.services.connectors

import com.parkwoocheol.composewebview.PlatformCookieManager
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.PsnApi
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.services.apiProfile
import it.maicol07.gamerlogue.services.apiRefs
import it.maicol07.gamerlogue.services.uidJsonArray
import it.maicol07.gamerlogue.services.webRefs

/**
 * PlayStation Network — uses the PSN API (psn-api style) via trophy titles.
 *
 * The WebView's only job is to grab the `npsso` cookie from the logged-in session ([credentialStep]
 * reads `ca.account.sony.com/api/v1/ssocookie`); [PsnApi] then exchanges it for a token and lists the
 * user's games from trophy titles (covers PS3/Vita/PS4/PS5 incl. disc games, unlike the purchased
 * GraphQL). All from Kotlin (no browser CORS). Matching falls back to names (`external_game_source=36`;
 * no store-URL template — npCommunicationId isn't an IGDB-indexed URL).
 */
class PsnConnector(private val api: PsnApi) :
    ServiceConnector(ExternalService.PLAYSTATION, host = "playstation.com", externalGameSource = 36) {

    override val platformFamily = 1 // IGDB platform_family: PlayStation

    // Land on the public home and drive its header sign-in button (see [loginTriggerScript]) instead of
    // opening library.playstation.com directly: hitting the library first goes straight to the Sony
    // OAuth page, whose cross-origin token XHR fails in the WebView. Starting from the home first sets
    // playstation.com's own first-party cookies, and the button builds the correct sign-in redirect.
    override val loginUrl = "https://www.playstation.com/"

    // Click the header sign-in button once it renders (React). No-op on the Sony sign-in page (button
    // absent there), so it's safe to inject on any load during the login wait.
    override val loginTriggerScript = SyncScripts.clickWhenPresent(
        label = "psn signin",
        finder = """function() { return document.querySelector('[data-qa="web-toolbar#profile-container#signin-button"]'); }""",
    )

    // The post-login URL is just the store host (no reliable path marker), so detect the session from
    // cookies: the client-readable isSignedIn flag, or the session/userinfo cookies the account sets.
    override suspend fun isLoggedIn(currentUrl: String): Boolean {
        if (!currentUrl.contains("playstation.com")) return false
        val cookies = PlatformCookieManager.getCookies("https://www.playstation.com")
        return cookies.any { c ->
            (c.name == "isSignedIn" && c.value == "true") || c.name == "session" || c.name == "userinfo"
        }
    }

    // Session spans the library/store origins and the Sony account origins that hold the npsso/SSO cookies.
    override val sessionUrls = listOf(
        "https://library.playstation.com/",
        "https://www.playstation.com/",
        "https://ca.account.sony.com/",
        "https://my.account.sony.com/",
    )

    // Strip the `/xx-xx` locale segment so the store redirects to the user's own region/account.
    override fun normalizePushUrl(url: String) =
        url.replace(Regex("(store\\.playstation\\.com)/[a-z]{2}-[a-z]{2}/"), "$1/")

    override fun uidFromUrl(url: String): String? =
        Regex("/(?:concept|product)/([^/?#]+)").find(url)?.groupValues?.get(1)

    // Read the npsso JSON same-origin and deliver the cookie itself; shared by owned + profile.
    private val credentialStep = WebStep(
        "https://ca.account.sony.com/api/v1/ssocookie",
        SyncScripts.wrap(
            """
            try {
                let npsso = (JSON.parse(document.body.innerText || '{}').npsso) || '';
                out = npsso;
            } catch (e) { console.log('[GL] psn npsso err ' + e); out = ''; }
            """.trimIndent(),
        ),
    )

    // Owned games and profile come from the trophy API (npsso credential); the wishlist has no clean API,
    // so it's scraped from the store wishlist page, and writes open each store page and click add (no API).
    override val ownedGames = apiRefs(credentialStep) { npsso -> api.ownedGames(api.accessToken(npsso)) }

    override val profile = apiProfile(credentialStep) { npsso -> api.profile(api.accessToken(npsso)) }

    override val wishlist = webRefs(
        WebStep(
            "https://library.playstation.com/wishlist",
            SyncScripts.wrap(
                """
                console.log('[GL] psn wishlist at ' + location.href);
                // Tiles render late (React); wait for them instead of reading once on an empty DOM
                // (that one-shot read was returning an empty wishlist).
                let tiles = await __glWaitForAll('[data-track-click="web:product-tile-click"]');
                // Each tile carries a clean name + product id in its data-telemetry-meta JSON, and the store
                // URL in its href — no text parsing/cleanup needed.
                let res = {};
                tiles.forEach(tile => {
                    let name = '', id = '';
                    try {
                        let meta = JSON.parse(tile.getAttribute('data-telemetry-meta') || '{}');
                        name = (meta.productName || '').trim();
                        id = String(meta.productId || '');
                    } catch (e) {}
                    let href = tile.getAttribute('href') || (tile.querySelector('a') ? tile.querySelector('a').getAttribute('href') : '') || '';
                    let uid = id || (href.match(/(?:concept|product)\/([^\/?#]+)/) || [])[1] || name;
                    if (name && uid) res[uid] = { uid: uid, name: name };
                });
                out = Object.values(res);
                console.log('[GL] psn wishlist games=' + out.length);
                """.trimIndent(),
            ),
        ),
    )

    // Push backlog games one at a time by opening their PS Store page (from IGDB `external_games.url`,
    // source 36) and clicking the add-to-wishlist button — PSN has no wishlist write API.
    override val wishlistWrite = WishlistWrite.PerGame { storeUrl ->
        WebStep(
            storeUrl,
            SyncScripts.wrap(
                """
                // The page's own game's wishlist toggle (the page also carries other editions' toggles and hidden
                // duplicates), matched on `data-qa` and the product or concept id the page's URL names. Success = it
                // flips to "removeFromWishlist".
                let id = (location.pathname.match(/\/(?:product|concept)\/([^\/?#]+)/) || [])[1] || '';
                let toggle = function() {
                    return Array.prototype.find.call(document.querySelectorAll('button[data-qa="wishlistToggle"]'), function(b) {
                        try {
                            let meta = JSON.parse(b.getAttribute('data-telemetry-meta') || '{}');
                            return !!id && (meta.productId === id || String(meta.conceptId) === id);
                        } catch (e) { return false; }
                    });
                };
                let state = function() { let b = toggle(); return b ? b.getAttribute('data-track-click') || '' : ''; };
                for (let i = 0; i < 20 && !toggle(); i++) await __glSleep(500);
                for (let i = 0; i < 6 && state().indexOf('addToWishlist') >= 0; i++) {
                    toggle().click();
                    await __glSleep(1500);
                }
                let ok = state().indexOf('removeFromWishlist') >= 0;
                out = ok ? [{ uid: '$storeUrl', name: 'added' }] : [];
                console.log('[GL] psn wishlist push added=' + ok + ' ' + location.href);
                """.trimIndent(),
            ),
        )
    }

    // On the game's store page (no locale in the URL: the store picks the account's), the game's own wishlist toggle —
    // a page also carries other editions' toggles and hidden duplicates, so it's matched on `data-qa` and the product
    // (or concept) id in its telemetry — clicked until it reads "addToWishlist". A wishlist uid is the product id, or a
    // concept id when the tile only linked the concept.
    override val wishlistRemove: (ExternalGameRef) -> WebStep = { ref ->
        val kind = if (ref.uid.all(Char::isDigit)) "concept" else "product"
        WebStep(
            "https://store.playstation.com/$kind/${ref.uid}",
            SyncScripts.wrap(
                """
                const id = ${listOf(ref).uidJsonArray()}[0];
                let toggle = function() {
                    return Array.prototype.find.call(document.querySelectorAll('button[data-qa="wishlistToggle"]'), function(b) {
                        try {
                            let meta = JSON.parse(b.getAttribute('data-telemetry-meta') || '{}');
                            return meta.productId === id || String(meta.conceptId) === id;
                        } catch (e) { return false; }
                    });
                };
                let state = function() { let b = toggle(); return b ? b.getAttribute('data-track-click') || '' : ''; };
                for (let i = 0; i < 20 && !toggle(); i++) await __glSleep(500);
                for (let i = 0; i < 6 && state().indexOf('removeFromWishlist') >= 0; i++) {
                    toggle().click();
                    await __glSleep(1500);
                }
                let ok = state().indexOf('addToWishlist') >= 0;
                out = ok ? [{ uid: id, name: '' }] : [];
                console.log('[GL] psn wishlist remove ' + id + ' state=' + state() + ' ' + location.href);
                """.trimIndent(),
            ),
        )
    }
}
