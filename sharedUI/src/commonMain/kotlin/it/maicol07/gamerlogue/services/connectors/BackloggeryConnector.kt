package it.maicol07.gamerlogue.services.connectors

import it.maicol07.gamerlogue.services.DataSource
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.StatusChange
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.services.jsonArray
import it.maicol07.gamerlogue.services.parseWriteResult
import it.maicol07.gamerlogue.services.webProfile
import it.maicol07.gamerlogue.services.webRefs
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.serialization.json.Json

/**
 * Backloggery (the 2023 rewrite). A Vue SPA over same-origin PHP endpoints with a plain cookie session
 * (no CSRF): `fetch_library.php` returns the whole library in one call. The wishlist is not a list of its
 * own but the ownership value `own = 7`, so both reads share one fetch and split on it.
 *
 * `fetch_library.php` answers with the library array itself (the site stores `response.data` as is), not a
 * `payload` wrapper like the other endpoints.
 *
 * Games are free-text titles. Our notes marker carries the IGDB id for exact matching; unmarked entries
 * still match by name. Writes preserve user notes and replace only the marker.
 *
 * Existing games are updated through `fetch_gameinfo.php` + `update_game.php`, preserving the entry.
 * New library games and wishlist entries use `add_game.php`: IGDB platform names/abbreviations are matched
 * against the site's catalog, preferring the selected platform or a compatible user default. Ambiguous
 * platforms are skipped. Wishlist entries use `own = 7`; additions use stealth saves.
 *
 * ponytail: endpoints/enums read off the live JS bundle, not a logged-in session — verify live.
 */
