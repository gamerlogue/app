package it.maicol07.gamerlogue.services.connectors

import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.services.XboxApi
import it.maicol07.gamerlogue.services.apiProfile
import it.maicol07.gamerlogue.services.apiRefs
import it.maicol07.gamerlogue.services.webRefs

/**
 * Xbox / Microsoft Store — owned titles via the Xbox Live API ([XboxApi]), like PSN.
 *
 * Owned games are XSTS-gated and not in the page DOM, so the WebView's only job is to grab the MSA
 * `access_token` from the logged-in session ([credentialStep] runs the MBI_SSL OAuth implicit flow and
 * reads the token from the redirect fragment); [XboxApi] then runs the user→XSTS token chain and lists
 * the title history off-browser (no CORS). The wishlist has no clean API, so it's scraped from the
 * Microsoft Store wishlist page, and writes open each product page and click add-to-wishlist (per-game,
 * like PSN). `external_game_source=11`.
 *
 * ponytail: the Microsoft Store is React/SSR and changes often — the wishlist read/push selectors and the
 * OAuth client/redirect need live tuning (see PSN for the same pattern that was tuned against the site).
 */
class XboxConnector(private val api: XboxApi) :
    ServiceConnector(ExternalService.XBOX, host = "xbox.com", externalGameSource = 11) {

    override val platformFamily = 2 // IGDB platform_family: Xbox

    // Sign in through the MSA implicit OAuth page itself: it's on login.live.com (so the ServiceWebView
    // actually shows it — it only reveals the WebView for URLs containing "login") and there's no
    // anonymous xbox.com/play page to mistake for a logged-in session.
    override val loginUrl = AUTH_URL

    // Logged in once MSA redirects to the desktop landing page. Can't just match "oauth20_desktop.srf"
    // (it's also the redirect_uri param in the authorize URL), and the #access_token fragment may be
    // stripped from the reported URL — so match the landing path while excluding the authorize page.
    override suspend fun isLoggedIn(currentUrl: String) =
        currentUrl.contains("oauth20_desktop.srf") && !currentUrl.contains("authorize")

    // xbox.com SSOs silently from the MSA session, so no separate store sign-in is needed: the wishlist is
    // read directly (readWishlist navigates xbox.com top-level).

    // MSA login (login.live.com) + the Microsoft/Xbox store origins that carry the signed-in session.
    override val sessionUrls = listOf(
        "https://login.live.com/",
        "https://account.microsoft.com/",
        "https://www.microsoft.com/",
        "https://www.xbox.com/",
    )

    override fun uidFromUrl(url: String): String? =
        Regex("/([0-9A-Za-z]{12})(?:[/?#]|$)").find(url)?.groupValues?.get(1)

    // Re-run the MBI_SSL OAuth implicit flow against the now-logged-in MSA session; with cookies present
    // it redirects straight to oauth20_desktop.srf with #access_token=… (no re-prompt). Read it same-origin.
    private val credentialStep = WebStep(
        AUTH_URL,
        SyncScripts.wrap(
            """
            try {
                let m = (location.hash || '').match(/access_token=([^&]+)/);
                let token = m ? decodeURIComponent(m[1]) : '';
                out = token;
                console.log('[GL] xbox token got=' + !!token);
            } catch (e) { console.log('[GL] xbox token err ' + e); out = ''; }
            """.trimIndent(),
        ),
    )

    // Owned games and profile come from the Xbox Live title history (MSA credential); the wishlist is
    // scraped from the store, and wishlist write opens each product page and clicks add (PSN-style).
    override val ownedGames = apiRefs(credentialStep) { token -> api.ownedGames(token) }

    override val profile = apiProfile(credentialStep) { token -> api.profile(token) }

    // ponytail: locale hardcoded — xbox.com/wishlist 307-redirects to /it-IT/wishlist and the WebView
    // doesn't follow it (shows a 404). Derive the locale from the account region if non-IT users need it.
    override val wishlist = webRefs(
        WebStep(
            "https://www.xbox.com/wishlist",
            SyncScripts.wrap(
                """
                console.log('[GL] xbox wishlist at ' + location.href);
                // Wishlist tiles are CSS-module anchors; the hashed class suffix (___OfDqr) changes per build,
                // so match the stable module prefix substring instead of the full generated class name.
                // They render late (React), so wait for them instead of reading an empty DOM.
                let tiles = await __glWaitForAll('[class*="WishlistProductItem-module__productDetails"] > a[href]');
                let result = {};
                Array.prototype.forEach.call(tiles, a => {
                    let href = a.getAttribute('href') || '';
                    let uid = (href.match(/\/([0-9A-Za-z]{12})(?:[\/?#]|$)/) || [])[1] || '';
                    let name = (a.getAttribute('aria-label') || a.textContent || '').trim();
                    if (uid && name) result[uid] = { uid: uid, name: name };
                });
                out = Object.values(result);
                console.log('[GL] xbox wishlist games=' + out.length);
                """.trimIndent(),
            ),
        ),
    )

    override val wishlistWrite = WishlistWrite.PerGame { storeUrl ->
        WebStep(
            storeUrl,
            SyncScripts.wrap(
                """
                // The add control carries no stable selector, so both states are matched on the
                // (localized) label: it always contains "wishlist", plus "add" or a done word.
                let labelled = function(words) {
                    return Array.prototype.find.call(
                        document.querySelectorAll('button, [role="button"]'),
                        function(b) {
                            let l = ((b.getAttribute('aria-label') || '') + ' ' + (b.textContent || '')).toLowerCase();
                            return b.offsetParent !== null && l.indexOf('wishlist') >= 0 &&
                                words.some(function(w) { return l.indexOf(w) >= 0; });
                        }
                    );
                };
                let ok = await __glClickUntil(
                    function() { return labelled(['add']); },
                    function() { return !!labelled(['remove', 'added', 'in wishlist']); }
                );
                out = ok ? [{ uid: '$storeUrl', name: 'added' }] : [];
                console.log('[GL] xbox wishlist push added=' + ok + ' ' + location.href);
                """.trimIndent(),
            ),
        )
    }

    private companion object {
        // MBI_SSL implicit flow for the official Xbox app client; the token lands in the
        // oauth20_desktop.srf fragment.
        const val AUTH_URL = "https://login.live.com/oauth20_authorize.srf" +
            "?client_id=000000004C12AE6F&response_type=token&display=touch" +
            "&scope=service::user.auth.xboxlive.com::MBI_SSL" +
            "&redirect_uri=https://login.live.com/oauth20_desktop.srf"
    }
}
