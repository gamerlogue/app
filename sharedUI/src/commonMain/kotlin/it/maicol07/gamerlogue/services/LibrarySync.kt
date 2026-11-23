package it.maicol07.gamerlogue.services

import at.released.igdbclient.model.Game
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.russhwolf.settings.ObservableSettings
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.LibraryChange
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.core.safeRequest
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.extensions.allPages
import it.maicol07.gamerlogue.extensions.currentUserEntries
import it.maicol07.gamerlogue.extensions.detachedCopy
import it.maicol07.gamerlogue.extensions.igdb.baseGameId
import it.maicol07.gamerlogue.extensions.igdb.platformNames
import it.maicol07.gamerlogue.extensions.quickDraft
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/** The user's library could not be loaded. Never read it as an empty library: writing against one duplicates entries. */
class LibraryLoadException(message: String) : IllegalStateException(message)

/**
 * The wishlist baseline after a sync: the [previous] games still on one side (one missing from a side was removed
 * there, so the other side keeps it out of the merge) plus the ones now on both the store [wishlist] and in the [library].
 */
internal fun nextWishlistBase(previous: Set<Int>, wishlist: Set<Int>, library: Set<Int>): Set<Int> =
    (previous + (wishlist intersect library)) intersect (wishlist + library)

/** The persisted wishlist baseline of each service (see [nextWishlistBase]). */
@Single
class WishlistSyncState(private val settings: ObservableSettings) {
    /** IGDB ids on both the [service] wishlist and in the library at a past wishlist sync, and still on one side. */
    fun base(service: ExternalService): Set<Int> =
        settings.getString(service.settingsKey(KEY), "").split(',').mapNotNull { it.toIntOrNull() }.toSet()

    /** Record the baseline of [service] after a sync that saw [wishlist] on the store and [entries] locally. */
    fun record(service: ExternalService, wishlist: Set<Int>, entries: List<LibraryEntry>) {
        val base = nextWishlistBase(base(service), wishlist, entries.mapTo(mutableSetOf()) { it.gameId })
        settings.putString(service.settingsKey(KEY), base.joinToString(","))
    }

    fun forget(service: ExternalService) = settings.remove(service.settingsKey(KEY))

    private companion object {
        const val KEY = "wishlistSyncState"
    }
}

/**
 * Writes Gamerlogue [LibraryEntry]s from data read off an external store, reusing the JSON:API CRUD.
 *
 * Two flows: [importOwned] (store → Gamerlogue, one-way, after the user confirms the mapping preview)
 * and [pullWishlist] (bidirectional additive merge; wishlist ≡ [GameLibraryStatus.BACKLOG]; a game removed on one
 * side since the last sync isn't added back there, see [WishlistSyncState]).
 */
