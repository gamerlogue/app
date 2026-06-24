package it.maicol07.gamerlogue.services.connectors

import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep

/**
 * Epic Games Store.
 *
 * ponytail: best-effort scaffold — Epic exposes owned games and wishlist only through its GraphQL
 * API, which usually needs a bearer beyond the session cookie. These scripts try the same-origin
 * GraphQL with `credentials:'include'` and degrade to empty; verify queries against a live session.
 */
class EpicConnector : ServiceConnector(ExternalService.EPIC, host = "epicgames.com", externalGameSource = 26) {
    override fun loginUrl() = "https://www.epicgames.com/id/login"

    override fun readOwned() = WebStep(STORE, SyncScripts.wrap("""
        let r = await fetch('$GRAPHQL', ${graphqlBody("{ Launcher { entitledOfferItems { items { id title } } } }")});
        let j = await r.json();
        let items = j?.data?.Launcher?.entitledOfferItems?.items || [];
        out = items.map(i => ({ uid: String(i.id), name: i.title || '' }));
    """.trimIndent()))

    override fun readWishlist() = WebStep(STORE, SyncScripts.wrap("""
        let r = await fetch('$GRAPHQL', ${graphqlBody("{ Wishlist { wishlistItems { elements { offerId offer { title } } } } }")});
        let j = await r.json();
        let els = j?.data?.Wishlist?.wishlistItems?.elements || [];
        out = els.map(e => ({ uid: String(e.offerId), name: e.offer?.title || '' }));
    """.trimIndent()))

    private companion object {
        const val STORE = "https://store.epicgames.com/"
        const val GRAPHQL = "https://store.epicgames.com/graphql"

        fun graphqlBody(query: String) =
            "{ method:'POST', headers:{'Content-Type':'application/json'}, credentials:'include', " +
                "body: JSON.stringify({ query: `$query` }) }"
    }
}
