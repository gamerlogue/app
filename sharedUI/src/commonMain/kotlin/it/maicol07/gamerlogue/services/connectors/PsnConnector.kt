package it.maicol07.gamerlogue.services.connectors

import co.touchlab.kermit.Logger
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.PsnApi
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep

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

    override fun loginUrl() = "https://library.playstation.com/"

    // Owned games come from the trophy API (npsso credential); the wishlist has no clean API, so it's
    // scraped from the store wishlist page instead. Wishlist write to PSN isn't supported (no API).
    override fun ownedViaApi() = true

    // Push backlog games one at a time by opening their PS Store page (from IGDB `external_games.url`,
    // source 36) and clicking the add-to-wishlist button — PSN has no wishlist write API.
    override fun pushesPerGame() = true

    // Strip the `/xx-xx` locale segment so the store redirects to the user's own region/account.
    override fun normalizePushUrl(url: String) =
        url.replace(Regex("(store\\.playstation\\.com)/[a-z]{2}-[a-z]{2}/"), "$1/")

    override fun uidFromUrl(url: String): String? =
        Regex("/(?:concept|product)/([^/?#]+)").find(url)?.groupValues?.get(1)

    override fun wishlistPushStep(storeUrl: String) = WebStep(
        storeUrl,
        SyncScripts.wrap(
            """
            // Click then VERIFY, with retry. The store is React+SSR: the add button's HTML renders before
            // its onClick is bound, so a single early click hits a dead handler and silently does nothing
            // (this was the bug — clicked=true but nothing added). Success = the button flips to the
            // already-wishlisted "removeFromWishlist" variant, so we re-click until that appears.
            let wait = function(ms) { return new Promise(function(r) { setTimeout(r, ms); }); };
            let confirmed = function() { return !!document.querySelector('button[data-track-click*="removeFromWishlist"]'); };
            let attempts = 0;
            for (let i = 0; i < 5 && !confirmed(); i++) {
                // First VISIBLE add button (offsetParent!==null skips hidden edition-picker duplicates).
                let btn = Array.prototype.find.call(
                    document.querySelectorAll('button[data-track-click*="addToWishlist"]'),
                    function(b) { return b.offsetParent !== null; },
                );
                if (!btn) { await wait(500); continue; } // still rendering
                attempts++;
                btn.click();
                await wait(1200); // let React handle the click + the add request fire
            }
            let ok = confirmed();
            out = ok ? [{ uid: '$storeUrl', name: 'added' }] : [];
            console.log('[GL] psn wishlist push added=' + ok + ' attempts=' + attempts + ' ' + location.href);
            """.trimIndent(),
        ),
    )

    override fun readWishlist() = WebStep(
        "https://library.playstation.com/wishlist",
        SyncScripts.wrap(
            """
            console.log('[GL] psn wishlist at ' + location.href);
            // Tiles render late (React); poll until they appear instead of reading once on an empty DOM
            // (that one-shot read was returning an empty wishlist).
            let tiles = await new Promise(function(res) {
                let tries = 0;
                (function tick() {
                    let t = document.querySelectorAll('[data-track-click="web:product-tile-click"]');
                    if (t.length || ++tries > 30) return res(t);
                    setTimeout(tick, 500);
                })();
            });
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
    )

    // Read the npsso JSON same-origin and deliver it as the single ref's uid.
    override fun credentialStep() = WebStep(
        "https://ca.account.sony.com/api/v1/ssocookie",
        SyncScripts.wrap(
            """
            try {
                let npsso = (JSON.parse(document.body.innerText || '{}').npsso) || '';
                out = npsso ? [{ uid: npsso, name: 'npsso' }] : [];
            } catch (e) { console.log('[GL] psn npsso err ' + e); out = []; }
            """.trimIndent(),
        ),
    )

    override suspend fun apiOwned(credential: String): List<ExternalGameRef> {
        if (credential.isBlank()) return emptyList()
        return runCatching { api.ownedGames(api.accessToken(credential)) }
            .onFailure { Logger.w(throwable = it) { "PSN ownedGames failed" } }
            .getOrDefault(emptyList())
    }
}
