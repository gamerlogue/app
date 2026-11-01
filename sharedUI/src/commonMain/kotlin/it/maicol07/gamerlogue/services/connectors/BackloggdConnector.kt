package it.maicol07.gamerlogue.services.connectors

import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.IgdbKey
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.StatusChange
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.services.jsonArray
import it.maicol07.gamerlogue.services.webProfile
import it.maicol07.gamerlogue.services.webRefs
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus

/**
 * Backloggd. A Rails/Turbo site with no JSON list API: reads scrape the server-rendered shelf pages
 * (`/u/{user}/games/`, `/u/{user}/wishlist/`) fetched same-origin and parsed with DOMParser.
 *
 * Backloggd's game ids ARE IGDB ids ([IgdbKey.GAME_ID]) — the site is built on IGDB — so matching is exact and
 * the wishlist write needs no store page: `POST /log/` with the IGDB id and the page's CSRF token.
 *
 * ponytail: the selectors/endpoints come from third-party clients (backloggd-mcp, csv importers), not a live
 * session — verify live. `POST /log/` is a toggle, which is safe only because the push skips games already
 * on the wishlist.
 */
class BackloggdConnector : ServiceConnector(ExternalService.BACKLOGGD, host = "backloggd.com", externalGameSource = 0) {
    override val igdbKey = IgdbKey.GAME_ID

    override val loginUrl = "https://backloggd.com/users/sign_in"

    // Turbo can finish the sign-in without a full page load; /settings/ exists only for a signed-in user.
    override val loginTriggerScript = SyncScripts.leaveLoginWhenSignedIn(
        "fetch('/settings/', { credentials: 'include' }).then(r => r.ok)",
        target = HOME,
    )

    // The sign-in path is "sign_in", which the default "signin" marker doesn't catch.
    override suspend fun isLoggedIn(currentUrl: String) =
        super.isLoggedIn(currentUrl) && !currentUrl.contains("sign_in")

    override val profile = webProfile(WebStep(HOME, SyncScripts.wrap("""
        $USERNAME_JS
        let user = await __blUser();
        let doc = user ? await __blDoc('/u/' + user + '/') : null;
        let og = doc && doc.querySelector('meta[property="og:image"]');
        out = {
            username: user,
            avatarUrl: og ? og.getAttribute('content') || '' : '',
            profileUrl: user ? 'https://backloggd.com/u/' + user + '/' : '',
        };
    """.trimIndent())))

    override val ownedGames = webRefs(WebStep(HOME, shelfScrape("games", withStatus = true)))

    override val wishlist = webRefs(WebStep(HOME, shelfScrape("wishlist", withStatus = false)))

    override val wishlistWrite = WishlistWrite.ById { ids ->
        WebStep(HOME, SyncScripts.wrap("""
            let token = (document.querySelector('meta[name="csrf-token"]') || {}).content || '';
            out = [];
            const ids = $ids;
            for (const [n, id] of ids.entries()) {
                __glProgress(n, ids.length);
                let r = await fetch('/log/', {
                    method: 'POST',
                    credentials: 'include',
                    headers: {
                        'X-CSRF-Token': token,
                        'X-Requested-With': 'XMLHttpRequest',
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: 'type=wishlist&game_id=' + id,
                });
                if (r.ok) out.push({ uid: String(id), name: '' });
            }
        """.trimIndent()))
    }

    // Backloggd keeps "playing" and "backlog" as shelves and every finished state under the "played" shelf
    // with a sub-status (completed=0, abandoned=2, shelved=4). Shelves are toggles, so the old one is
    // switched off before the new one is switched on; a move between played sub-statuses only patches it.
    override val syncStatuses = GameLibraryStatus.entries.toSet()