class BackloggeryConnector :
    ServiceConnector(ExternalService.BACKLOGGERY, host = "backloggery.com", externalGameSource = 0) {
    override val loginUrl = "https://backloggery.com/!/login/"

    // A Vue app: signing in only changes the route client-side, which the WebView doesn't report.
    override val loginTriggerScript = SyncScripts.leaveLoginWhenSignedIn(
        "fetch('/api/auth_login.php', { method: 'POST', credentials: 'include' }).then(r => r.ok ? r.json() : {})" +
            ".then(j => !!(j.payload && j.payload.username))",
        target = HOME,
    )

    override val profile = webProfile(
        WebStep(
            HOME,
            SyncScripts.wrap(
                """
                $API_JS
                let user = await __bgUser();
                let p = user ? ((await __bgPost('fetch_profile.php', { type: 'load_user_profile', username: user })) || {}).payload || {} : {};
                out = {
                    username: user,
                    // A profile without an avatar has `avatar: ""`; the site shows its default image then, so do we.
                    avatarUrl: 'https://backloggery.com/img/' + (p.avatar ? 'users/avatars/' + p.avatar : 'default_avatar.png'),
                    profileUrl: user ? 'https://backloggery.com/' + user : '',
                };
                """.trimIndent()
            )
        )
    )

    override val ownedGames = webRefs(WebStep(HOME, libraryScrape(wishlist = false)))

    override val wishlist = webRefs(WebStep(HOME, libraryScrape(wishlist = true)))

    /** All existing entries, including wishlist entries, for metadata-only maintenance. */
    val gamesForIdSync = DataSource.Web(
        WebStep(
            HOME, SyncScripts.wrap(
                """
            $API_JS
            out = Object.values(await __bgIdRows()).map(function(g) {
                return { uid: String(g.game_inst_id), name: g.title || '', gameId: __bgIgdbId(g.notes) };
            });
        """.trimIndent()
            )
        ),
        ::parseWriteResult,
    )

    fun resyncIds(games: List<ExternalGameRef>) = WebStep(
        HOME, SyncScripts.wrap(
            """
        $API_JS
        const games = ${Json.encodeToString(games)};
        let rows = await __bgIdRows();
        for (const [n, ref] of games.entries()) {
            __bgProgress(n, games.length);
            let row = rows[ref.uid];
            if (!row) throw new Error('Backloggery entry no longer exists: ' + ref.uid);
            if (__bgNotes(row.notes, ref.gameId) === String(row.notes || '')) continue;
            let info = await __bgPost('fetch_gameinfo.php', { game_inst_id: Number(ref.uid) });
            let game = info && info.status && info.payload;
            if (!game || Array.isArray(game)) throw new Error('Backloggery entry could not be read: ' + ref.uid);
            let body = Object.assign(__bgEdit(row, game), {
                notes: __bgNotes(game.notes ?? row.notes, ref.gameId),
                is_stealth: true,
            });
            let saved = await __bgPost('update_game.php', body);
            if (!saved || !saved.status) throw new Error('Backloggery rejected the ID update: ' + ref.uid);
            row.notes = body.notes;
            out.push(ref);
        }
    """.trimIndent()
        ), reload = false
    )

    override val wishlistWrite = WishlistWrite.ByGame { games ->
        wishlistAdd(
            games.map { game ->
                StatusChange(
                    game.gameId,
                    game.name,
                    null,
                    null,
                    GameLibraryStatus.BACKLOG,
                    platformIds = game.platformIds,
                    platformNames = game.platformNames,
                )
            }
        )
    }

    private fun wishlistAdd(changes: List<StatusChange>) = WebStep(
        HOME,
        SyncScripts.wrap(
            """
            $API_JS
            $WRITE_JS
            let context = await __bgWriteContext();
            const changes = ${changes.jsonArray()};
            out = [];
            for (const [n, c] of changes.entries()) {
                __bgProgress(n, changes.length);
                let uid = await __bgAdd(c, 7, context);
                if (uid) out.push({ uid: uid, name: c.name, gameId: c.gameId });
            }
            """.trimIndent()
        ),
        reload = false,
    )

    // `status`: Unplayed=10, Unfinished=20, Beaten=30, Completed=40, Endless=60, None=80.
    // `priority`: Now Playing=80, Ongoing=70, Paused=60, High…Low=50…30, Replay=20, Shelved=10.
    // The priority carries the "what am I doing with it" signal (playing/paused/dropped), so it wins.
    private fun libraryScrape(wishlist: Boolean) = SyncScripts.wrap(
        """
        $API_JS
        let user = await __bgUser();
        // The site posts the store action itself ({type, username}) and keeps the response as the library.
        let data = user ? await __bgPost('fetch_library.php', { type: 'load_user_library', username: user }) : null;
        let games = Array.isArray(data) ? data : (data && Array.isArray(data.payload) ? data.payload : []);
        console.log('[GL] backloggery user=' + (user ? 'yes' : 'no') + ' response=' + (Array.isArray(data) ? 'array' : typeof data) +
            ' games=' + games.length + ' wishlist=' + games.filter(function(g) { return Number(g.own) === 7; }).length);
        out = games
            .filter(function(g) { return (Number(g.own) === 7) === $wishlist; })
            .map(function(g) {
                let ref = { uid: String(g.game_inst_id), name: g.title || '' };
                let gameId = __bgIgdbId(g.notes);
                if (gameId !== null) ref.gameId = gameId;
                if (!$wishlist) ref.status = __bgStatus(g);
                return ref;
            });
        """.trimIndent()
    )

    override val syncStatuses = GameLibraryStatus.entries.toSet()

    // The site's editor: fetch the full entry, change status/priority, send it back with the previous
    // status/ownership. The review comes HTML-escaped from fetch_gameinfo and the editor unescapes it before
    // saving — skipping that would double-escape it on every write.
    override val statusWrite: (List<StatusChange>) -> WebStep = { changes ->
        WebStep(
            HOME,
            SyncScripts.wrap(
                """
                $API_JS
                $WRITE_JS
                const changes = ${changes.jsonArray()};
                // The editor starts from the library row and swaps in fetch_gameinfo's entry, keeping the row's
                // platform_title (the entry lacks it, and the server needs it once the save adds a history milestone).
                let context = await __bgWriteContext();
                let library = context.library;
                let rows = {};
                (Array.isArray(library) ? library : []).forEach(function(g) { rows[String(g.game_inst_id)] = g; });
                out = [];
                for (const [n, c] of changes.entries()) {
                    __bgProgress(n, changes.length);
                    if (!c.uid) {
                        let uid = await __bgAdd(c, c.owned ? 1 : 0, context);
                        if (uid) out.push({ uid: uid, name: c.name, gameId: c.gameId });
                        continue;

                    }
                    let info = await __bgPost('fetch_gameinfo.php', { game_inst_id: Number(c.uid) });
                    let game = info && info.status && info.payload;
                    if (!game || Array.isArray(game)) {
                        console.log('[GL] backloggery gameinfo ' + c.uid + ' unusable: ' + JSON.stringify(info).slice(0, 300));
                        continue;
                    }
                    let [status, priority] = __bgTarget(c.to, game);
                    let row = rows[c.uid] || {};
                    let body = Object.assign(__bgEdit(row, game), {
                        status: status,
                        priority: priority,
                        is_stealth: false,
                        notes: __bgNotes(game.notes ?? row.notes, c.gameId),
                    });
                    let saved = await __bgPost('update_game.php', body);
                    console.log('[GL] backloggery update ' + c.uid + ': ' + JSON.stringify(saved).slice(0, 300));
                    if (saved && saved.status) out.push({ uid: c.uid, name: c.name, gameId: c.gameId });
                }
                """.trimIndent()
            ),
            reload = false,
        )
    }

    private companion object {
        const val HOME = "https://backloggery.com/"

        val WRITE_JS = """
            async function __bgWriteContext() {
                // Page reuse keeps this snapshot and confirmed additions across batches of this WebView flow.
                if (!window.__glBackloggeryContext) {
                    let user = await __bgUser();
                    let library = user ? await __bgPost('fetch_library.php', { type: 'load_user_library', username: user }) : [];
                    window.__glBackloggeryContext = await __bgCreateContext(library);
                }
                return window.__glBackloggeryContext;
            }
            function __bgTarget(to, game) {
                let st = Number(game.status), pr = Number(game.priority);
                let calm = pr === 80 || pr === 70 || pr === 60 || pr === 10 ? 40 : pr;
                if (to === 'PLAYING') return [st === 10 ? 20 : st, 80];
                if (to === 'PAUSED') return [st === 20 || st === 60 ? st : 20, 60];
                if (to === 'ABANDONED') return [st === 10 ? 20 : st, 10];
                if (to === 'COMPLETED') return [st === 40 ? 40 : 30, calm];
                return [st === 10 || st === 20 ? st : 10, calm];
            }
            function __bgPlatformKey(value) {
                let key = String(value || '').toLowerCase().replace(/^(sony|microsoft|nintendo|sega)\s+/, '')
                    .replace(/[^a-z0-9]/g, '');
                return key === 'pcmicrosoftwindows' ? 'pc' : key;
            }
            async function __bgCreateContext(library) {
                let data = [
                    await __bgPost('fetch_platforms.php', null),
                    await __bgPost('fetch_user_platforms.php', { get_owner_platforms: true }),
                    await __bgPost('fetch_user_defaults.php', null),
                ];
                return {
                    catalog: data[0] && Array.isArray(data[0].payload) ? data[0].payload : [],
                    platforms: data[1] && Array.isArray(data[1].payload) ? data[1].payload : [],
                    defaults: data[2] && Array.isArray(data[2].payload) ? data[2].payload[0] || {} : {},
                    library: Array.isArray(library) ? library : [],
                };
            }
            function __bgPlatform(change, context) {
                let match = function(names) {
                    let keys = new Set(names.map(__bgPlatformKey).filter(Boolean));
                    return context.catalog.filter(function(p) {
                        return keys.has(__bgPlatformKey(p.title)) || keys.has(__bgPlatformKey(p.abbr));
                    });
                };
                let preferred = change.platformIds.map(function(id) {

                    let platforms = match(change.platformNames[id] || []);
                    return platforms.length === 1 ? platforms[0] : null;
                }).filter(Boolean);
                let available = change.platformIds.length ? preferred : match(Object.values(change.platformNames).flat());
                let configured = available.find(function(p) { return Number(p.platform_id) === Number(context.defaults.platform_id); });
                return configured || preferred[0] || (available.length === 1 ? available[0] : null);
            }
            async function __bgAdd(change, own, context) {
                let platform = __bgPlatform(change, context);
                if (!platform) {
                    console.log('[GL] backloggery no unambiguous platform ' + JSON.stringify({
                        name: change.name, platformIds: change.platformIds, platformNames: change.platformNames,
                    }));
                    return null;
                }
                let titleKey = function(title) { return String(title || '').normalize('NFKC').trim().toLowerCase(); };
                if (context.library.some(function(g) {
                    return titleKey(g.title) === titleKey(change.name) && Number(g.platform_id) === Number(platform.platform_id);
                })) return null;
                let registered = context.platforms.find(function(p) { return Number(p.platform_id) === Number(platform.platform_id); });
                if (!registered) {
                    let added = await __bgPost('add_user_platform.php', { platform_id: platform.platform_id, title: platform.title });
                    if (!added || !added.status) return null;
                    registered = platform;
                    context.platforms.push(platform);
                }
                let target = __bgTarget(change.to, { status: 10, priority: 40 });
                let game = {
                    title: change.name, platform_id: Number(platform.platform_id),
                    platform_title: registered.title, abbr: registered.abbr,
                    status: target[0], priority: target[1], own: own,
                    region: Number(context.defaults.region) || 0, phys_digi: Number(context.defaults.phys_digi) || 0,
                    notes: __bgNotes('', change.gameId), parent_inst_id: 0, update_parent: false, is_stealth: true,
                };
                let saved = await __bgPost('add_game.php', game);
                let uid = saved && saved.status && saved.payload;
                if (!/^\d+$/.test(String(uid || ''))) return null;
                context.library.push(Object.assign({}, game, { game_inst_id: uid }));
                return String(uid);
            }
        """.trimIndent()

        // auth_login.php with no body restores the cookie session and returns the signed-in user.
        val API_JS = """
            // Both editor operations must send the same required bookkeeping fields.
            function __bgEdit(row, game) {
                let review = String(game.review || '').replaceAll('&#039;', "'").replaceAll('&amp;', '&');
                return Object.assign({}, row, game, {
                    platform_title: row.platform_title || game.platform_title,
                    prev_status: Number(game.status), prev_own: Number(game.own),
                    review: review, has_review: review.length > 0, review_viewed: false,
                    update_parent: false,
                });
            }
            const __bgIgdbMarker = /^\[Gamerlogue IGDB: ([1-9]\d*)\]\r?$/gm;
            function __bgIgdbId(notes) {
                let ids = Array.from(String(notes || '').matchAll(__bgIgdbMarker), function(m) { return Number(m[1]); });
                return ids.length && ids.every(function(id) { return Number.isInteger(id) && id <= 2147483647 && id === ids[0]; })
                    ? ids[0] : null;
            }
            function __bgNotes(notes, gameId) {
                if (!Number.isInteger(gameId) || gameId <= 0 || gameId > 2147483647) throw new Error('Invalid IGDB game id');
                let marker = '[Gamerlogue IGDB: ' + gameId + ']', found = false;
                let text = String(notes || '').replace(__bgIgdbMarker, function(old) {
                    if (found) return '';
                    found = true;
                    return marker + (old.endsWith('\r') ? '\r' : '');
                });
                if (!found) text += (text && !text.endsWith('\n') ? (text.includes('\r\n') ? '\r\n' : '\n') : '') + marker;
                // The site's Notes editor is limited to 700 characters; never truncate the user's text.
                if (text.length > 700) throw new Error('Backloggery notes exceed 700 characters with the IGDB marker');
                return text;
            }
            let __bgDone = 0, __bgTotal = 0;
            function __bgProgress(done, total) {
                __bgDone = done; __bgTotal = total;
                __glProgress(done, total);
            }
            async function __bgWait(ms) {
                const until = Date.now() + ms;
                while (Date.now() < until) {
                    __glProgress(__bgDone, __bgTotal);
                    await __glSleep(Math.min(1000, until - Date.now()));
                }
            }
            async function __bgPost(endpoint, body) {
                for (let attempt = 0; ; attempt++) {
                    // Pace all requests, including the editor reads; transport failures are never replayed.
                    await __bgWait(1000);
                    let r = await fetch('/api/' + endpoint, {
                        method: 'POST',
                        credentials: 'include',
                        headers: { 'Content-Type': 'application/json' },
                        body: body ? JSON.stringify(body) : undefined,
                    });
                    if (r.status === 429 && attempt < 3) {
                        let header = r.headers.get('Retry-After');
                        let seconds = header && /^\d+$/.test(header.trim()) ? Number(header) : NaN;
                        let retryAt = header ? Date.parse(header) : NaN;
                        let wait = Number.isFinite(seconds) ? seconds * 1000 :
                            Number.isFinite(retryAt) ? Math.max(0, retryAt - Date.now()) : 15000 * Math.pow(2, attempt);
                        // A longer server cooldown must be surfaced, never shortened into an early retry.
                        if (wait > 120000) throw new Error('Backloggery ' + endpoint + ' HTTP 429; Retry-After: ' + header);
                        await __bgWait(wait);
                        continue;
                    }
                    if (!r.ok) throw new Error('Backloggery ' + endpoint + ' HTTP ' + r.status +
                        (body && body.game_inst_id ? ' (game_inst_id=' + body.game_inst_id + ')' : ''));
                    return r.json();
                }
            }
            // Library status: the priority carries "what am I doing with it" (playing/paused/dropped), so it wins.
            function __bgStatus(g) {
                let pr = Number(g.priority), st = Number(g.status);
                if (pr === 80 || pr === 70) return 'PLAYING';
                if (pr === 10) return 'ABANDONED';
                if (st === 30 || st === 40) return 'COMPLETED';
                if (pr === 60) return 'PAUSED';
                if (st === 60) return 'PLAYING';
                return 'BACKLOG';
            }
            async function __bgUser() {
                let me = await __bgPost('auth_login.php', null);
                return (me && me.payload && me.payload.username) || '';
            }
            async function __bgLibrary() {
                let user = await __bgUser();
                if (!user) throw new Error('Backloggery session is missing');
                let data = await __bgPost('fetch_library.php', { type: 'load_user_library', username: user });
                if (Array.isArray(data)) return data;
                if (data && Array.isArray(data.payload)) return data.payload;
                throw new Error('Backloggery library response is invalid');
            }
            async function __bgIdRows() {
                if (!window.__glBackloggeryIdRows) {
                    let rows = {};
                    (await __bgLibrary()).forEach(function(g) { rows[String(g.game_inst_id)] = g; });
                    window.__glBackloggeryIdRows = rows;
                }
                return window.__glBackloggeryIdRows;
            }
        """.trimIndent()
    }

}

