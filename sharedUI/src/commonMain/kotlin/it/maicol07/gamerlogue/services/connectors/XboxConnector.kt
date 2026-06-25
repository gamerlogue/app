package it.maicol07.gamerlogue.services.connectors

import co.touchlab.kermit.Logger
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.XboxApi

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
    override fun loginUrl() = AUTH_URL

    // Logged in once MSA redirects to the desktop landing page. Can't just match "oauth20_desktop.srf"
    // (it's also the redirect_uri param in the authorize URL), and the #access_token fragment may be
    // stripped from the reported URL — so match the landing path while excluding the authorize page.
    override fun isLoggedIn(currentUrl: String) =
        currentUrl.contains("oauth20_desktop.srf") && !currentUrl.contains("authorize")

    // Owned games come from the Xbox Live title history (MSA credential); the wishlist is scraped from
    // the store, and wishlist write opens each product page and clicks add (PSN-style, no batch API).
    override fun ownedViaApi() = true

    override fun pushesPerGame() = true

    // The wishlist DOM lives on the Microsoft Store, a different origin than the login.live.com token
    // login; WebView third-party-cookie blocking stops the store's silent SSO, so the user signs into
    // the store directly (top-level → first-party cookies) before we read/write the wishlist.
    override fun storeLoginUrl() = "https://www.microsoft.com/store/wishlist"

    override fun uidFromUrl(url: String): String? =
        Regex("/([0-9A-Za-z]{12})(?:[/?#]|$)").find(url)?.groupValues?.get(1)

    // Re-run the MBI_SSL OAuth implicit flow against the now-logged-in MSA session; with cookies present
    // it redirects straight to oauth20_desktop.srf with #access_token=… (no re-prompt). Read it same-origin.
    override fun credentialStep() = WebStep(
        AUTH_URL,
        SyncScripts.wrap(
            """
            try {
                let m = (location.hash || '').match(/access_token=([^&]+)/);
                let token = m ? decodeURIComponent(m[1]) : '';
                out = token ? [{ uid: token, name: 'xbl' }] : [];
                console.log('[GL] xbox token got=' + !!token);
            } catch (e) { console.log('[GL] xbox token err ' + e); out = []; }
            """.trimIndent(),
        ),
    )

    override suspend fun apiOwned(credential: String): List<ExternalGameRef> {
        if (credential.isBlank()) return emptyList()
        return runCatching { api.ownedGames(credential) }
            .onFailure { Logger.w(throwable = it) { "Xbox ownedGames failed" } }
            .getOrDefault(emptyList())
    }

    override fun readWishlist() = WebStep(
        "https://www.microsoft.com/store/wishlist",
        SyncScripts.wrap(
            """
            console.log('[GL] xbox wishlist at ' + location.href);
            // Only product anchors carry a 12-char store id; match those, NOT every /p/ link (nav/footer
            // links also have /p/ and made the old poll exit early on a page with zero real tiles).
            let productAnchors = function() {
                return Array.prototype.filter.call(
                    document.querySelectorAll('a[href]'),
                    function(a) { return /\/([0-9A-Za-z]{12})(?:[\/?#]|$)/.test(a.getAttribute('href') || ''); },
                );
            };
            // Tiles render late (React); poll until product anchors appear instead of reading an empty DOM.
            let tiles = await new Promise(function(res) {
                let tries = 0;
                (function tick() {
                    let t = productAnchors();
                    if (t.length || ++tries > 30) return res(t);
                    setTimeout(tick, 500);
                })();
            });
            let result = {};
            tiles.forEach(a => {
                let href = a.getAttribute('href') || '';
                let uid = (href.match(/\/([0-9A-Za-z]{12})(?:[\/?#]|$)/) || [])[1] || '';
                let name = (a.getAttribute('aria-label') || a.textContent || '').trim();
                if (uid && name) result[uid] = { uid: uid, name: name };
            });
            out = Object.values(result);
            console.log('[GL] xbox wishlist games=' + out.length);
            """.trimIndent(),
        ),
    )

    override fun wishlistPushStep(storeUrl: String) = WebStep(
        storeUrl,
        SyncScripts.wrap(
            """
            // Click then VERIFY with retry (same React/SSR timing issue as PSN: the add button renders
            // before its onClick is bound). Success = an add control flips to a remove/added state.
            let wait = function(ms) { return new Promise(function(r) { setTimeout(r, ms); }); };
            let findAdd = function() {
                return Array.prototype.find.call(
                    document.querySelectorAll('button, [role="button"]'),
                    function(b) {
                        let l = ((b.getAttribute('aria-label') || '') + ' ' + (b.textContent || '')).toLowerCase();
                        return b.offsetParent !== null && l.indexOf('wishlist') >= 0 && l.indexOf('add') >= 0;
                    },
                );
            };
            let confirmed = function() {
                return !!Array.prototype.find.call(
                    document.querySelectorAll('button, [role="button"]'),
                    function(b) {
                        let l = ((b.getAttribute('aria-label') || '') + ' ' + (b.textContent || '')).toLowerCase();
                        return l.indexOf('wishlist') >= 0 && (l.indexOf('remove') >= 0 || l.indexOf('added') >= 0 || l.indexOf('in wishlist') >= 0);
                    },
                );
            };
            let attempts = 0;
            for (let i = 0; i < 5 && !confirmed(); i++) {
                let btn = findAdd();
                if (!btn) { await wait(500); continue; }
                attempts++;
                btn.click();
                await wait(1200);
            }
            let ok = confirmed();
            out = ok ? [{ uid: '$storeUrl', name: 'added' }] : [];
            console.log('[GL] xbox wishlist push added=' + ok + ' attempts=' + attempts + ' ' + location.href);
            """.trimIndent(),
        ),
    )

    private companion object {
        // MBI_SSL implicit flow for the official Xbox app client; the token lands in the
        // oauth20_desktop.srf fragment.
        const val AUTH_URL = "https://login.live.com/oauth20_authorize.srf" +
            "?client_id=000000004C12AE6F&response_type=token&display=touch" +
            "&scope=service::user.auth.xboxlive.com::MBI_SSL" +
            "&redirect_uri=https://login.live.com/oauth20_desktop.srf"
    }
}
