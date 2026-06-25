package it.maicol07.gamerlogue.services.connectors

import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.uidJsonArray

/**
 * GOG. Reads come from same-origin www.gog.com account JSON endpoints used by the site itself.
 *
 * GOG's numeric product id can't be turned into a store URL (GOG pages are slug-based), so we match
 * IGDB on the `uid` field ([idMatchesUid]) — IGDB's GOG `external_games.uid` is that same product id.
 * Wishlist read/write are best-effort and may need live tuning.
 */
class GogConnector : ServiceConnector(ExternalService.GOG, host = "www.gog.com", externalGameSource = 5) {
    override val idMatchesUid = true

    override fun loginUrl() = "https://www.gog.com/account"

    /** Recognise a GOG store page (returns its slug) so IGDB `websites` URLs are matched for push. */
    override fun uidFromUrl(url: String) = Regex("gog\\.com/(?:[a-z]{2}/)?game/([^/?#]+)").find(url)?.groupValues?.get(1)

    override fun readOwned() = WebStep(ACCOUNT, SyncScripts.wrap("""
        out = [];
        let page = 1, totalPages = 1;
        do {
            let r = await fetch('https://www.gog.com/account/getFilteredProducts?mediaType=1&page=' + page,
                { credentials: 'include', headers: { 'X-Requested-With': 'XMLHttpRequest' } });
            let j = await r.json();
            totalPages = j.totalPages || 1;
            out = out.concat((j.products || []).map(p => ({ uid: String(p.id), name: p.title || '' })));
        } while (++page <= totalPages);
    """.trimIndent()))

    override fun readWishlist() = WebStep(ACCOUNT, SyncScripts.wrap("""
        out = [];
        let page = 1, totalPages = 1;
        do {
            let r = await fetch('https://www.gog.com/account/wishlist/search?hiddenFlag=0&mediaType=1&page=' + page,
                { credentials: 'include', headers: { 'X-Requested-With': 'XMLHttpRequest' } });
            let j = await r.json();
            totalPages = j.totalPages || 1;
            out = out.concat((j.products || []).map(p => ({ uid: String(p.id), name: p.title || '' })));
        } while (++page <= totalPages);
    """.trimIndent()))

    override fun addToWishlist(refs: List<ExternalGameRef>) = WebStep(ACCOUNT, SyncScripts.wrap("""
        const ids = ${refs.uidJsonArray()};
        for (const id of ids) {
            await fetch('https://www.gog.com/wishlist/add/' + id, { method: 'GET', credentials: 'include' });
        }
        out = ids.map(id => ({ uid: id, name: '' }));
    """.trimIndent()))

    private companion object {
        const val ACCOUNT = "https://www.gog.com/account"
    }
}
