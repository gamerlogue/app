package it.maicol07.gamerlogue.services.connectors

import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.StatusChange
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.jsonArray
import it.maicol07.gamerlogue.services.webProfile
import it.maicol07.gamerlogue.services.webRefs
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus

/**
 * HowLongToBeat. Same-origin JSON endpoints used by the Next.js site (as in the Playnite HLTB plugin):
 * `/api/user` gives the signed-in user, `/api/user/{id}/games/list` the whole tracked list with one flag
 * per HLTB list. HLTB has no wishlist (its Backlog list is already part of the library read).
 *
 * Writes go through the site's own edit form data: `/api/submit` saves a whole entry (times, notes,
 * platform, review), so a status change loads the stored entry from `/submit/edit/{submissionId}`
 * (server-rendered `editData` in `__NEXT_DATA__`) and sends it back with only the lists changed, applying
 * the same tweaks the form does. Games not on HLTB yet aren't added ([canAddGames]): HLTB ids aren't IGDB
 * ids, so finding the game would mean trusting the first name-search hit.
 *
 * Matching uses each game's Steam app id (`profile_steam`) through IGDB's Steam mapping, falling back to the
 * **name** for games not on Steam.
 *
 * ponytail: submit payload reconstructed from the site's bundle (form `defaultValues: editData`), not a
 * live session — verify live.
 */
