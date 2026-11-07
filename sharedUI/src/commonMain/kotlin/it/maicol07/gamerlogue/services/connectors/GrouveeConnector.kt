package it.maicol07.gamerlogue.services.connectors

import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.StatusChange
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.services.jsonArray
import it.maicol07.gamerlogue.services.webProfile
import it.maicol07.gamerlogue.services.webRefs
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Grouvee. Django pages; the shelves page is a Vue app fed by a same-origin JSON API, which the reads use:
 * `/api/shelves/build_all_games/{userId}/` lists the shelves, `/api/shelves/build_shelf_page/{shelfId}/?page=N`
 * a shelf's games; each game page's IGDB link then gives its slug for an exact match. Every page defines the
 * `GROUVEE_USERNAME`, `GROUVEE_USER_ID` and `csrfmiddlewaretoken` globals, which give the user and the CSRF
 * token.
 *
 * The default shelves map to statuses (Playing → PLAYING, Played → COMPLETED, Backlog → BACKLOG); the
 * **Wish List** shelf is the wishlist. Custom shelves are ignored. Grouvee game ids are its own (not IGDB
 * ids), so matching uses the IGDB slug from the game page, falling back to the **name**.
 *
 * ponytail: API shapes read off the site's own collection bundle, not a live session — verify live.
 * `/shelves/toggle/` is a toggle, safe only because the push skips games already on the wishlist.
 */
class GrouveeConnector : ServiceConnector(ExternalService.GROUVEE, host = "www.grouvee.com", externalGameSource = 0) {
    override val loginUrl = "https://www.grouvee.com/user/login/"

    // Django's login view shows the form to a signed-in user too, so on a return visit the URL alone never
    // reads as logged in. The page's own global tells: when it names a user, leave the form for the home page.
    override val loginTriggerScript = """
        if (typeof GROUVEE_USERNAME !== 'undefined' && GROUVEE_USERNAME) location.href = '/';
    """.trimIndent()

    override val profile = webProfile(WebStep(HOME, SyncScripts.wrap("""
        $SHELVES_JS
        let u = __gvUser();
        // The avatar is only on the public profile page (`img.profile-avatar-v2`), not in the page globals.
        let doc = u.base ? await __glDoc(u.base + '/') : null;
        let img = doc && doc.querySelector('img.profile-avatar-v2');
        out = {
            username: u.name,
            avatarUrl: img ? img.getAttribute('src') || '' : '',
            profileUrl: u.name ? 'https://www.grouvee.com' + u.base + '/' : '',
        };
    """.trimIndent())))

    override val ownedGames = webRefs(WebStep(HOME, shelvesScrape(OWNED_SHELVES)))

    override val wishlist = webRefs(WebStep(HOME, shelvesScrape(mapOf(WISH_LIST to null))))

    override val wishlistWrite = WishlistWrite.SearchByName { name ->
        val query = Json.encodeToString(String.serializer(), name)
        WebStep(HOME, SyncScripts.wrap("""
            $SHELVES_JS
            let shelf = (await __gvShelves())['$WISH_LIST'];
            let hit = await __gvFind($query);
            out = [];
            if (!hit) __glSkip($query, 'NOT_FOUND');
            if (shelf && hit) {
                let j = await __gvToggle(hit.id, shelf);
                if (j.success && !j.removed) out = [{ uid: String(hit.id), name: hit.name || '' }];
            }
        """.trimIndent()))
    }

    // Only the three default shelves carry a status, so PAUSED/ABANDONED games aren't synced with Grouvee.
    override val syncStatuses = OWNED_SHELVES.values.map(GameLibraryStatus::valueOf).toSet()

    // A game missing on Grouvee is found by exact (normalized) name; no match = not written. Shelves are toggles:
    // the old one is switched off, and a toggle that reports `removed` (the game was already there) is repeated
    // to switch it back on. `from` may be stale and there is no cheap way to read a game's shelves, so a toggle
    // that reports the old shelf wasn't set (it was just added) is undone by toggling it again.
    override val statusWrite: (List<StatusChange>) -> WebStep = { changes ->
        WebStep(HOME, SyncScripts.wrap("""
            $SHELVES_JS
            let shelves = await __gvShelves();
            let slug = { ${OWNED_SHELVES.entries.joinToString { (slug, status) -> "'$status': '$slug'" }} };
            let toggle = function(gameId, shelfSlug) { return __gvToggle(gameId, shelves[shelfSlug]); };
            out = [];
            const changes = ${changes.jsonArray()};
            for (const [n, c] of changes.entries()) {
                if (__glCancelled()) break;
                __glProgress(n, changes.length);
                let id = c.uid;
                if (!id) {
                    let hit = await __gvFind(c.name);
                    id = hit ? String(hit.id) : null;
                    if (!id) __glSkip(c.name, 'NOT_FOUND');
                }
                if (!id || !shelves[slug[c.to]]) continue;
                if (c.from && slug[c.from] && slug[c.from] !== slug[c.to]) {
                    let off = await toggle(id, slug[c.from]);
                    if (off.success && !off.removed) await toggle(id, slug[c.from]);
                }
                let j = await toggle(id, slug[c.to]);
                if (j.success && j.removed) j = await toggle(id, slug[c.to]);
                if (j.success) out.push({ uid: id, name: c.name, gameId: c.gameId });
            }
        """.trimIndent()))
    }

