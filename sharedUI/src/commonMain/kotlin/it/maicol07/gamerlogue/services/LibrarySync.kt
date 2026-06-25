package it.maicol07.gamerlogue.services

import at.released.igdbclient.model.Game
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.allPages
import it.maicol07.gamerlogue.extensions.currentUserEntries
import it.maicol07.gamerlogue.extensions.persist
import it.maicol07.gamerlogue.extensions.quickDraft
import it.maicol07.gamerlogue.safeRequest
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus

/**
 * Writes Gamerlogue [LibraryEntry]s from data read off an external store, reusing the JSON:API CRUD.
 *
 * Two flows: [importOwned] (store → Gamerlogue, one-way, after the user confirms the mapping preview)
 * and [pullWishlist] (bidirectional additive merge; wishlist ≡ [GameLibraryStatus.BACKLOG]).
 */
class LibrarySync(
    private val matcher: GameMatcher,
    private val authProvider: AuthTokenProvider,
) {
    /** [added] = entries created in Gamerlogue; [toPush] = backlog games to add to the store wishlist. */
    data class WishlistResult(val added: Int, val toPush: List<OutgoingGame>)

    /** A Gamerlogue backlog game about to be pushed to a store wishlist (for the outgoing preview).
     *  [onPlatform] is false when the game doesn't release on the store's platform family (shown in a
     *  separate, non-pushable section); pushable rows need both [onPlatform] and a non-null [storeUrl]. */
    data class OutgoingGame(
        val gameId: Int,
        val uid: String,
        val name: String,
        val coverImageId: String?,
        val storeUrl: String?,
        val onPlatform: Boolean = true,
        /** Already on the store wishlist — shown in the preview but disabled/deselected, not pushed. */
        val alreadyOnWishlist: Boolean = false,
    )

    /** Persist confirmed IGDB [games] as owned. Existing entries keep their status/ratings and only
     *  gain `owned=true`; new ones become owned backlog items. Returns how many saved successfully. */
    suspend fun importOwned(games: List<Game>): Int {
        val existing = allEntries().associateBy { it.gameId }
        return persistEach(games) { game ->
            LibraryEntry.quickDraft(
                game,
                existing[game.id.toInt()]?.status ?: GameLibraryStatus.BACKLOG,
                authProvider.currentUser,
                existing = existing[game.id.toInt()],
            ).apply { owned = true }
        }
    }

    /** Persist confirmed wishlisted [games] as BACKLOG (owned=false), skipping games already tracked. */
    suspend fun importWishlist(games: List<Game>): Int {
        val existingIds = allEntries().mapTo(mutableSetOf()) { it.gameId }
        return persistEach(games.filter { it.id.toInt() !in existingIds }) { game ->
            LibraryEntry.quickDraft(game, GameLibraryStatus.BACKLOG, authProvider.currentUser)
        }
    }

    private suspend fun persistEach(games: List<Game>, draft: (Game) -> LibraryEntry): Int {
        var saved = 0
        // Distinct by id: several store entries (game + demo/DLC) can map to one IGDB game; saving the
        // same gameId twice would create a duplicate / be rejected, leaving fewer entries than expected.
        games.distinctBy { it.id }.forEach { game ->
            val result = draft(game).persist()
            if (result.get() != null) {
                saved++
            } else {
                Logger.w(tag = "LibrarySync", throwable = result.getError()) { "import failed: ${game.name} (${game.id})" }
            }
        }
        return saved
    }

    /**
     * Backlog games not yet on the store wishlist, by IGDB id, labelled with name/cover and the
     * connector's store URL (from IGDB `external_games.url`) for the outgoing preview and push.
     * "Already on the wishlist" is decided by IGDB id (so it works whether the store gave uids or names).
     */
    suspend fun computeWishlistPush(connector: ServiceConnector, storeWishlist: List<ExternalGameRef>): List<OutgoingGame> {
        // Only un-owned backlog games belong on a wishlist — an owned game isn't "wished for".
        val backlogIds = allEntries().filter { it.status == GameLibraryStatus.BACKLOG && !it.owned }.map { it.gameId }
        if (backlogIds.isEmpty()) return emptyList()
        val onWishlistIds = matcher.match(connector, storeWishlist).mapNotNull { it.game?.id?.toInt() }.toSet()

        val urls = matcher.urlsForGames(connector, backlogIds)
        val games = matcher.gamesByIds(backlogIds).associateBy { it.id.toInt() }
        val family = connector.platformFamily
        // Show every backlog game in the preview: ones already on the store wishlist are flagged so the UI
        // disables/deselects them, games with no store mapping (storeUrl=null) are shown disabled, and
        // games that don't release on this store's platform family are flagged (null family = PC = all ok).
        return backlogIds.map { id ->
            val url = urls[id]
            val game = games[id]
            val onPlatform = family == null ||
                game?.platforms?.any { it.platform_family?.id?.toInt() == family } == true
            OutgoingGame(
                id,
                url?.let { connector.uidFromUrl(it) } ?: id.toString(),
                game?.name ?: "",
                game?.cover?.image_id,
                url?.let { connector.normalizePushUrl(it) },
                onPlatform,
                alreadyOnWishlist = id in onWishlistIds,
            )
        }
    }

    /**
     * Merge the store wishlist with the Gamerlogue backlog (additive both ways; removals out of scope).
     * Adds wishlisted games missing from the library as backlog (owned=false), and returns the backlog
     * games not yet on the store wishlist so the caller can push them.
     */
    suspend fun pullWishlist(connector: ServiceConnector, serviceWishlist: List<ExternalGameRef>): WishlistResult {
        val matches = matcher.match(connector, serviceWishlist)
        val added = importWishlist(matches.mapNotNull { it.game })
        return WishlistResult(added, computeWishlistPush(connector, serviceWishlist))
    }

    /**
     * IGDB game ids already tracked, to flag rows that don't need importing. [ownedOnly] for the owned
     * import (only games already marked owned), otherwise any tracked game (used by the wishlist preview).
     */
    suspend fun existingGameIds(ownedOnly: Boolean): Set<Int> =
        allEntries().filter { !ownedOnly || it.owned }.mapTo(mutableSetOf()) { it.gameId }

    private suspend fun allEntries(): List<LibraryEntry> =
        safeRequest { LibraryEntry.currentUserEntries().allPages() }.get().orEmpty()
}
