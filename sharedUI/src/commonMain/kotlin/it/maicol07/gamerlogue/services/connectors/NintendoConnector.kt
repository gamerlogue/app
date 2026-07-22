package it.maicol07.gamerlogue.services.connectors

import com.parkwoocheol.composewebview.PlatformCookieManager
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.services.webProfile
import it.maicol07.gamerlogue.services.webRefs

/**
 * Nintendo. Login is the global Nintendo Account portal (accounts.nintendo.com). The library is read from
 * the Virtual Game Cards portal (accounts.nintendo.com/portal/vgcs) and the wishlist from the eShop account
 * page (ec.nintendo.com/my/wishlist); both are card grids whose items link to /titles/{id}.
 *
 * Nintendo has no public API and IGDB has no reliable eShop `external_game_source` / URL mapping, so games
 * are matched by **name** ([storeUrlTemplate] left null, [idMatchesUid] off) via [GameMatcher]'s fallback —
 * hence every read returns the game title in `name`.
 *
 * ponytail: like PSN/Xbox/Epic, the injected JS and endpoints below are best-effort and tuned live on the
 * Android WebView; Nintendo exposes no clean same-origin JSON like GOG/Steam.
 */
class NintendoConnector : ServiceConnector(ExternalService.NINTENDO, host = "nintendo.com", externalGameSource = 0) {
    // IGDB platform_family for Nintendo (PS=1, Xbox=2). Only used to flag on-platform wishlist pushes.
    // ponytail: confirm the real id against IGDB's platform_family endpoint; wrong value only mis-flags onPlatform.
    override val platformFamily = 5

    override val loginUrl = ACCOUNT

    override val sessionUrls = listOf(ACCOUNT, "https://ec.nintendo.com/")

    // The portal login page, its 2FA/passkey pages and the signed-in dashboard all live on
    // accounts.nintendo.com, so a URL marker can't tell them apart. Detect the session from the account
    // session cookie instead: NASID (Nintendo Account Session ID) is only set once signed in — during the
    // whole login/2FA flow the cookies are just NATID/NAUS/Akamai (NAUS actually drops after login).
    override suspend fun isLoggedIn(currentUrl: String): Boolean {
        if (!currentUrl.contains("accounts.nintendo.com")) return false
        val cookies = PlatformCookieManager.getCookies("https://accounts.nintendo.com")
        return cookies.any { it.name == "NASID" && it.value.isNotBlank() }
    }

    // eShop title pages are /titles/{id}; expose the id so IGDB `websites` can be matched for push.
    override fun uidFromUrl(url: String) = Regex("/titles/(\\d+)").find(url)?.groupValues?.get(1)

    // Read the profile from the dashboard DOM: the api.accounts.nintendo.com/users/me endpoint is a
    // different origin and bearer-auth, so a same-origin fetch is blocked by CORS. The nickname/avatar are
    // rendered in the account header. ponytail: poll briefly since the header hydrates after load.
    override val profile = webProfile(WebStep(ACCOUNT, SyncScripts.wrap("""
        let nick = await new Promise(function(res) {
            let tries = 0;
            (function tick() {
                let el = document.querySelector('.c-user_nickname');
                if ((el && el.textContent.trim()) || ++tries > 30) return res(el);
                setTimeout(tick, 300);
            })();
        });
        let img = document.querySelector('.c-avatorIcon > img');
        out = {
            username: nick ? nick.textContent.trim() : '',
            avatarUrl: img ? (img.getAttribute('src') || '') : '',
            profileUrl: 'https://accounts.nintendo.com/',
        };
    """.trimIndent())))

    // Library (Virtual Game Cards portal) and wishlist (eShop account page) are the same shape of card grid,
    // so both use the same /titles/{id} scrape; only the URL differs. ponytail: best-effort, tuned live —
    // the VGCS markup is unverified (author has no owned Nintendo games), but it mirrors the wishlist grid.
    override val ownedGames = webRefs(WebStep(OWNED, titlesScrape()))

    override val wishlist = webRefs(WebStep(WISHLIST, titlesScrape()))

    // Push is a two-hop: IGDB gives a www.nintendo.com detail URL, but the add button lives on the eShop
    // product page. resolve() reads the numeric title id from the page's eShop buy link
    // (ec.nintendo.com/title_purchase_confirm?title={id}) and builds the region-less ec product URL
    // (ec redirects it to the account's region, as /my/wishlist does). step() then clicks .btn-wishlist —
    // a text-only toggle, but computeWishlistPush only sends not-yet-wishlisted games, so a click adds.
    override val wishlistWrite = WishlistWrite.PerGameResolved(
        resolve = { storeUrl ->
            WebStep(storeUrl, SyncScripts.wrap("""
                let a = document.querySelector('a[href*="title_purchase_confirm"]');
                let id = a ? new URL(a.href).searchParams.get('title') : '';
                out = id ? [{ uid: 'https://ec.nintendo.com/titles/' + id, name: '' }] : [];
            """.trimIndent()))
        },
        step = { ecUrl ->
            WebStep(ecUrl, SyncScripts.wrap("""
                let btn = await new Promise(function(res) {
                    let tries = 0;
                    (function tick() {
                        let b = document.querySelector('.btn-wishlist');
                        if (b || ++tries > 30) return res(b);
                        setTimeout(tick, 500);
                    })();
                });
                if (btn) { btn.click(); await new Promise(function(r) { setTimeout(r, 1000); }); }
                out = btn ? [{ uid: '$ecUrl', name: 'added' }] : [];
            """.trimIndent()))
        },
    )

    // Poll for the React-rendered cards, then read every /titles/{id} link (uid) and its cover <img alt>
    // (name; matching is name-based). Deduped by uid since each card links to the same title several times.
    // IGDB has no Nintendo source, so matching is by name only — strip the eShop platform/edition noise
    // ("Xenoblade Chronicles: Definitive Edition – Nintendo Switch 2 Edition" → "Xenoblade Chronicles:
    // Definitive Edition") that otherwise breaks the IGDB fuzzy search.
    private fun titlesScrape() = SyncScripts.wrap("""
        let clean = function(s) {
            return (s || '')
                .replace(/\s*[–—-]\s*Nintendo Switch.*$/i, '')
                .replace(/\s+for Nintendo Switch.*$/i, '')
                .replace(/\s+/g, ' ').trim();
        };
        let anchors = await new Promise(function(res) {
            let tries = 0;
            (function tick() {
                let a = document.querySelectorAll('a[href*="/titles/"]');
                if (a.length || ++tries > 30) return res(a);
                setTimeout(tick, 500);
            })();
        });
        let map = {};
        anchors.forEach(function(a) {
            let m = (a.getAttribute('href') || '').match(/\/titles\/(\d+)/);
            if (!m) return;
            let uid = m[1];
            let img = a.querySelector('img[alt]');
            let name = clean(img ? img.getAttribute('alt') : a.textContent);
            if (!map[uid]) map[uid] = { uid: uid, name: name };
            else if (!map[uid].name && name) map[uid].name = name;
        });
        out = Object.values(map);
    """.trimIndent())

    private companion object {
        const val ACCOUNT = "https://accounts.nintendo.com/"
        const val OWNED = "https://accounts.nintendo.com/portal/vgcs"
        const val WISHLIST = "https://ec.nintendo.com/my/wishlist"
    }
}
