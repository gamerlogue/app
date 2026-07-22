package it.maicol07.gamerlogue.services.connectors

import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.UbisoftApi
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.services.apiRefs
import it.maicol07.gamerlogue.services.webProfile
import it.maicol07.gamerlogue.services.webRefs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Ubisoft Connect — **PC only**: Ubisoft's console releases (PlayStation/Xbox/Switch) are already
 * covered by their own platform connectors, so this one only ever reflects the PC (Ubisoft Connect
 * launcher) copy of a game ([platformFamily] is left null, like Steam/GOG/Epic).
 *
 * Ubisoft has no `external_game_source` entry in IGDB's (deprecated but still the only fixed-id one)
 * external-game enum, so games are matched by **name** ([storeUrlTemplate] left null), like Nintendo.
 *
 * Owned games are read the way the actively-maintained `galaxy-integration-uplay` fork for Uplay does
 * it: the WebView only captures the session already established by `connect.ubisoft.com` login (ticket,
 * session id), then [UbisoftApi] calls Ubisoft's REST/GraphQL APIs from Kotlin (a different origin, so a
 * same-origin browser `fetch` would hit CORS). No account password is ever POSTed to Ubisoft's login
 * endpoint (documented to 429/lock out the account) — only the ticket the browser session already
 * holds is reused. The profile is scraped from the account settings page DOM instead, since the Connect
 * session carries no display name/avatar worth trusting.
 *
 * ponytail: the wishlist has no known API (GOG Galaxy's integration doesn't implement it either), so
 * it's a best-effort DOM scrape/click on the Ubisoft Store, tuned live like Epic/PSN/Nintendo's.
 */
