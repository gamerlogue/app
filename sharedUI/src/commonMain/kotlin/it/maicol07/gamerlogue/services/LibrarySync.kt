package it.maicol07.gamerlogue.services

import at.released.igdbclient.model.Game
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.safeRequest
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.allPages
import it.maicol07.gamerlogue.extensions.currentUserEntries
import it.maicol07.gamerlogue.extensions.quickDraft
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koin.core.annotation.Single

/**
 * Writes Gamerlogue [LibraryEntry]s from data read off an external store, reusing the JSON:API CRUD.
 *
 * Two flows: [importOwned] (store → Gamerlogue, one-way, after the user confirms the mapping preview)
 * and [pullWishlist] (bidirectional additive merge; wishlist ≡ [GameLibraryStatus.BACKLOG]).
 */
@Single
class LibrarySync(
    private val matcher: GameMatcher,
    private val authProvider: AuthTokenProvider,
    private val exceptionReporter: ExceptionReporter,
) {
    private companion object {
        /** Parallel save requests during an import. */
        const val MaxConcurrentSaves = 4
    }

    /** [added] = entries created in Gamerlogue; [toPush] = backlog games to add to the store wishlist. */
    data class WishlistResult(val added: Int, val toPush: List<OutgoingGame>)

    /** A Gamerlogue backlog game about to be pushed to a store wishlist (for the outgoing preview).
     *  [onPlatform] is false when the game doesn't release on the store's platform family (shown in a
     *  separate, non-pushable section); pushable rows need both [onPlatform] and a non-null [storeUrl] —
     *  except for [it.maicol07.gamerlogue.services.WishlistWrite.SearchByName] connectors, which have no
     *  [storeUrl] and instead need [matchesPublisher] ([ServiceConnector.matchesPublisher]) to confirm the
     *  game is actually on that store before it's searched for and pushed. */
    data class OutgoingGame(
        val gameId: Int,
        val uid: String,
        val name: String,
        val coverImageId: String?,
        val storeUrl: String?,
        val onPlatform: Boolean = true,
        /** Already on the store wishlist — shown in the preview but disabled/deselected, not pushed. */
        val alreadyOnWishlist: Boolean = false,
        val matchesPublisher: Boolean = true,
    )

    /** Persist confirmed IGDB [games] as owned. Existing entries keep their status/ratings and only
     *  gain `owned=true`; new ones become owned backlog items. [connector] derives `platformsIds`
     *  (see [platformIdsFor]) for newly created entries. Returns how many saved successfully. */
    suspend fun importOwned(connector: ServiceConnector, games: List<Game>): Int {
        val existing = allEntries().associateBy { it.gameId }
        return persistEach(games) { game ->
            LibraryEntry.quickDraft(
                game,
                existing[game.id.toInt()]?.status ?: GameLibraryStatus.BACKLOG,
                authProvider.currentUser.value,
                existing = existing[game.id.toInt()],
                platformsIds = connector.platformIdsFor(game),
            ).apply { owned = true }
        }
    }

    /** Persist confirmed wishlisted [games] as BACKLOG (owned=false), skipping games already tracked.
     *  [connector] derives `platformsIds` (see [platformIdsFor]) for the new entries. */
    suspend fun importWishlist(connector: ServiceConnector, games: List<Game>): Int =
        importWishlist(connector, games, allEntries())

    private suspend fun importWishlist(
        connector: ServiceConnector,
        games: List<Game>,
        entries: List<LibraryEntry>,
    ): Int {
        val existingIds = entries.mapTo(mutableSetOf()) { it.gameId }
        return persistEach(games.filter { it.id.toInt() !in existingIds }) { game ->
            LibraryEntry.quickDraft(
                game,
                GameLibraryStatus.BACKLOG,
                authProvider.currentUser.value,
                platformsIds = connector.platformIdsFor(game),
            )
        }
    }

    private suspend fun persistEach(games: List<Game>, draft: (Game) -> LibraryEntry): Int = coroutineScope {
        // Distinct by id: several store entries (game + demo/DLC) can map to one IGDB game; saving the
        // same gameId twice would create a duplicate / be rejected, leaving fewer entries than expected.
        val distinct = games.distinctBy { it.id }
        // An import is one request per game; running them strictly one at a time makes a few hundred
        // games a few hundred serial round-trips. The semaphore keeps the backend from seeing a burst.
        val inFlight = Semaphore(MaxConcurrentSaves)
        distinct.map { game ->
            async {
                val result = inFlight.withPermit { exceptionReporter.safeRequest { draft(game).save() } }
                if (result.get() != null) {
                    true
                } else {
                    Logger.w(tag = "LibrarySync", throwable = result.getError()) {
                        "import failed: ${game.name} (${game.id})"
                    }
                    false
                }
            }
        }.count { it.await() }
    }

    /**
     * Backlog games not yet on the store wishlist, by IGDB id, labelled with name/cover and the
     * connector's store URL (from IGDB `external_games.url`) for the outgoing preview and push.
     * "Already on the wishlist" is decided by IGDB id (so it works whether the store gave uids or names).
     */
    suspend fun computeWishlistPush(
        connector: ServiceConnector,
        storeWishlist: List<ExternalGameRef>,
    ): List<OutgoingGame> = computeWishlistPush(connector, storeWishlist, allEntries())

    private suspend fun computeWishlistPush(
        connector: ServiceConnector,
        storeWishlist: List<ExternalGameRef>,
        entries: List<LibraryEntry>,
    ): List<OutgoingGame> {
        // Only un-owned backlog games belong on a wishlist — an owned game isn't "wished for".
        val backlogIds = entries.filter { it.status == GameLibraryStatus.BACKLOG && !it.owned }.map { it.gameId }
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
                matchesPublisher = game != null && connector.matchesPublisher(game),
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
        val added = importWishlist(connector, matches.mapNotNull { it.game }, allEntries())
        // Re-read: the push preview must also list the entries the import just created (flagged as
        // already on the store wishlist), which the pre-import snapshot does not contain.
        return computeWishlistPush(connector, serviceWishlist, allEntries())
            .let { WishlistResult(added, it) }
    }

    /**
     * IGDB game ids already tracked, to flag rows that don't need importing. [ownedOnly] for the owned
     * import (only games already marked owned), otherwise any tracked game (used by the wishlist preview).
     */
    suspend fun existingGameIds(ownedOnly: Boolean): Set<Int> =
        allEntries().filter { !ownedOnly || it.owned }.mapTo(mutableSetOf()) { it.gameId }

    private suspend fun allEntries(): List<LibraryEntry> =
        exceptionReporter.safeRequest { LibraryEntry.currentUserEntries().allPages() }.get().orEmpty()
}
