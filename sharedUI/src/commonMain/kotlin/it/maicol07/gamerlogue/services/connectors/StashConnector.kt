package it.maicol07.gamerlogue.services.connectors

import com.russhwolf.settings.ObservableSettings
import io.ktor.http.encodeURLPathPart
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.IgdbKey
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncScripts
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.settingsKey
import it.maicol07.gamerlogue.services.webProfile
import it.maicol07.gamerlogue.services.webRefs
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Stash. The website has no sign-in (the account API takes the mobile app's bearer token), but profiles
 * are public: the user gives their handle ([needsHandle]) and every read scrapes `/users/{handle}/games/`.
 * Import only — there is no web write surface — and no separate wishlist: the "want" list is read with the
 * library, as backlog.
 *
 * Covers are served from IGDB, so a card's cover image id is the uid and matches exactly
 * ([IgdbKey.COVER_IMAGE_ID]); a card without one falls back to its slug and a name match.
 *
 * ponytail: markup comes from inspecting the public pages, not a tuned session — verify live. Cloudflare
 * blocks plain HTTP clients, which is why reads still go through the WebView.
 */
class StashConnector(private val settings: ObservableSettings) :
    ServiceConnector(ExternalService.STASH, host = "stash.games", externalGameSource = 0) {
    override val needsHandle = true

    override val igdbKey = IgdbKey.COVER_IMAGE_ID

    private val handle: String?
        get() = settings.getStringOrNull(service.settingsKey(HANDLE_KEY))?.takeIf { it.isNotBlank() }

    // The handle is user input: percent-encoded as a path segment here, and the URL embedded in scripts as a JSON
    // string literal, so neither the URL nor the injected JS can be broken out of.
    private val encodedHandle: String? get() = handle?.encodeURLPathPart()

    private val profileUrl: String get() = encodedHandle?.let { "https://stash.games/users/$it/" } ?: HOME

    private val profileUrlJs: String get() = Json.encodeToString(String.serializer(), profileUrl)

    override val loginUrl: String get() = profileUrl

    // Nothing to sign into: "logged in" means the user's public profile page is open.
    override suspend fun isLoggedIn(currentUrl: String) =
        encodedHandle?.let { currentUrl.contains("stash.games/users/$it", ignoreCase = true) } == true

    // The header shows the display name (`h1.profile__name`), the `@handle` and the avatar; the page also
    // carries them server-side in its og: meta. When the loaded document isn't the profile yet (e.g.
    // Cloudflare's "Just a moment" check, which reloads by itself), the page is fetched and parsed instead.
    override val profile get() = webProfile(WebStep(profileUrl, SyncScripts.wrap("""
        let doc = document;
        if (!await __glWaitFor(function() { return doc.querySelector('.profile-header__handle'); }, 20)) {
            doc = (await __glDoc($profileUrlJs)) || doc;
        }
        let name = doc.querySelector('h1.profile__name');
        let handle = doc.querySelector('.profile-header__handle');
        let img = doc.querySelector('img.user-avatar__image');
        let ogImage = doc.querySelector('meta[property="og:image"]');
        out = {
            username: (name && name.textContent.trim()) || (handle ? handle.textContent.trim().replace(/^@/, '') : ''),
            avatarUrl: (img && img.getAttribute('src')) || (ogImage && ogImage.getAttribute('content')) || '',
            profileUrl: $profileUrlJs,
        };
    """.trimIndent())))

    override val ownedGames get() = webRefs(WebStep(profileUrl, gamesScrape()))

    // Cards: `a.games-list__item-link[href="/games/{slug}"][data-text=title]`, the status in its
    // `<use href="#want|#playing|#beaten|#archived">` icon (aria-label "View game: Title — Want" as fallback).
    // 24 per page; stops on a page with no new card, in case past-the-end pages repeat the last one.
    private fun gamesScrape() = SyncScripts.wrap("""
        let statuses = { want: 'BACKLOG', playing: 'PLAYING', beaten: 'COMPLETED', archived: 'ABANDONED' };
        let seen = {};
        out = [];
        for (let page = 1; page <= 200; page++) {
            // A failed page throws, so the read reports an error instead of a partial library.
            let doc = await __glFetchDoc($profileUrlJs + 'games/?page=' + page);
            let added = 0;
            doc.querySelectorAll('a.games-list__item-link').forEach(function(a) {
                let slug = ((a.getAttribute('href') || '').match(/\/games\/([^\/?#]+)/) || [])[1];
                if (!slug || seen[slug]) return;
                seen[slug] = true;
                added++;
                let img = a.querySelector('img');
                let cover = ((img && (img.getAttribute('src') || img.getAttribute('data-src')) || '')
                    .match(/\/t_[a-z_0-9]+\/([a-z0-9]+)\./) || [])[1];
                let use = a.querySelector('use');
                let key = ((use && (use.getAttribute('href') || use.getAttribute('xlink:href'))) || '').replace('#', '');
                if (!statuses[key]) key = ((a.getAttribute('aria-label') || '').split('—').pop() || '').trim().toLowerCase();
                out.push({ uid: cover || slug, name: a.getAttribute('data-text') || '', status: statuses[key] || null });
            });
            __glProgress(out.length, 0);
            if (!added) break;
        }
    """.trimIndent())

    private companion object {
        const val HOME = "https://stash.games/"
    }
}