    override val statusWrite: (List<StatusChange>) -> WebStep = { changes ->
        WebStep(HOME, SyncScripts.wrap("""
            let token = (document.querySelector('meta[name="csrf-token"]') || {}).content || '';
            let send = function(method, path, body) {
                return fetch(path, {
                    method: method,
                    credentials: 'include',
                    headers: {
                        'X-CSRF-Token': token,
                        'X-Requested-With': 'XMLHttpRequest',
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: body,
                });
            };
            let played = { COMPLETED: 0, ABANDONED: 2, PAUSED: 4 };
            let shelf = function(s) { return s in played ? 'play' : (s === 'PLAYING' ? 'playing' : 'backlog'); };
            out = [];
            const changes = ${changes.jsonArray()};
            for (const [n, c] of changes.entries()) {
                __glProgress(n, changes.length);
                let from = c.from ? shelf(c.from) : null, to = shelf(c.to), ok = true;
                if (from && from !== to) ok = (await send('POST', '/log/', 'type=' + from + '&game_id=' + c.gameId)).ok;
                if (ok && from !== to) ok = (await send('POST', '/log/', 'type=' + to + '&game_id=' + c.gameId)).ok;
                if (ok && to === 'play') {
                    ok = (await send('PATCH', '/log/status/', 'game_id=' + c.gameId + '&status_id=' + played[c.to])).ok;
                }
                if (ok) out.push({ uid: String(c.gameId), name: c.name, gameId: c.gameId });
            }
        """.trimIndent()))
    }

    // Every shelf page is the same card grid: `.card.game-cover[game_id]` with the title in the cover's alt
    // and the user's log state in a sibling `#preloaded-log-{id}` whose shelf attributes are timestamps or
    // empty strings (non-empty = on that shelf). Paginated until a page has no cards.
    private fun shelfScrape(shelf: String, withStatus: Boolean) = SyncScripts.wrap("""
        $USERNAME_JS
        let user = await __blUser();
        out = [];
        let on = function(el, attr) { return !!(el && el.getAttribute(attr)); };
        let status = function(log) {
            if (on(log, 'data-is-playing')) return 'PLAYING';
            if (on(log, 'data-is-play')) {
                let sub = ((log && log.getAttribute('data-status-title')) || '').toLowerCase();
                if (sub.indexOf('abandon') >= 0) return 'ABANDONED';
                if (sub.indexOf('shelve') >= 0) return 'PAUSED';
                // completed / retired (finished an endless game) / plain played
                return 'COMPLETED';
            }
            if (on(log, 'data-is-backlog')) return 'BACKLOG';
            return null;
        };
        // A `return` here would skip the wrapper's result delivery, so an unknown user just runs no page.
        for (let page = 1; user && page <= 200; page++) {
            let doc = await __blDoc('/u/' + user + '/$shelf/?page=' + page);
            let cards = doc ? doc.querySelectorAll('.card.game-cover[game_id]') : [];
            if (!cards.length) break;
            cards.forEach(function(card) {
                let id = card.getAttribute('game_id');
                let img = card.querySelector('img');
                let ref = { uid: id, name: img ? img.getAttribute('alt') || '' : '' };
                if ($withStatus) {
                    let st = status(doc.getElementById('preloaded-log-' + id));
                    // Wishlist-only games belong to the wishlist read, not the tracked library.
                    if (!st) return;
                    ref.status = st;
                }
                out.push(ref);
            });
            __glProgress(out.length, 0);
        }
    """.trimIndent())

    private companion object {
        const val HOME = "https://backloggd.com/"

        // The signed-in username: the settings form holds it; the navbar profile link is the fallback.
        val USERNAME_JS = """
            async function __blDoc(path) {
                let r = await fetch(path, { credentials: 'include' });
                return r.ok ? new DOMParser().parseFromString(await r.text(), 'text/html') : null;
            }
            async function __blUser() {
                let settings = await __blDoc('/settings/');
                let input = settings && settings.querySelector('input[name="user[username]"]');
                if (input && input.value) return input.value;
                let a = document.querySelector('nav a[href^="/u/"]');
                let m = a && a.getAttribute('href').match(/^\/u\/([^\/?#]+)/);
                return m ? m[1] : '';
            }
        """.trimIndent()
    }
}
