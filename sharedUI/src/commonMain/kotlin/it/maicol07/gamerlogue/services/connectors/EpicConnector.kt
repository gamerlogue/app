package it.maicol07.gamerlogue.services.connectors

import co.touchlab.kermit.Logger
import it.maicol07.gamerlogue.services.EpicApi
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.ServiceProfile
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep

/**
 * Epic Games Store.
 *
 * Epic redirects its landing pages across sub-domains (www → store, account → accounts), so any
 * cross-origin `fetch` from an injected script hits CORS. Profile and owned games use the API path
 * (like PSN/Xbox): the WebView only navigates to the `id/api/redirect` JSON endpoint to grab a launcher
 * authorization code, then [EpicApi] exchanges it and reads the identity + library from Kotlin
 * (off-WebView, no CORS); the token response already carries the displayName, so the profile needs no
 * extra call. The wishlist is store-only (not reachable with the launcher token), so it's read via the
 * store's GraphQL POSTed same-origin from a store page, and written per-game by opening each product
 * page and clicking its bookmark button (like PSN).
 */
class EpicConnector(private val api: EpicApi) :
    ServiceConnector(ExternalService.EPIC, host = "epicgames.com", externalGameSource = 26) {
    override fun loginUrl() = "https://www.epicgames.com/id/login"

    override fun sessionUrls() = listOf("https://www.epicgames.com/", "https://store.epicgames.com/")

    // When an id.epicgames.com session already exists, the login page shows an account picker with a
    // "Continue with my account" button instead of the credential form. Auto-click it so the user isn't
    // prompted every time. Text match on "continu" covers the localized label (e.g. IT "Continua").
    // Fire-and-forget + no-op when the button is absent (real first login), so it's safe on any load.
    override fun loginTriggerScript() = """
        (function() {
            var tries = 0;
            (function tick() {
                var els = Array.prototype.slice.call(document.querySelectorAll('button, a'));
                var btn = els.find(function(e) { return /continu/i.test(e.textContent || ''); });
                if (btn) { console.log('[GL] epic continue click'); btn.click(); return; }
                if (++tries > 40) return;
                setTimeout(tick, 500);
            })();
        })();
    """.trimIndent()

    // Recognise an Epic store product page (returns its slug) so IGDB `websites` URLs are matched for push.
    override fun uidFromUrl(url: String) =
        Regex("epicgames\\.com/(?:store/)?(?:[a-z]{2}-[A-Z]{2}/)?(?:p|product)/([^/?#]+)")
            .find(url)?.groupValues?.get(1)

    // Strip the `/xx-XX` locale segment so the store redirects to the user's own region.
    override fun normalizePushUrl(url: String) =
        url.replace(Regex("(store\\.epicgames\\.com)/[a-z]{2}-[A-Z]{2}/"), "$1/")

    // Profile + owned games both use the API path (the credential is a launcher authorization code).
    override fun ownedViaApi() = true

    // Navigate straight to the JSON `id/api/redirect` endpoint and read the code from the body — a
    // `fetch` from any Epic page is cross-origin (CORS-blocked), but a direct navigation is same-origin
    // with the www session cookies. Falls back to a `code=` query param if the endpoint 302-redirects.
    override fun credentialStep() = WebStep(
        "https://www.epicgames.com/id/api/redirect?clientId=$LauncherClientId&responseType=code",
        SyncScripts.wrap("""
            let code = '';
            try {
                let j = JSON.parse(document.body.innerText || '{}');
                code = j.authorizationCode || '';
            } catch (e) {}
            if (!code) code = (location.href.match(/code=([^&]+)/) || [])[1] || '';
            console.log('[GL] epic code=' + (code ? 'ok' : 'empty'));
            out = code ? [{ uid: code, name: 'code' }] : [];
        """.trimIndent()),
    )

    // The launcher token response includes displayName + account_id, so the profile needs no extra call.
    // Epic's launcher API exposes no avatar URL; profileUrl points at the public store profile page.
    override suspend fun apiProfile(credential: String): ServiceProfile? {
        if (credential.isBlank()) return null
        return runCatching {
            val token = api.token(credential)
            token.displayName.takeIf { it.isNotBlank() }?.let { name ->
                ServiceProfile(
                    username = name,
                    avatarUrl = null,
                    profileUrl = token.accountId.takeIf { it.isNotBlank() }
                        ?.let { "https://store.epicgames.com/u/$it" },
                )
            }
        }.onFailure { Logger.w(throwable = it) { "Epic profile failed" } }.getOrNull()
    }

    override suspend fun apiOwned(credential: String): List<ExternalGameRef> {
        if (credential.isBlank()) return emptyList()
        return runCatching { api.ownedGames(api.token(credential).accessToken) }
            .onFailure { Logger.w(throwable = it) { "Epic ownedGames failed" } }
            .getOrDefault(emptyList())
    }

    // Wishlist read via the store's own GraphQL, POSTed same-origin from a store page (so no CORS and no
    // dependence on the off-screen DOM rendering). The launcher token can't reach it — the wishlist is a
    // store concept — but the store session cookie can. Logs any GraphQL errors (e.g. persisted-query
    // enforcement) for tuning. uid = offerId (name drives the IGDB match, as Epic has no URL template).
    override fun readWishlist() = WebStep("https://store.epicgames.com/", SyncScripts.wrap("""
        let q = 'query wishlistQuery { Wishlist { wishlistItems { elements { offerId namespace offer { title productSlug } } } } }';
        let r = await fetch('https://store.epicgames.com/graphql', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ query: q, operationName: 'wishlistQuery' }),
        });
        let j = await r.json();
        console.log('[GL] epic wishlist status=' + r.status + ' err=' + (j.errors ? JSON.stringify(j.errors).slice(0, 200) : 'none'));
        let els = (((j.data || {}).Wishlist || {}).wishlistItems || {}).elements || [];
        let byUid = {};
        els.forEach(function(e) {
            let offer = e.offer || {};
            let uid = e.offerId || offer.productSlug || '';
            let name = offer.title || '';
            if (uid && name) byUid[uid] = { uid: String(uid), name: name };
        });
        out = Object.values(byUid);
        console.log('[GL] epic wishlist games=' + out.length);
    """.trimIndent()))

    // Push per game: open the product page and click its bookmark button. The button's icon carries a
    // language-independent data-testid — "empty-icon" (not wishlisted) / "filled-icon" (already added) —
    // so we detect state without reading the localized label. Click then verify with retry, since the
    // store is React+SSR and an early click can hit an unbound handler (same fix as the PSN connector).
    override fun pushesPerGame() = true

    override fun wishlistPushStep(storeUrl: String) = WebStep(storeUrl, SyncScripts.wrap("""
        let wait = function(ms) { return new Promise(function(r) { setTimeout(r, ms); }); };
        let btnWith = function(testid) {
            return Array.prototype.find.call(document.querySelectorAll('button'), function(b) {
                return b.querySelector('svg[data-testid="' + testid + '"]') && b.offsetParent !== null;
            });
        };
        let confirmed = function() { return !!btnWith('filled-icon'); };
        let attempts = 0;
        for (let i = 0; i < 6 && !confirmed(); i++) {
            let btn = btnWith('empty-icon');
            if (!btn) { await wait(500); continue; }
            attempts++;
            btn.click();
            await wait(1200);
        }
        let ok = confirmed();
        out = ok ? [{ uid: '$storeUrl', name: 'added' }] : [];
        console.log('[GL] epic wishlist push added=' + ok + ' attempts=' + attempts + ' ' + location.href);
    """.trimIndent()))

    private companion object {
        // Public "fortnitePCGameClient" launcher client id — used to request the authorization code.
        const val LauncherClientId = "34a02cf8f4414e29b15921876da36f9a"
    }
}
