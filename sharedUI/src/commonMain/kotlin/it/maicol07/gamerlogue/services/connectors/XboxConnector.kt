package it.maicol07.gamerlogue.services.connectors

import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep

/**
 * Xbox / Microsoft Store.
 *
 * ponytail: best-effort scaffold — owned titles come from the XSTS-authenticated titlehub, not
 * reachable from a plain web session; the web library at xbox.com/play hydrates a subset into the
 * page. Reads scrape that data layer and degrade to empty; wishlist write defaults to no-op.
 */
class XboxConnector : ServiceConnector(ExternalService.XBOX, host = "xbox.com", externalGameSource = 11) {
    override val platformFamily = 2 // IGDB platform_family: Xbox

    override fun loginUrl() = "https://www.xbox.com/play"

    override fun readOwned() = WebStep("https://www.xbox.com/play", SyncScripts.wrap("""
        let items = (window.__PRELOADED_STATE__?.library?.titles)
                 || (window.__INITIAL_STATE__?.library?.titles) || [];
        out = items.map(t => ({ uid: String(t.titleId || t.productId || ''), name: t.name || t.title || '' }))
                   .filter(x => x.uid);
    """.trimIndent()))

    override fun readWishlist() = WebStep("https://www.microsoft.com/store/wishlist", SyncScripts.wrap("""
        let items = (window.__INITIAL_STATE__?.wishlist?.items) || [];
        out = items.map(t => ({ uid: String(t.productId || ''), name: t.title || '' })).filter(x => x.uid);
    """.trimIndent()))
}