class UbisoftConnector(private val api: UbisoftApi) :
    ServiceConnector(ExternalService.UBISOFT, host = "ubisoft.com", externalGameSource = 0) {
    override val loginUrl =
        "https://connect.ubisoft.com/login?appId=$AppId&genomeId=$GenomeId&lang=en-US" +
            "&nextUrl=https:%2F%2Fconnect.ubisoft.com%2F"

    override val sessionUrls = listOf(
        "https://connect.ubisoft.com/",
        "https://account.ubisoft.com/",
        "https://www.ubisoft.com/",
        "https://store.ubisoft.com/",
        "https://public-ubiservices.ubi.com/",
    )

    // Ubisoft Store product pages are /{locale}/{slug}/{productId}.html; expose the product id so IGDB
    // `websites` can be matched for wishlist push (name-based owned/wishlist matching still drives the
    // main import).
    override fun uidFromUrl(url: String) = Regex("ubisoft\\.com/(?:[a-z]{2}/)?[^/]+/([0-9a-f]{24})\\.html").find(url)?.groupValues?.get(1)

    // Games are name-matched with no store URL to confirm the match, so a random backlog game (e.g. from
    // another publisher entirely) would otherwise be just as "pushable" as an actual Ubisoft title. Only
    // allow the search-by-name push for games IGDB actually credits to Ubisoft.
    override fun matchesPublisher(game: Game) =
        game.involved_companies.orEmpty().any { it.company?.name?.contains("ubisoft", ignoreCase = true) == true }

    // Read the session the Connect login already stored in localStorage (ticket/sessionId) and hand it
    // on as a JSON-encoded credential string.
    private val credentialStep = WebStep(
        "https://connect.ubisoft.com/",
        SyncScripts.wrap(
            """
            let session = null;
            for (let i = 0; i < localStorage.length && !session; i++) {
                let key = localStorage.key(i);
                try {
                    let val = JSON.parse(localStorage.getItem(key) || 'null');
                    if (val && val.ticket && val.sessionId) session = val;
                } catch (e) {}
            }
            console.log('[GL] ubisoft session=' + (session ? 'ok' : 'empty'));
            out = session ? [{
                uid: JSON.stringify({ ticket: session.ticket, sessionId: session.sessionId }),
                name: 'session',
            }] : [];
            """.trimIndent(),
        ),
    )

    override val ownedGames = apiRefs(credentialStep) { credential -> api.ownedGames(credential.toSession()) }

    // The Connect API's own session data carries no display name/avatar worth trusting, so the profile
    // is scraped from the account settings page instead — a React SPA, so poll until it hydrates.
    override val profile = webProfile(
        WebStep(
            "https://www.ubisoft.com/account/account-information",
            SyncScripts.wrap(
                """
                let waitFor = function(selector) {
                    return new Promise(function(res) {
                        let tries = 0;
                        (function tick() {
                            let el = document.querySelector(selector);
                            if (el || ++tries > 30) return res(el);
                            setTimeout(tick, 500);
                        })();
                    });
                };
                let usernameInput = await waitFor('input[data-e2e="textfield-input"]');
                let avatarImg = document.querySelector('[data-e2e="avatar"] img');
                out = {
                    username: usernameInput ? (usernameInput.value || '') : '',
                    avatarUrl: avatarImg ? (avatarImg.getAttribute('src') || '') : '',
                    profileUrl: 'https://www.ubisoft.com/account/account-information',
                };
                console.log('[GL] ubisoft profile username=' + (usernameInput ? 'ok' : 'empty'));
                """.trimIndent(),
            ),
        ),
    )

    // Best-effort wishlist read from the Ubisoft Store account page; no known JSON endpoint, so this
    // scrapes rendered wishlist cards (poll since the store is client-rendered).
    override val wishlist = webRefs(
        WebStep(
            "https://store.ubisoft.com/wishlist",
            SyncScripts.wrap(
                """
                let cards = await new Promise(function(res) {
                    let tries = 0;
                    (function tick() {
                        let c = document.querySelectorAll('.items-in-wishlist .product-tile[data-itemid]');
                        if (c.length || ++tries > 40) return res(c);
                        setTimeout(tick, 500);
                    })();
                });
                if (!cards.length) {
                    cards = document.querySelectorAll('.product-tile[data-itemid]');
                }
                console.log('[GL] ubisoft wishlist url=' + location.href
                    + ' scoped=' + document.querySelectorAll('.items-in-wishlist .product-tile[data-itemid]').length
                    + ' any=' + document.querySelectorAll('.product-tile[data-itemid]').length
                    + ' wrapper=' + document.querySelectorAll('.items-in-wishlist').length);
                let map = {};
                cards.forEach(function(c) {
                    let uid = c.getAttribute('data-itemid');
                    let titleEl = c.querySelector('.prod-title');
                    let name = (titleEl ? titleEl.textContent : '').trim();
                    if (uid && name && !map[uid]) map[uid] = { uid: uid, name: name };
                });
                out = Object.values(map);
                console.log('[GL] ubisoft wishlist games=' + out.length);
                """.trimIndent(),
            ),
        ),
    )

    // Push by searching the store's header Algolia search box for the game name (no reliable IGDB store
    // URL exists — games are name-matched), then clicking the first exact-title result's wishlist button.
    override val wishlistWrite = WishlistWrite.SearchByName { name ->
        val escaped = name.jsEscaped()
        WebStep(
            "https://store.ubisoft.com/",
            SyncScripts.wrap(
                """
                let wait = function(ms) { return new Promise(function(r) { setTimeout(r, ms); }); };
                let waitFor = function(selector) {
                    return new Promise(function(res) {
                        let tries = 0;
                        (function tick() {
                            let el = document.querySelector(selector);
                            if (el || ++tries > 40) return res(el);
                            setTimeout(tick, 500);
                        })();
                    });
                };
                let waitForAll = function(selector) {
                    return new Promise(function(res) {
                        let tries = 0;
                        (function tick() {
                            let els = document.querySelectorAll(selector);
                            if (els.length || ++tries > 40) return res(els);
                            setTimeout(tick, 500);
                        })();
                    });
                };
                let input = await waitFor('#searchbox .ais-SearchBox-input');
                if (!input) {
                    out = [];
                    console.log('[GL] ubisoft wishlist push: no search box');
                } else {
                    input.focus();
                    let nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
                    nativeSetter.call(input, '$escaped');
                    input.dispatchEvent(new Event('input', { bubbles: true }));
                    input.dispatchEvent(new Event('change', { bubbles: true }));
                    input.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true, key: 'a' }));
                    await wait(800);
                    let keywordEl = document.querySelector('.algolia-search-result[data-search-keyword]');
                    console.log('[GL] ubisoft wishlist search: inputValue=' + input.value
                        + ' recordedKeyword=' + (keywordEl ? keywordEl.getAttribute('data-search-keyword') : 'none'));
                    let cards = await waitForAll('#search-result-items .algolia-producttile-card');
                    let wanted = '$escaped'.toLowerCase();
                    let target = null;
                    cards.forEach(function(c) {
                        if (target) return;
                        let link = c.querySelector('.product-hit-link');
                        let gameName = (link ? (link.getAttribute('data-game-name') || '') : '').toLowerCase();
                        if (gameName === wanted) target = c;
                    });
                    if (!target) target = cards[0] || null;
                    let btn = target ? target.querySelector('.add-to-wishlist') : null;
                    if (!btn) {
                        out = [];
                        console.log('[GL] ubisoft wishlist push: no result for $escaped');
                    } else {
                        if (!btn.classList.contains('product-added')) {
                            btn.click();
                            for (let i = 0; i < 10 && !btn.classList.contains('product-added'); i++) await wait(500);
                        }
                        let added = btn.classList.contains('product-added');
                        out = added ? [{ uid: '$escaped', name: 'added' }] : [];
                        console.log('[GL] ubisoft wishlist push added=' + added + ' name=$escaped');
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    private fun String.jsEscaped() = replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")

    private fun String.toSession(): UbisoftApi.Session {
        val obj = Json.parseToJsonElement(this).jsonObject
        return UbisoftApi.Session(
            ticket = obj["ticket"]?.jsonPrimitive?.content.orEmpty(),
            sessionId = obj["sessionId"]?.jsonPrimitive?.content.orEmpty(),
        )
    }

    private companion object {
        // Fixed Ubisoft Connect client app/genome ids (matches the actively-maintained
        // galaxy-integration-uplay fork's current values — the older Club-only app id this used to share
        // with [UbisoftApi] is now rejected by Ubisoft's gateway for the games/ownership APIs).
        const val AppId = "f68a4bb5-608a-4ff2-8123-be8ef797e0a6"
        const val GenomeId = "954e66a0-be1b-4aa0-9690-fb75201e4e9e"
    }
}
