package it.maicol07.gamerlogue.services.connectors

import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.uidJsonArray

/**
 * Steam — everything runs on the **store** domain (single login, no cross-domain SSO).
 *
 * Owned games: the modern games page is a React SPA (no `rgGames`) and the legacy `?xml=1` feed is
 * gone, so we use the official WebAPI `IPlayerService/GetOwnedGames` (appid + name in one call). The
 * access token comes from the store's own `pointssummary/ajaxgetasyncconfig`; the steamid is decoded
 * from that token's JWT `sub` claim. Wishlist read reuses the same token (GetWishlist + GetItems);
 * wishlist writes use the store endpoint directly.
 */
class SteamConnector : ServiceConnector(
    service = ExternalService.STEAM,
    host = "store.steampowered.com",
    externalGameSource = 1,
    storeUrlTemplate = "https://store.steampowered.com/app/{id}",
) {
    override fun loginUrl() = "https://store.steampowered.com/login/"
    override fun uidFromUrl(url: String) = Regex("/app/(\\d+)").find(url)?.groupValues?.get(1)

    override fun readOwned() = WebStep(HOME, SyncScripts.wrap("""
        console.log('[GL] steam readOwned at ' + location.href);
        function __b64(s) { s = s.replace(/-/g, '+').replace(/_/g, '/'); while (s.length % 4) s += '='; return atob(s); }
        let token = '';
        try {
            let cr = await fetch('/pointssummary/ajaxgetasyncconfig', { credentials: 'include' });
            let cj = await cr.json();
            token = (cj && cj.data && cj.data.webapi_token) || '';
        } catch (e) { console.log('[GL] token err ' + e); }
        console.log('[GL] token=' + (token ? 'ok' : 'empty'));
        let steamid = '';
        try { steamid = JSON.parse(__b64(token.split('.')[1])).sub || ''; } catch (e) {}
        console.log('[GL] steamid=' + steamid);
        if (token && steamid) {
            let u = 'https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?access_token='
                + encodeURIComponent(token) + '&steamid=' + steamid
                + '&include_appinfo=1&include_played_free_games=1&format=json';
            let r = await fetch(u, { credentials: 'omit' });
            console.log('[GL] api status ' + r.status);
            let j = await r.json();
            let games = (j && j.response && j.response.games) || [];
            out = games.map(g => ({ uid: String(g.appid), name: g.name || '' }));
            console.log('[GL] api games=' + out.length);
        }
    """.trimIndent()))

    override fun readWishlist() = WebStep(HOME, SyncScripts.wrap("""
        console.log('[GL] steam readWishlist at ' + location.href);
        function __b64(s) { s = s.replace(/-/g, '+').replace(/_/g, '/'); while (s.length % 4) s += '='; return atob(s); }
        // Wishlist moved off dynamicstore, so read it from the current WebAPI: GetWishlist gives the
        // appids, GetItems resolves names in bulk. Both use the store WebAPI token (as readOwned) and
        // the steamid decoded from that token's JWT `sub` claim.
        let token = '';
        try {
            let cr = await fetch('/pointssummary/ajaxgetasyncconfig', { credentials: 'include' });
            let cj = await cr.json();
            token = (cj && cj.data && cj.data.webapi_token) || '';
        } catch (e) { console.log('[GL] token err ' + e); }
        let steamid = '';
        try { steamid = JSON.parse(__b64(token.split('.')[1])).sub || ''; } catch (e) {}
        console.log('[GL] steamid=' + steamid);
        let ids = [];
        if (token && steamid) {
            let r = await fetch('https://api.steampowered.com/IWishlistService/GetWishlist/v1/?access_token='
                + encodeURIComponent(token) + '&steamid=' + steamid, { credentials: 'omit' });
            console.log('[GL] wishlist status ' + r.status);
            let j = await r.json();
            ids = (((j && j.response && j.response.items) || [])).map(it => String(it.appid));
        }
        console.log('[GL] wishlist ids=' + ids.length);
        let names = {};
        for (let i = 0; token && i < ids.length; i += 100) {
            let chunk = ids.slice(i, i + 100);
            let input = {
                ids: chunk.map(a => ({ appid: Number(a) })),
                context: { language: 'english', country_code: 'US', steam_realm: 1 },
                data_request: { include_basic_info: true },
            };
            try {
                let u = 'https://api.steampowered.com/IStoreBrowseService/GetItems/v1/?access_token='
                    + encodeURIComponent(token) + '&input_json=' + encodeURIComponent(JSON.stringify(input));
                let r = await fetch(u, { credentials: 'omit' });
                let j = await r.json();
                let items = (j && j.response && j.response.store_items) || [];
                for (const it of items) if (it.appid) names[String(it.appid)] = it.name || '';
            } catch (e) { console.log('[GL] names err ' + e); }
        }
        console.log('[GL] wishlist names=' + Object.keys(names).length);
        out = ids.map(id => ({ uid: id, name: names[id] || '' }));
    """.trimIndent()))

    override fun addToWishlist(refs: List<ExternalGameRef>) = WebStep(HOME, SyncScripts.wrap("""
        const ids = ${refs.uidJsonArray()};
        for (const id of ids) {
            const body = new URLSearchParams({ sessionid: window.g_sessionID || '', appid: id });
            await fetch('https://store.steampowered.com/api/addtowishlist',
                { method: 'POST', body, credentials: 'include' });
        }
        out = ids.map(id => ({ uid: id, name: '' }));
    """.trimIndent()))

    private companion object {
        const val HOME = "https://store.steampowered.com/"
    }
}
