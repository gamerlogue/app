package it.maicol07.gamerlogue.services.connectors

import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.services.uidJsonArray
import it.maicol07.gamerlogue.services.webProfile
import it.maicol07.gamerlogue.services.webRefs

/**
 * GOG. Reads come from same-origin www.gog.com account JSON endpoints used by the site itself.
 *
 * GOG's numeric product id can't be turned into a store URL (GOG pages are slug-based), so we match
 * IGDB on the `uid` field ([idMatchesUid]) — IGDB's GOG `external_games.uid` is that same product id.
 * Wishlist read/write are best-effort and may need live tuning.
 */
class GogConnector : ServiceConnector(ExternalService.GOG, host = "www.gog.com", externalGameSource = 5) {
    override val idMatchesUid = true

    override val loginUrl = "https://www.gog.com/account"

    // Logged-out, /account redirects to a hash-based login modal (e.g. www.gog.com/en##openlogin) rather
    // than a /login path, so the default marker check misreads it as signed in. Treat it as logged out.
    override suspend fun isLoggedIn(currentUrl: String) =
        super.isLoggedIn(currentUrl) && !currentUrl.contains("openlogin", ignoreCase = true)

    /** Recognise a GOG store page (returns its slug) so IGDB `websites` URLs are matched for push. */
    override fun uidFromUrl(url: String) = Regex("gog\\.com/(?:[a-z]{2}/)?game/([^/?#]+)").find(url)?.groupValues?.get(1)

    // Same-origin userData.json the site uses for the header; avatar is protocol-relative, username maps
    // to the public /u/ page. ponytail: best-effort like the rest of this connector — tune if GOG changes.
    override val profile = webProfile(WebStep(ACCOUNT, SyncScripts.wrap("""
        let r = await fetch('https://www.gog.com/userData.json', { credentials: 'include' });
        let j = await r.json();
        let av = j.avatar ? (j.avatar.indexOf('http') === 0 ? j.avatar : 'https:' + j.avatar) : '';
        // GOG now returns the bare image hash; images.gog.com 404s without a format extension.
        if (av && !/\.(png|jpg|jpeg|webp)$/i.test(av)) av += '.png';
        out = {
            username: j.username || '',
            avatarUrl: av,
            profileUrl: j.username ? 'https://www.gog.com/u/' + j.username : '',
        };
    """.trimIndent())))

    override val ownedGames = webRefs(WebStep(ACCOUNT,
        paginated("https://www.gog.com/account/getFilteredProducts?mediaType=1&page=")))

    override val wishlist = webRefs(WebStep(ACCOUNT,
        paginated("https://www.gog.com/account/wishlist/search?sortBy=date_added&page=")))

    override val wishlistWrite = WishlistWrite.Batch { refs ->
        WebStep(ACCOUNT, SyncScripts.wrap("""
            const ids = ${refs.uidJsonArray()};
            for (const id of ids) {
                await fetch('https://www.gog.com/wishlist/add/' + id, { method: 'GET', credentials: 'include' });
            }
            out = ids.map(id => ({ uid: id, name: '' }));
        """.trimIndent()))
    }

    // Shared by ownedGames + wishlist: same paginated products endpoint shape, only the URL differs.
    private fun paginated(url: String) = SyncScripts.wrap("""
        out = [];
        let page = 1, totalPages = 1;
        do {
            let r = await fetch('$url' + page,
                { credentials: 'include', headers: { 'X-Requested-With': 'XMLHttpRequest' } });
            let j = await r.json();
            totalPages = j.totalPages || 1;
            out = out.concat((j.products || []).map(p => ({ uid: String(p.id), name: p.title || '' })));
        } while (++page <= totalPages);
    """.trimIndent())

    private companion object {
        const val ACCOUNT = "https://www.gog.com/account"
    }
}
