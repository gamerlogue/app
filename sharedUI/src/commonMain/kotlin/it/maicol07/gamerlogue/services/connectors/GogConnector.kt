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
 * ponytail: only the first page of owned products is read; add pagination (`totalPages`) if a large
 * library is truncated. Wishlist read/write are best-effort and may need live tuning.
 */
class GogConnector : ServiceConnector(ExternalService.GOG, host = "www.gog.com", externalGameSource = 5) {
    override fun loginUrl() = "https://www.gog.com/account"

    override fun readOwned() = WebStep(ACCOUNT, SyncScripts.wrap("""
        let r = await fetch('https://www.gog.com/account/getFilteredProducts?mediaType=1&page=1',
            { credentials: 'include' });
        let j = await r.json();
        out = (j.products || []).map(p => ({ uid: String(p.id), name: p.title || '' }));
    """.trimIndent()))

    override fun readWishlist() = WebStep(ACCOUNT, SyncScripts.wrap("""
        let r = await fetch('https://www.gog.com/account/wishlist/search?hiddenFlag=0&mediaType=1&page=1',
            { credentials: 'include' });
        let j = await r.json();
        out = (j.products || []).map(p => ({ uid: String(p.id), name: p.title || '' }));
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