class HowLongToBeatConnector :
    ServiceConnector(ExternalService.HOWLONGTOBEAT, host = "howlongtobeat.com", externalGameSource = 0) {
    override val loginUrl = "https://howlongtobeat.com/login"

    // A Next.js app: signing in only changes the route client-side, which the WebView doesn't report.
    override val loginTriggerScript = SyncScripts.leaveLoginWhenSignedIn(
        "fetch('/api/user', { credentials: 'include' }).then(r => r.ok ? r.json() : {})" +
            ".then(j => !!(j.data && j.data[0] && j.data[0].user_id))",
        target = HOME,
    )

    override val profile = webProfile(
        WebStep(
            HOME, SyncScripts.wrap(
                """
        $API_JS
        let me = await __hlUser();
        let path = me.user_name ? '/user/' + encodeURIComponent(me.user_name) : '';
        let page = path ? await __hlPageProps(path) : null;
        let avatar = page && page.userData && page.userData.user_avatar;
        out = {
            username: me.user_name || '',
            avatarUrl: avatar ? 'https://howlongtobeat.com/avatars/' + encodeURIComponent(avatar) : '',
            profileUrl: path ? new URL(path, '$HOME').href : '',
        };
    """.trimIndent()
            )
        )
    )

    override val ownedGames = webRefs(WebStep(HOME, listScrape()))

    // Replay and the custom lists have no status of ours, so a write leaves them as they are.
    override val syncStatuses = setOf(
        GameLibraryStatus.PLAYING,
        GameLibraryStatus.COMPLETED,
        GameLibraryStatus.ABANDONED,
        GameLibraryStatus.BACKLOG,
    )

    override val canAddGames = false

    override val statusWrite: (List<StatusChange>) -> WebStep = { changes ->
        WebStep(HOME, SyncScripts.wrap("""
            $API_JS
            let me = await __hlUser();
            let entries = {};
            (await __hlList(me)).forEach(function(g) {
                if (!entries[String(g.game_id)]) entries[String(g.game_id)] = g;
            });
            let flag = { PLAYING: 'playing', BACKLOG: 'backlog', COMPLETED: 'completed', ABANDONED: 'retired' };
            const changes = ${changes.jsonArray()};
            out = [];
            for (const [n, c] of changes.entries()) {
                if (__glCancelled()) break;
                __glProgress(n, changes.length);
                let entry = me.user_id && c.uid ? entries[c.uid] : null;
                let page = entry ? await __hlPageProps('/submit/edit/' + entry.id) : null;
                let edit = page && page.editData, game = page && page.gameData;
                if (!edit || !game || !flag[c.to]) continue;
                // What the site's edit form sends: the stored entry, with only the status lists changed.
                let body = JSON.parse(JSON.stringify(edit));
                delete body.highText;
                body.lists = Object.assign({}, body.lists);
                ['playing', 'backlog', 'completed', 'retired'].forEach(function(l) { body.lists[l] = l === flag[c.to]; });
                if (isNaN(body.userId)) body.userId = me.user_id;
                body.adminId = me.user_rights >= 10 ? me.user_id : null;
                body.gameId = game.game_id;
                body.customLabels = { custom: me.set_customtab, custom2: me.set_customtab2, custom3: me.set_customtab3 };
                body.general = Object.assign({}, body.general, { progressBefore: (edit.general || {}).progress });
                let r = await fetch('/api/submit', {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body),
                });
                let j = r.ok ? await r.json() : { error: true };
                if (!j.error) out.push({ uid: c.uid, name: c.name, gameId: c.gameId });
            }
        """.trimIndent()))
    }

    // One flag per HLTB list; a game can sit on several, so the most "advanced" one wins.
    private fun listScrape() = SyncScripts.wrap("""
        $API_JS
        let me = await __hlUser();
        if (!me.user_id) throw new Error('HowLongToBeat session is missing');
        // One ref per game, from its first entry: the same playthrough statusWrite updates, so the two agree.
        let first = {};
        let games = (await __hlList(me)).filter(function(g) {
            let key = String(g.game_id);
            if (first[key]) return false;
            first[key] = true;
            return true;
        });
        let on = function(g, f) { return Number(g[f]) > 0; };
        let status = function(g) {
            if (on(g, 'list_playing')) return 'PLAYING';
            if (on(g, 'list_comp') || on(g, 'list_replay')) return 'COMPLETED';
            if (on(g, 'list_retired')) return 'ABANDONED';
            return 'BACKLOG';
        };
        out = games.map(function(g) {
            let ref = { uid: String(g.game_id), name: g.custom_title || g.game_name || '', status: status(g) };
            if (Number(g.profile_steam) > 0) ref.steamAppId = String(g.profile_steam);
            return ref;
        });
        // A game's Steam app id (`profile_steam`, its "Steam" link) makes the match exact instead of by name. The
        // list may not carry it, so it's read from each game's submit-page props; a few run in parallel.
        let done = 0;
        await __glPool(out.filter(function(r) { return !r.steamAppId; }), 6, async function(ref) {
            let page = await __hlPageProps('/submit/game/' + ref.uid);
            let steam = page && page.gameData && Number(page.gameData.profile_steam);
            if (steam > 0) ref.steamAppId = String(steam);
            __glProgress(++done, out.length);
        });
    """.trimIndent())

    private companion object {
        const val HOME = "https://howlongtobeat.com/"

        // `/api/user` answers `{data: [{user_id, user_name, …}]}` for the cookie session (the site's own
        // `currentUser`, incl. `user_rights` and the `set_customtab*` labels), `{}` when signed out. A
        // list entry's `id` is its submission id. Page props are the server-rendered `__NEXT_DATA__`.
        val API_JS = """
            async function __hlUser() {
                let r = await fetch('/api/user', { credentials: 'include' });
                let j = r.ok ? await r.json() : {};
                return (j.data && j.data[0]) || {};
            }
            async function __hlList(me) {
                if (!me.user_id) return [];
                let r = await fetch('/api/user/' + me.user_id + '/games/list', {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ user_id: me.user_id,
                        lists: ['playing', 'backlog', 'replays', 'custom', 'custom2', 'custom3', 'completed', 'retired'],
                        set_playstyle: 'comp_all', name: '', platform: '', storefront: '', sortBy: '', sortFlip: 0,
                        view: '', random: 0, limit: 5000, currentUserHome: true }),
                });
                if (!r.ok) throw new Error('HowLongToBeat game list HTTP ' + r.status);
                return ((await r.json()).data || {}).gamesList || [];
            }
            // Next.js serves a page's server props as JSON at /_next/data/{buildId}{path}.json — far lighter than
            // the HTML; the HTML's __NEXT_DATA__ is the fallback when that route isn't available.
            async function __hlPageProps(path) {
                try {
                    let build = window.__NEXT_DATA__ && window.__NEXT_DATA__.buildId;
                    if (build) {
                        let j = await fetch('/_next/data/' + build + path + '.json', { credentials: 'include' });
                        if (j.ok) return (await j.json()).pageProps || null;
                    }
                    let r = await fetch(path, { credentials: 'include' });
                    if (!r.ok) return null;
                    let doc = new DOMParser().parseFromString(await r.text(), 'text/html');
                    return JSON.parse(doc.getElementById('__NEXT_DATA__').textContent).props.pageProps;
                } catch (e) { return null; }
            }
        """.trimIndent()
    }
}