@Single
class LibrarySync(
    private val matcher: GameMatcher,
    private val exceptionReporter: ExceptionReporter,
    private val libraryChanges: LibraryChanges,
    private val profileApi: ProfileApi,
    val wishlistState: WishlistSyncState,
) {
    /** Entries imported by the reviewed wishlist flow before its outgoing WebView step. */
    val pendingWishlistImports: MutableMap<ExternalService, WishlistResult> = mutableMapOf()

    private companion object {
        /** Parallel save requests during an import. */
        const val MAX_CONCURRENT_SAVES = 4
    }

    /** [added] = entries created in Gamerlogue, [addedGames] their games; [toPush] = backlog games to add to the store
     *  wishlist; [removedOnOneSide] = games removed on one side since the last sync and left on the other. */
    data class WishlistResult(
        val added: Int,
        val toPush: List<OutgoingGame>,
        val removedOnOneSide: Int,
        val addedGames: List<Game> = emptyList(),
        /** The store games matched only by a guess or not at all: left out, waiting for the user. */
        val unsure: List<GameMatcher.Match> = emptyList(),
        /** The games removed from the library since a past sync and still on the store wishlist, by IGDB id. */
        val removedHere: Map<Int, ExternalGameRef> = emptyMap(),
    )

    /** A Gamerlogue backlog game about to be pushed to a store wishlist (for the outgoing preview).
     *  [onPlatform] is false when the game doesn't release on the store's platform family (shown in a
     *  separate, non-pushable section); pushable rows need both [onPlatform] and a non-null [storeUrl] —
     *  except for by-name, by-id and full-game writes. Those need [matchesPublisher]
     *  ([ServiceConnector.matchesPublisher]) to confirm the game belongs on the target service. */
    data class OutgoingGame(
        val gameId: Int,
        val uid: String,
        val name: String,
        val coverImageId: String?,
        val storeUrl: String?,
        val onPlatform: Boolean = true,
        /** Already on the store wishlist — shown in the preview but disabled/deselected, not pushed. */
        val alreadyOnWishlist: Boolean = false,
        /** Removed from the store wishlist since the last sync — shown in the preview, but not preselected. */
        val removedFromStore: Boolean = false,
        val matchesPublisher: Boolean = true,
        val platformIds: List<Int> = emptyList(),
        val platformNames: Map<Int, List<String>> = emptyMap(),
    ) {
        /** Use a platform explicitly selected in the library, or the game's sole platform. */
        val defaultPlatformId: Int?
            get() = platformNames.filterValues { it.isNotEmpty() }.let { available ->
                platformIds.firstOrNull { it in available } ?: available.keys.singleOrNull()
            }

        /** [defaultPlatformId], else — for a game with no platform in the library — the first of the user's
         *  [preferred] platforms it releases on. */
        fun preselectedPlatformId(preferred: List<Int>): Int? = defaultPlatformId
            ?: preferred.takeIf { platformIds.isEmpty() }?.firstOrNull { platformNames[it].orEmpty().isNotEmpty() }

        companion object {
            /** The platforms of the [confirmed] games, most used first, ahead of the [previous] preference. */
            fun rankPlatforms(confirmed: List<OutgoingGame>, previous: List<Int>): List<Int> {
                val used = confirmed.mapNotNull { it.platformIds.firstOrNull() }
                    .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
                return used + (previous - used.toSet())
            }
        }
    }

    /** Persist confirmed IGDB [games] as owned. Existing entries keep their status/ratings and only
     *  gain `owned=true`; new ones become owned backlog items. [connector] derives `platformsIds`
     *  (see [platformIdsFor]) for newly created entries. Returns how many saved successfully. */
    suspend fun importOwned(connector: ServiceConnector, games: List<Game>, snapshot: MutableList<LibraryEntry>? = null): Int {
        val entries = snapshot ?: allEntries().toMutableList()
        val existing = entries.associateBy { it.gameId }
        return persistEach(games, entries) { game ->
            LibraryEntry.quickDraft(
                game,
                existing[game.baseGameId]?.status ?: GameLibraryStatus.BACKLOG,
                existing = existing[game.baseGameId]?.detachedCopy(::LibraryEntry),
                platformsIds = connector.platformIdsFor(game),
            ).apply { owned = true }
        }.size
    }

    /** Persist confirmed wishlisted [games] as BACKLOG (owned=false), skipping games already tracked.
     *  [connector] derives `platformsIds` (see [platformIdsFor]) for the new entries. Returns how many entries
     *  were created: an existing entry that only gains an edition isn't counted. */
    suspend fun importWishlist(
        connector: ServiceConnector,
        games: List<Game>,
        snapshot: MutableList<LibraryEntry>? = null,
    ): Int {
        val entries = snapshot ?: allEntries().toMutableList()
        val existing = entries.associateBy { it.gameId }
        val missing = games.filter { game ->
            val entry = existing[game.baseGameId]
            entry == null || (game.version_parent != null &&
                game.id.toInt() !in runCatching { entry.editionsIds }.getOrDefault(emptyList()))
        }
        return persistEach(missing, entries) { game ->
            val entry = existing[game.baseGameId]
            val owned = entry?.owned ?: false
            LibraryEntry.quickDraft(
                game,
                entry?.status ?: GameLibraryStatus.BACKLOG,
                existing = entry?.detachedCopy(::LibraryEntry),
                platformsIds = connector.platformIdsFor(game),
            ).apply { this.owned = owned }
        }.count { it.gameId !in existing }
    }

    /** Set each game's status: existing entries are updated in place (keeping `owned`), missing ones are
     *  created as not owned. Returns how many saved successfully. */
    suspend fun applyStatuses(games: List<Pair<Game, GameLibraryStatus>>, snapshot: MutableList<LibraryEntry>? = null): Int {
        val entries = snapshot ?: allEntries().toMutableList()
        val existing = entries.associateBy { it.gameId }
        val statusOf = games.associate { (game, status) -> game.id to status }
        return persistEach(games.map { it.first }, entries) { game ->
            val entry = existing[game.baseGameId]
            val owned = entry?.owned ?: false
            LibraryEntry.quickDraft(game, statusOf.getValue(game.id), existing = entry?.detachedCopy(::LibraryEntry))
                .apply { this.owned = owned }
        }.size
    }

    /** Saves one entry per base game and returns the saved ones. */
    private suspend fun persistEach(
        games: List<Game>,
        entries: MutableList<LibraryEntry>,
        draft: (Game) -> LibraryEntry,
    ): List<LibraryEntry> = coroutineScope {
        // One import batch per sync: the server sums its new entries into one `library_imported` activity.
        val batch = Uuid.random().toString()
        // Distinct by id: several store entries (game + demo/DLC) can map to one IGDB game; saving the
        // same gameId twice would create a duplicate / be rejected, leaving fewer entries than expected.
        val distinct = games.groupBy { it.baseGameId }
        // An import is one request per game; running them strictly one at a time makes a few hundred
        // games a few hundred serial round-trips. The semaphore keeps the backend from seeing a burst.
        val inFlight = Semaphore(MAX_CONCURRENT_SAVES)
        distinct.values.map { editions ->
            val game = editions.first()
            async {
                val result = inFlight.withPermit {
                    exceptionReporter.safeRequest {
                        val entry = draft(game)
                        val editionIds = editions.filter { it.version_parent != null }.map { it.id.toInt() }
                        if (editionIds.isNotEmpty()) {
                            entry.editionsIds =
                                (runCatching { entry.editionsIds }.getOrDefault(emptyList()) + editionIds).distinct()
                        }
                        profileApi.saveImported(entry, batch)
                        // An entry saved under an edition's id (legacy) is still looked up by its base game.
                        entry.gameId = game.baseGameId
                        entry
                    }
                }
                val saved = result.get()
                if (saved == null) {
                    Logger.w(tag = "LibrarySync", throwable = result.getError()) {
                        "import failed: ${game.name} (${game.id})"
                    }
                }
                saved
            }
        }.mapNotNull { it.await() }.also { saved ->
            if (saved.size < distinct.size) {
                // A failed save may still have reached the server (a timeout, an unreadable response): reload, so the
                // next write of this flow doesn't create a duplicate of it.
                entries.refill(allEntries())
            } else {
                val ids = saved.mapTo(mutableSetOf()) { it.gameId }
                entries.removeAll { it.gameId in ids }
                entries.addAll(saved)
            }
            if (saved.isNotEmpty()) libraryChanges.notifyChanged(LibraryChange.Other)
        }
    }

    private fun MutableList<LibraryEntry>.refill(fresh: List<LibraryEntry>) {
        clear()
        addAll(fresh)
    }

    /**
     * Backlog games not yet on the store wishlist, by IGDB id, labelled with name/cover and the
     * connector's store URL (from IGDB `external_games.url`) for the outgoing preview and push.
     * "Already on the wishlist" is decided by IGDB id (so it works whether the store gave uids or names).
     */
    suspend fun computeWishlistPush(
        connector: ServiceConnector,
        storeWishlist: List<ExternalGameRef>,
        snapshot: List<LibraryEntry>? = null,
        matchedWishlistIds: Set<Int>? = null,
    ): List<OutgoingGame> {
        val entries = snapshot ?: allEntries()
        // Only un-owned backlog games belong on a wishlist — an owned game isn't "wished for".
        val backlogIds = entries.filter { it.status == GameLibraryStatus.BACKLOG && !it.owned }.map { it.gameId }
        if (backlogIds.isEmpty()) return emptyList()
        val onWishlistIds =
            matchedWishlistIds ?: matcher.match(connector, storeWishlist).mapNotNull { it.game?.baseGameId }.toSet()
        val base = wishlistState.base(connector.service)

        // Name, IGDB-id and full-game writes never open a store page: looking the pages up is wasted requests.
        val needsUrls = connector.wishlistWrite?.needsStorePage == true
        val urls = if (needsUrls) matcher.urlsForGames(connector, backlogIds) else emptyMap()
        val games = matcher.gamesByIds(backlogIds).associateBy { it.id.toInt() }
        val platforms = entries.associate { it.gameId to runCatching { it.platformsIds }.getOrDefault(emptyList()) }
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
                removedFromStore = id in base && id !in onWishlistIds,
                matchesPublisher = game != null && connector.matchesPublisher(game),
                platformIds = platforms[id].orEmpty(),
                platformNames = game?.platformNames.orEmpty(),
            )
        }
    }

    /**
     * Merge the store wishlist with the Gamerlogue backlog (additive both ways; removals aren't propagated).
     * Adds wishlisted games missing from the library as backlog (owned=false), and returns the backlog
     * games not yet on the store wishlist so the caller can push them (except the [OutgoingGame.removedFromStore] ones).
     */
    suspend fun pullWishlist(connector: ServiceConnector, serviceWishlist: List<ExternalGameRef>): WishlistResult {
        val matches = matcher.match(connector, serviceWishlist)
        val entries = allEntries().toMutableList()
        val onWishlist = matches.mapNotNull { it.game?.baseGameId }.toSet()
        // Removed from the library since a past sync: not imported again.
        val removed = wishlistState.base(connector.service) - entries.mapTo(mutableSetOf()) { it.gameId }
        val incoming = matches.filter { it.confident }.mapNotNull { it.game }.filterNot { it.baseGameId in removed }
        val known = entries.mapTo(mutableSetOf()) { it.gameId }
        val added = importWishlist(connector, incoming, entries)
        val toPush = computeWishlistPush(connector, serviceWishlist, entries, onWishlist)
        wishlistState.record(connector.service, onWishlist, entries)
        return WishlistResult(
            added,
            toPush,
            (removed intersect onWishlist).size + toPush.count { it.removedFromStore },
            // The games that made new entries, which is what `added` counts.
            incoming.filterNot { it.baseGameId in known }.distinctBy { it.baseGameId },
            matches.filterNot { it.confident },
            matches.mapNotNull { m -> m.game?.baseGameId?.takeIf { it in removed }?.let { it to m.ref } }.toMap(),
        )
    }

    /**
     * Every entry of the user's library, keyed by base game: an entry saved under an edition's id (before editions
     * were folded into their base game) is read as the base game, with the edition kept in `editionsIds`.
     * @throws LibraryLoadException when the library could not be loaded; an empty result is a real empty library.
     */
    suspend fun allEntries(): List<LibraryEntry> {
        val entries = exceptionReporter.safeRequest { allPages { LibraryEntry.currentUserEntries() } }.get()
            ?: throw LibraryLoadException("The library could not be loaded")
        val parents = matcher.editionParents(entries.map { it.gameId })
        entries.filter { it.gameId in parents }.forEach { entry ->
            val edition = entry.gameId
            entry.gameId = parents.getValue(edition)
            entry.editionsIds = (runCatching { entry.editionsIds }.getOrDefault(emptyList()) + edition).distinct()
        }
        return entries
    }
}
