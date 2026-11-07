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
 * Infinite Backlog. A Vue SPA over a same-origin REST API under `/api` (`users/me/complete`,
 * `user_collections`, `user_wishlists`), signed in through a same-origin Keycloak (`/auth`, realm `ib`,
 * public client `web-application`). The SPA sends a Keycloak bearer token it keeps to itself, so the reads
 * get their own: a silent `prompt=none` authorization-code + PKCE round trip through the app's own
 * `/silent-check-sso.html` in a hidden iframe, exactly as keycloak-js does on load.
 *
 * Sign-in opens that same authorization endpoint interactively ([loginUrl]): the Keycloak form, then a
 * redirect to `/silent-check-sso.html#…code=…` once signed in. The SPA has no `/login` route (its router
 * shows a 404 there).
 *
 * Its games are IGDB documents served as-is (`/api/games/slug/hollow-knight` → `id` 14593, IGDB's own), so the
 * game id is the IGDB id ([IgdbKey.GAME_ID]) and matching is exact. Library sync writes through the same
 * `user_collections` calls the site makes (see [statusWrite]); wishlist pushes post `user_wishlists` like its
 * "add to wishlist" button.
 *
 * ponytail: the OIDC endpoints/redirect were checked against the live server; API response shapes are still
 * guessed defensively. Verify live.
 */
class InfiniteBacklogConnector :
    ServiceConnector(ExternalService.INFINITE_BACKLOG, host = "infinitebacklog.net", externalGameSource = 0) {
    override val igdbKey = IgdbKey.GAME_ID

    // A fixed PKCE challenge: this sign-in's code is never exchanged (reads mint their own), Keycloak just
    // needs the request to be complete.
    override val loginUrl = "$AUTH?client_id=$CLIENT&redirect_uri=$SILENT_SSO_ENCODED&state=gamerlogue" +
        "&response_mode=fragment&response_type=code&scope=openid" +
        "&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&code_challenge_method=S256"

    override val sessionUrls = listOf(HOME, "https://infinitebacklog.net/auth/realms/ib/")

    // Signed in = Keycloak handed a code back to the app's silent-SSO page (an error, e.g. a cancelled
    // sign-in, lands there too, but without a code).
    override suspend fun isLoggedIn(currentUrl: String) =
        currentUrl.startsWith(SILENT_SSO) && currentUrl.contains("code=")

    override val profile = webProfile(WebStep(HOME, SyncScripts.wrap("""
        $API_JS
        let me = await __ibMe();
        let av = me.avatar ? String(me.avatar) : '';
        out = {
            username: me.username || '',
            avatarUrl: !av ? '' : /^\d+$/.test(av) ? 'https://images.infinitebacklog.net/avatars/' + av + '.png' : av,
            profileUrl: me.username ? 'https://infinitebacklog.net/users/' + encodeURIComponent(me.username) : '',
        };
    """.trimIndent())))

    // `status`: noStatus | unplayed | playing | played; `completion`: notStarted | unfinished |
    // campaignCompleted | completed | continuous | dropped. Completion is the finer signal, so it wins.
    override val ownedGames = webRefs(WebStep(HOME, SyncScripts.wrap("""
        $API_JS
        let me = await __ibMe();
        let status = function(it) {
            let c = it.completion || it.completion_status || '', s = it.status || '';
            if (c === 'dropped') return 'ABANDONED';
            if (c === 'campaignCompleted' || c === 'completed') return 'COMPLETED';
            if (s === 'playing' || c === 'continuous') return 'PLAYING';
            if (s === 'played' && c === 'unfinished') return 'PAUSED';
            return 'BACKLOG';
        };
        if (!me.id) throw new Error('Infinite Backlog session is missing');
        out = (await __ibPages('/api/user_collections?user_id=' + me.id)).map(function(it) {
            return { uid: __ibUid(it), name: __ibName(it), status: status(it) };
        });
    """.trimIndent())))

    // Every status of ours has a status + completion pair, the same ones the library read maps back.
    override val syncStatuses = GameLibraryStatus.entries.toSet()

    // What the site's own forms send. An update re-sends the whole stored entry (`GET user_collections?
    // game_id=`, as the edit page loads it) with only status + completion changed, so playtimes, sessions and
    // notes stay; an add posts the site's quick-add defaults, on the user's platform (skipped when unknown:
    // the site would ask for one too).
    override val statusWrite: (List<StatusChange>) -> WebStep = { changes ->
        WebStep(HOME, SyncScripts.wrap("""
            $API_JS
            let me = await __ibMe();
            let target = function(to, current) {
                if (to === 'BACKLOG') return ['unplayed', 'notStarted'];
                if (to === 'PLAYING') return ['playing', current === 'continuous' ? 'continuous' : 'unfinished'];
                if (to === 'PAUSED') return ['played', 'unfinished'];
                if (to === 'COMPLETED') return ['played', current === 'completed' ? 'completed' : 'campaignCompleted'];
                return ['played', 'dropped'];
            };
            const changes = ${changes.jsonArray()};
            out = [];
            for (const [n, c] of changes.entries()) {
                if (__glCancelled()) break;
                __glProgress(n, changes.length);
                if (!me.id) break;
                // The entry's own game id (an edition entry differs from the base game); `entry-N` uids aren't ids.
                let id = /^\d+$/.test(c.uid || '') ? Number(c.uid) : c.gameId;
                let found = await __ibGet('/api/user_collections?user_id=' + me.id + '&game_id=' + id);
                // A failed lookup (429/5xx/expired token) is not "no entry": creating one then would duplicate it.
                if (found === null) continue;
                let entry = __ibList(found)[0];
                let ok;
                if (entry) {
                    let [status, completion] = target(c.to, entry.completion);
                    let body = Object.assign({}, entry, { status: status, completion: completion });
                    ok = await __ibSend('PUT', '/api/user_collections/' + entry.id, body);
                } else if (c.platformIds.length) {
                    let [status, completion] = target(c.to, '');
                    ok = await __ibSend('POST', '/api/user_collections', {
                        game_id: id, edition_id: null, status: status, completion: completion,
                        digital_store: null, ownership: 'owned', physical: true, playtime_type: 'manual',
                        platform_id: c.platformIds[0],
                    });
                }
                if (ok) out.push({ uid: String(id), name: c.name, gameId: c.gameId });
            }
        """.trimIndent()))
    }

    // The site's own "add to wishlist" call: the IGDB id is the game id.
    override val wishlistWrite = WishlistWrite.ById { ids ->
        WebStep(HOME, SyncScripts.wrap("""
            $API_JS
            const ids = $ids;
            out = [];
            for (const [n, id] of ids.entries()) {
                if (__glCancelled()) break;
                __glProgress(n, ids.length);
                if (await __ibSend('POST', '/api/user_wishlists', { game_id: id })) out.push({ uid: String(id), name: '' });
            }
        """.trimIndent()))
    }

    override val wishlist = webRefs(WebStep(HOME, SyncScripts.wrap("""
        $API_JS
        let me = await __ibMe();
        if (!me.id) throw new Error('Infinite Backlog session is missing');
        out = (await __ibPages('/api/user_wishlists?user_id=' + me.id)).map(function(it) {
            return { uid: __ibUid(it), name: __ibName(it) };
        });
    """.trimIndent())))

    private companion object {
        const val HOME = "https://infinitebacklog.net/"
        const val AUTH = "https://infinitebacklog.net/auth/realms/ib/protocol/openid-connect/auth"
        const val TOKEN = "https://infinitebacklog.net/auth/realms/ib/protocol/openid-connect/token"
        const val CLIENT = "web-application"
        const val SILENT_SSO = "https://infinitebacklog.net/silent-check-sso.html"
        const val SILENT_SSO_ENCODED = "https%3A%2F%2Finfinitebacklog.net%2Fsilent-check-sso.html"

        // __ibToken: Keycloak's silent check-sso. The hidden iframe lands on /silent-check-sso.html, which posts
        // its own URL (with `code=` or `error=login_required` in the fragment) to the parent; the code is then
        // exchanged with the PKCE verifier for an access token, cached for the rest of the script.
        val API_JS = """
            let __ibBearer = null;
            async function __ibToken() {
                if (__ibBearer !== null) return __ibBearer;
                let b64 = function(buf) {
                    return btoa(String.fromCharCode.apply(null, new Uint8Array(buf)))
                        .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
                };
                let verifier = b64(crypto.getRandomValues(new Uint8Array(48)));
                let challenge = b64(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier)));
                let frame = document.createElement('iframe');
                frame.style.display = 'none';
                let landed = new Promise(function(resolve) {
                    let onMessage = function(e) {
                        if (e.origin !== location.origin || e.source !== frame.contentWindow) return;
                        window.removeEventListener('message', onMessage);
                        resolve(String(e.data));
                    };
                    window.addEventListener('message', onMessage);
                    setTimeout(function() { resolve(''); }, 15000);
                });
                frame.src = '$AUTH?client_id=$CLIENT&redirect_uri=$SILENT_SSO_ENCODED&state=gamerlogue' +
                    '&response_mode=fragment&response_type=code&scope=openid&prompt=none' +
                    '&code_challenge=' + challenge + '&code_challenge_method=S256';
                document.body.appendChild(frame);
                let href = await landed;
                frame.remove();
                let code = new URLSearchParams(href.split('#')[1] || '').get('code');
                __ibBearer = '';
                if (code) {
                    let r = await fetch('$TOKEN', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        body: new URLSearchParams({ grant_type: 'authorization_code', client_id: '$CLIENT',
                            code: code, redirect_uri: '$SILENT_SSO', code_verifier: verifier }),
                    });
                    __ibBearer = r.ok ? ((await r.json()).access_token || '') : '';
                }
                console.log('[GL] infinite backlog token=' + (__ibBearer ? 'yes' : 'no'));
                return __ibBearer;
            }
            async function __ibGet(url) {
                let token = await __ibToken();
                let r = await fetch(url, { credentials: 'include', headers: token ? { Authorization: 'Bearer ' + token } : {} });
                return r.ok ? r.json() : null;
            }
            async function __ibSend(method, url, body) {
                let token = await __ibToken();
                let r = await fetch(url, {
                    method: method,
                    credentials: 'include',
                    headers: Object.assign({ 'Content-Type': 'application/json' },
                        token ? { Authorization: 'Bearer ' + token } : {}),
                    body: JSON.stringify(body),
                });
                return r.ok;
            }
            async function __ibMe() {
                let j = await __ibGet('/api/users/me/complete');
                return (j && (j.user || j.data || j)) || {};
            }
            function __ibList(j) {
                return Array.isArray(j) ? j : (j && (j.data || j.items || j.results)) || [];
            }
            async function __ibPages(base) {
                let all = [], seen = {};
                for (let offset = 0; offset < 20000; offset += 50) {
                    let json = await __ibGet(base + '&limit=50&offset=' + offset);
                    // A failed page must fail the read: a partial library would be treated as the whole one.
                    if (json === null) throw new Error('Infinite Backlog request failed at offset ' + offset);
                    let page = __ibList(json);
                    // Stop when a page brings nothing new, in case the server repeats the last page.
                    let fresh = page.filter(function(it) {
                        let key = it.id !== undefined ? 'id:' + it.id : JSON.stringify(it);
                        if (seen[key]) return false;
                        seen[key] = true;
                        return true;
                    });
                    all = all.concat(fresh);
                    __glProgress(all.length, 0);
                    if (page.length < 50 || !fresh.length) break;
                }
                return all;
            }
            // The game's (= IGDB) id. An entry without one keeps a non-numeric uid, so it falls back to a name
            // match instead of having its own row id taken for an IGDB id.
            function __ibUid(it) {
                let id = (it.game && it.game.id) || it.game_id;
                return id ? String(id) : 'entry-' + it.id;
            }
            function __ibName(it) { return (it.game && (it.game.name || it.game.title)) || it.name || it.title || ''; }
        """.trimIndent()
    }
}