    // Reads the given shelves (slug → status, null = none) page by page; a game on several of them keeps the
    // status of the first shelf listed. Rows link the game as `/games/{id}-{slug}/`.
    private fun shelvesScrape(statusBySlug: Map<String, String?>) = SyncScripts.wrap("""
        $SHELVES_JS
        let wanted = { ${statusBySlug.entries.joinToString { (slug, status) -> "'$slug': ${status?.let { "'$it'" } ?: "null"}" }} };
        let shelves = await __gvShelves();
        let seen = {}, total = 0;
        out = [];
        for (const slug of Object.keys(wanted)) {
            let id = shelves[slug], pages = 1;
            for (let page = 1; id && page <= pages; page++) {
                let data = await __gvApi('/api/shelves/build_shelf_page/' + id + '/?page=' + page);
                // A failed page must fail the read: a partial library would be treated as the whole one.
                if (!data) throw new Error('Grouvee shelf ' + slug + ' failed at page ' + page);
                if (page === 1) total += (data.filter_data || {}).number_of_filtered_games || 0;
                pages = Math.min((data.page_data || {}).num_pages || 1, 500);
                ((data.serialized_data || {}).ssgs || []).forEach(function(ssg) {
                    let g = ssg.game || {};
                    if (!g.id || seen[g.id]) return;
                    seen[g.id] = true;
                    let ref = { uid: String(g.id), name: g.name || '', url: g.url || '' };
                    if (wanted[slug]) ref.status = wanted[slug];
                    out.push(ref);
                });
                __glProgress(out.length, total);
            }
        }
        // Each game page links its IGDB entry ("See more on IGDB"): its slug makes the match exact instead of by
        // name. One request per game, so a few run in parallel; a page that fails just leaves the name match.
        let resolved = 0;
        await __glPool(out.filter(function(r) { return r.url; }), 6, async function(ref) {
            try {
                let doc = await __glDoc(ref.url);
                let a = doc && doc.querySelector('a[href*="igdb.com/games/"]');
                let m = a && a.getAttribute('href').match(/igdb\.com\/games\/([^\/?#]+)/);
                if (m) ref.igdbSlug = m[1];
            } catch (e) {}
            __glProgress(++resolved, out.length);
        });
    """.trimIndent())

    private companion object {
        const val HOME = "https://www.grouvee.com/"
        const val WISH_LIST = "wish-list"
        val OWNED_SHELVES = mapOf("playing" to "PLAYING", "played" to "COMPLETED", "backlog" to "BACKLOG")

        // The user's shelf ids are per-user numbers, read from the collection API as slug → id.
        val SHELVES_JS = """
            function __gvUser() {
                // Bare names, not window.*: the page may declare them with let/const, which aren't window properties.
                let name = typeof GROUVEE_USERNAME !== 'undefined' ? GROUVEE_USERNAME : '';
                let id = typeof GROUVEE_USER_ID !== 'undefined' ? GROUVEE_USER_ID : '';
                return { name: name, base: name && id && id !== 'None' ? '/user/' + id + '-' + name : '' };
            }
            function __gvCsrf() { return typeof csrfmiddlewaretoken !== 'undefined' ? csrfmiddlewaretoken : ''; }
            // Shelves are toggles: the answer's `removed` says whether the game was on the shelf (now off) or not.
            async function __gvToggle(gameId, shelfId) {
                let body = new URLSearchParams({ game_id: gameId, shelf_id: shelfId, csrfmiddlewaretoken: __gvCsrf() });
                let r = await fetch('/shelves/toggle/', { method: 'POST', credentials: 'include', body: body });
                return r.ok ? r.json() : {};
            }
            // The autocomplete hit whose name equals [name] once normalized (case, accents, punctuation), or null:
            // the first hit alone may be a different game, and a write onto it would be wrong.
            async function __gvFind(name) {
                let key = function(s) {
                    return String(s || '').normalize('NFKD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '');
                };
                let r = await fetch('/search/auto_complete/?q=' + encodeURIComponent(name), { credentials: 'include' });
                let hits = r.ok ? (await r.json()).results || [] : [];
                return hits.find(function(h) { return key(h.name) === key(name); }) || null;
            }
            async function __gvApi(path) {
                let r = await fetch(path, { credentials: 'include', headers: { 'Accept': 'application/json' } });
                return r.ok ? r.json() : null;
            }
            // Slug → id of the user's shelves, from the collection API the (Vue) shelves page itself loads.
            async function __gvShelves() {
                let id = typeof GROUVEE_USER_ID !== 'undefined' ? GROUVEE_USER_ID : '', map = {};
                let data = id && id !== 'None' ? await __gvApi('/api/shelves/build_all_games/' + id + '/') : null;
                // No shelves is a failure, not an empty library: reads would otherwise skip every shelf.
                if (!data) throw new Error('Grouvee shelves could not be read');
                let sd = (data && data.serialized_data) || {};
                (sd.main_shelves || []).concat(sd.other_shelves || []).forEach(function(s) {
                    let m = (s.url || '').match(/\/shelves\/(\d+)-([a-z0-9-]+)\/?$/);
                    let slug = m ? m[2] : (s.name || '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
                    if (slug && !map[slug]) map[slug] = String(m ? m[1] : s.id);
                });
                return map;
            }
        """.trimIndent()
    }
}
