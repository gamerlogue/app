package it.maicol07.gamerlogue.services.connectors

import com.parkwoocheol.composewebview.PlatformCookieManager
import io.ktor.http.encodeURLParameter
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.GameMatcher
import it.maicol07.gamerlogue.services.NintendoApi
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.services.byTitle
import it.maicol07.gamerlogue.services.uidJsonArray
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
class NintendoConnector(private val api: NintendoApi) :
    ServiceConnector(ExternalService.NINTENDO, host = "nintendo.com", externalGameSource = 0) {
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
    // rendered in the account header, which hydrates after load — hence the wait on a *filled* nickname.
    override val profile = webProfile(WebStep(ACCOUNT, SyncScripts.wrap("""
        let nick = await __glWaitFor(function() {
            let el = document.querySelector('.c-user_nickname');
            return el && el.textContent.trim() ? el : null;
        }, 30, 300);
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

    // IGDB links US store pages, and an eShop id is per region, so a push finds the game by name for the account's
    // region (read off the eShop's own session). A European account searches Europe's eShop off-WebView and opens the
    // eShop page for its country and language, which forwards to the game's page on its Nintendo site; there the
    // wishlist button is a text-only toggle, confirmed by its label changing. A US account searches nintendo.com in the
    // WebView (its search runs in the page only) and opens the product page found, where the wishlist button reports
    // its state in aria-pressed. computeWishlistPush only sends games not on the wishlist, so a click adds.
    // ponytail: the US product-page click is read off the public page, not tried with a US account; other regions
    // (Japan, the rest of the Americas) are skipped as unsupported.
    override val wishlistWrite = WishlistWrite.ResolvedByName(
        account = WebStep(
            WISHLIST,
            SyncScripts.wrap(
                """
                let s = (await (await fetch('/api/auth/session', { credentials: 'include' })).json()) || {};
                let l = s.localeInfo || {};
                let ok = (l.region === 'EUR' || l.country === 'US') && l.country && l.language;
                out = ok ? [{ uid: l.country + '/' + l.language, name: '' }] : [];
                console.log('[GL] nintendo account ' + JSON.stringify(l));
                """.trimIndent(),
            ),
        ),
        // The account is the eShop's own "COUNTRY/language" path segment (e.g. "IT/it").
        resolve = { name, account, run ->
            if (account.startsWith("US/")) {
                run(usSearch(name)).map { it.name to it.uid }.byTitle(name)
            } else {
                api.europeanTitleId(name)?.let { "https://ec.nintendo.com/$account/titles/$it" }
            }
        },
        step = { pageUrl -> WebStep(pageUrl, if (pageUrl.startsWith(US_SITE)) US_PUSH else EUROPEAN_PUSH) },
    )

    // nintendo.com's search, results rendered by the page: each product link is labelled with the game's full title.
    private fun usSearch(name: String) = WebStep(
        "$US_SITE/us/search/?q=${name.encodeURLParameter()}",
        SyncScripts.wrap(
            """
            let links = await __glWaitForAll('a[aria-label][href*="/store/products/"]');
            out = Array.prototype.map.call(links, function(a) {
                return { uid: a.href, name: a.getAttribute('aria-label').replace(/ /g, ' ') };
            });
            console.log('[GL] nintendo us search results=' + out.length);
            """.trimIndent(),
        ),
    )

    // Wait for the React-rendered cards, then read every /titles/{id} link (uid) and its cover <img alt>
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
        let anchors = await __glWaitForAll('a[href*="/titles/"]');
        let map = {};
        Array.prototype.forEach.call(anchors, function(a) {
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

    // The game's page is reached through its wishlist link, which carries the account's region (a region-less eShop
    // link doesn't open); none means the game isn't on the wishlist.
    override val wishlistRemovePage: (ExternalGameRef) -> WebStep = { ref ->
        WebStep(WISHLIST, SyncScripts.wrap("""
            const id = ${listOf(ref).uidJsonArray()}[0];
            await __glWaitForAll('a[href*="/titles/"]');
            let a = document.querySelector('a[href*="/titles/' + id + '"]');
            out = a ? [{ uid: a.href, name: '' }] : [];
        """.trimIndent()))
    }

    // There, the wishlist button is a text-only toggle: on a wishlisted game, a click removes it. Its label changes with
    // the state, so a click that left the label as it was didn't take.
    override val wishlistRemove: (ExternalGameRef) -> WebStep = { ref ->
        WebStep(
            WISHLIST,
            SyncScripts.wrap("""
                const id = ${listOf(ref).uidJsonArray()}[0];
                let btn = await __glWaitForEl('.btn-wishlist');
                let label = function() { return (document.querySelector('.btn-wishlist') || btn).textContent.trim(); };
                let before = btn ? label() : '';
                if (btn) btn.click();
                for (let i = 0; btn && i < 10 && label() === before; i++) await __glSleep(500);
                out = btn && label() !== before ? [{ uid: id, name: '' }] : [];
                console.log('[GL] nintendo wishlist remove ' + id + ' ' + JSON.stringify([before, btn ? label() : '']));
            """.trimIndent()),
        )
    }

    private companion object {
        const val US_SITE = "https://www.nintendo.com"

        // The European product page's wishlist button: text-only, so a click is confirmed by its label changing.
        val EUROPEAN_PUSH = SyncScripts.wrap(
            """
            let btn = await __glWaitForEl('.btn-wishlist');
            let label = function() { return (document.querySelector('.btn-wishlist') || btn).textContent.trim(); };
            let before = btn ? label() : '';
            if (btn) btn.click();
            for (let i = 0; btn && i < 10 && label() === before; i++) await __glSleep(500);
            let ok = !!btn && label() !== before;
            out = ok ? [{ uid: location.href, name: 'added' }] : [];
            console.log('[GL] nintendo wishlist push added=' + ok + ' ' + location.href);
            """.trimIndent(),
        )

        // The US product page's wishlist button ("Add to Wish List", one per layout): its aria-pressed is the state.
        val US_PUSH = SyncScripts.wrap(
            """
            let btn = function(pressed) {
                return Array.prototype.find.call(document.querySelectorAll('button[aria-pressed]'), function(b) {
                    return b.offsetParent !== null && /wish/i.test(b.getAttribute('aria-label') || '') &&
                        b.getAttribute('aria-pressed') === pressed;
                });
            };
            let ok = await __glClickUntil(function() { return btn('false'); }, function() { return !!btn('true'); }, 6);
            out = ok ? [{ uid: location.href, name: 'added' }] : [];
            console.log('[GL] nintendo us wishlist push added=' + ok + ' ' + location.href);
            """.trimIndent(),
        )

        const val ACCOUNT = "https://accounts.nintendo.com/"
        const val OWNED = "https://accounts.nintendo.com/portal/vgcs"
        const val WISHLIST = "https://ec.nintendo.com/my/wishlist"
    }
}
