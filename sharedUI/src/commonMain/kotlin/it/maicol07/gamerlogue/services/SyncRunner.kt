package it.maicol07.gamerlogue.services

import at.released.igdbclient.model.Game
import co.touchlab.kermit.Logger
import com.russhwolf.settings.ObservableSettings
import it.maicol07.gamerlogue.extensions.igdb.baseGameId
import it.maicol07.gamerlogue.ui.components.SyncPhase
import it.maicol07.gamerlogue.ui.components.WebSession
import it.maicol07.gamerlogue.ui.components.confirmLibraryAdditions
import it.maicol07.gamerlogue.ui.components.writeBatches
import it.maicol07.gamerlogue.ui.components.writeEach
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * The store flows that run without a preview, shared by the sync screen and the background sync: each signs in
 * through the [WebSession], reads and writes the service, and returns its [SyncOutcome]. A failed read, library load,
 * write or sign-in throws; [SyncCoordinator] records which.
 */
// The shared flows plus the write helpers they need, moved together out of the view model.
@Suppress("TooManyFunctions")
@Single
class SyncRunner(
    private val connectors: Map<ExternalService, ServiceConnector>,
    private val settings: ObservableSettings,
    private val librarySync: LibrarySync,
    private val trackerSync: TrackerSync,
    private val matcher: GameMatcher,
) {
    /**
     * Whether [kind] can run for [service] with nobody to review it: the service supports it and, for a library
     * sync, the reviewed first sync happened, since nothing is written unattended before it.
     */
    fun runsUnattended(service: ExternalService, kind: SyncKind): Boolean {
        val connector = connectors.getValue(service)
        return when (kind) {
            SyncKind.WISHLIST -> connector.wishlist != null
            SyncKind.LIBRARY -> connector.statusWrite != null && trackerSync.base(service).isNotEmpty()
        }
    }

    /** Two-way wishlist sync: pulls the store wishlist, then pushes the backlog games missing from it. */
    suspend fun syncWishlist(service: ExternalService, session: WebSession): SyncOutcome.WishlistSynced {
        val connector = connectors.getValue(service)
        session.awaitLogin(connector)
        val wishlist = wishlistRefs(connector, session)
        val result = librarySync.pullWishlist(connector, wishlist)
        val selected = when (service) {
            ExternalService.BACKLOGGERY -> session.confirmPush(markPresent(connector, session, result.toPush))
            else -> result.toPush.filterNot { it.removedFromStore }
        }
        val push = pushWishlist(connector, session, selected)
        setLastSyncAt(service)
        val skipped = session.leftOut(push.missed, selected.associate { it.name to it.gameId })
        return SyncOutcome.WishlistSynced(
            result.added,
            push.count,
            skipped,
            result.removedOnOneSide,
            result.toPush.filter { it.removedFromStore }.map { it.asSyncGame() },
            received = result.addedGames.map {
                SyncGame(it.baseGameId, it.name, it.cover?.image_id, GameLibraryStatus.BACKLOG)
            },
            sent = push.sent(skipped).map { it.asSyncGame() },
        )
    }

    /**
     * Unattended tracker library sync: synced mappings and confident matches only, and only the changes one
     * side made since the last sync — conflicts are counted and left for the reviewed sync. Null for a service
     * that can only be imported from.
     */
    suspend fun syncLibrary(service: ExternalService, session: WebSession): SyncOutcome.LibrarySynced? {
        val connector = connectors.getValue(service)
        val write = connector.statusWrite ?: return null
        session.awaitLogin(connector)
        val refs = session.read(connector.ownedGames, SyncPhase.READING)
        // A tracker that held games at the last sync and now reads none is a broken read, not an emptied library:
        // merged, it would look like every game was removed there and resend the rest as new.
        if (refs.isEmpty() && trackerSync.base(service).isNotEmpty()) {
            throw ServiceReadException("$service library read returned no games")
        }
        val entries = librarySync.allEntries().toMutableList()
        val (remote, onTracker) = trackerSync.resolve(connector, refs)
        val plan = trackerSync.plan(connector, remote, entries, onTracker)
        trackerSync.record(service, plan.agreed)
        trackerSync.prune(service, plan.forgotten)
        // Still in Gamerlogue, so it's the tracker that removed it.
        val removedOnTracker = plan.removedOnOneSide intersect entries.mapTo(mutableSetOf()) { it.gameId }
        val incoming = plan.changes.filter { it.winner == SyncSide.REMOTE }
        val pulled = trackerSync.pull(service, incoming, entries, remote)
        val outgoing = trackerSync.outgoing(plan.changes.filter { it.winner == SyncSide.LOCAL }, entries)
        val (pushed, missed) = writeStatuses(service, session, write, outgoing)
        setLastSyncAt(service)
        val skipped = session.leftOut(missed, outgoing.associate { it.name to it.gameId })
        val games = gamesById(incoming.map { it.gameId } + outgoing.map { it.gameId } + removedOnTracker)
        val statusOf = entries.associate { it.gameId to it.status }
        return SyncOutcome.LibrarySynced(
            pulled,
            pushed,
            skipped,
            plan.changes.count { it.winner == null },
            plan.removedOnOneSide.size,
            removedOnTracker.map { syncGame(it, games, statusOf[it]) },
            // ponytail: a pull that saved only some lists them all; it isn't recorded then, so it's planned again.
            received = if (pulled > 0) incoming.map { syncGame(it.gameId, games, it.remote) } else emptyList(),
            sent = outgoing.sentOf(skipped).map { syncGame(it.gameId, games, it.to, it.name) },
        )
    }

    /**
     * Writes the tracker side of a reviewed library sync, handed over by the preview screen. The plan stays
     * pending until the push succeeds, so a failed write or login can be retried. Null for an import-only service.
     */
    suspend fun applyReviewedLibrary(service: ExternalService, session: WebSession): SyncOutcome.LibrarySynced? {
        val connector = connectors.getValue(service)
        val write = connector.statusWrite ?: return null
        session.awaitLogin(connector)
        val pending = trackerSync.pendingWrites[service].orEmpty()
        val (pushed, missed) = writeStatuses(service, session, write, pending)
        trackerSync.pendingWrites.remove(service)
        setLastSyncAt(service)
        val skipped = session.leftOut(missed, pending.associate { it.name to it.gameId })
        val sent = pending.sentOf(skipped)
        val games = gamesById(sent.map { it.gameId })
        // The reviewed plan already showed what was removed on one side.
        return SyncOutcome.LibrarySynced(
            pulled = 0,
            pushed,
            skipped,
            conflicts = 0,
            removedOnOneSide = 0,
            sent = sent.map { syncGame(it.gameId, games, it.to, it.name) },
        )
    }

    /**
     * Review and push backlog games after the incoming wishlist has been confirmed. The reviewed import is
     * only held in memory, so after a screen recreation or process death there is nothing to push: null. It is
     * dropped once the push succeeds, so a failed one can be retried.
     */
    suspend fun pushReviewedWishlist(service: ExternalService, session: WebSession): SyncOutcome.WishlistSynced? {
        val connector = connectors.getValue(service)
        val pending = librarySync.pendingWishlistImports[service]
        if (pending == null) {
            Logger.w { "No reviewed wishlist import to push for $service" }
            return null
        }
        session.awaitLogin(connector)
        // Outgoing preview: let the user pick which backlog games to push to the store, then push them. A store
        // that can't be written to gets no checklist: every row would just read "no match".
        val selected = if (connector.wishlistWrite == null) {
            emptyList()
        } else {
            val reviewed = when (service) {
                ExternalService.BACKLOGGERY -> markPresent(connector, session, pending.toPush)
                else -> pending.toPush
            }
            session.confirmPush(reviewed)
        }
        val push = pushWishlist(connector, session, selected)
        librarySync.pendingWishlistImports.remove(service)
        val skipped = session.leftOut(push.missed, selected.associate { it.name to it.gameId })
        return SyncOutcome.WishlistSynced(
            pending.added,
            push.count,
            skipped,
            pending.removedOnOneSide,
            sent = push.sent(skipped).map { it.asSyncGame() },
        )
    }

    private fun setLastSyncAt(service: ExternalService) =
        settings.putLong(service.settingsKey(LAST_SYNC_AT), Clock.System.now().toEpochMilliseconds())

    /** Writes the (reviewed) [changes] and returns how many were written and the ones the tracker didn't confirm. */
    private suspend fun writeStatuses(
        service: ExternalService,
        session: WebSession,
        write: (List<StatusChange>) -> WebStep,
        changes: List<StatusChange>,
    ): Pair<Int, List<String>> {
        val reviewed = if (service == ExternalService.BACKLOGGERY) session.confirmLibraryAdditions(changes) else changes
        val missed = mutableListOf<String>()
        val written = session.writeBatches(reviewed, write) { batch, written ->
            missed += unconfirmed(batch, written) { listOfNotNull(it.gameId.toString(), it.uid, it.name) }.map { it.name }
            trackerSync.recordWritten(service, batch, written)
            // Drop what's written from a pending reviewed plan, so a retry after a failure doesn't resend it.
            val done = written.mapNotNull { it.gameId }.toSet()
            trackerSync.pendingWrites[service]?.let { pending ->
                trackerSync.pendingWrites[service] = pending.filterNot { it.gameId in done }
            }
        }
        return written to missed
    }

    /**
     * The [missed] games of this flow's write: with the reason its script reported, else just unconfirmed. A game
     * named in [sent] (name to IGDB id, as this flow sent it) gets its id and cover, for the history to show it.
     */
    private suspend fun WebSession.leftOut(missed: List<String>, sent: Map<String, Int>): List<SkippedWrite> {
        val explained = skips.map { it.name }.toSet()
        val left = skips + missed.filterNot { it in explained }.map { SkippedWrite(it, SkipReason.UNCONFIRMED) }
        val ids = left.mapNotNull { sent[it.name] }.distinct()
        if (ids.isEmpty()) return left
        val covers = matcher.gamesByIds(ids).associate { it.id.toInt() to it.cover?.image_id }
        return left.map { skip -> sent[skip.name]?.let { skip.copy(gameId = it, coverImageId = covers[it]) } ?: skip }
    }

    /** The IGDB games of [ids], fetched at once for the history to name and show them. */
    private suspend fun gamesById(ids: Collection<Int>): Map<Int, Game> =
        if (ids.isEmpty()) emptyMap() else matcher.gamesByIds(ids.distinct()).associateBy { it.id.toInt() }

    /** Game [id] as the history lists it, named from [games] (else [name]) with [status]. */
    private fun syncGame(id: Int, games: Map<Int, Game>, status: GameLibraryStatus?, name: String = "") =
        SyncGame(id, games[id]?.name ?: name, games[id]?.cover?.image_id, status)

    /** The changes whose write went through: the ones [skipped] doesn't name. */
    private fun List<StatusChange>.sentOf(skipped: List<SkippedWrite>): List<StatusChange> {
        val left = skipped.mapTo(mutableSetOf()) { it.name }
        return filterNot { it.name in left }
    }

    private fun LibrarySync.OutgoingGame.asSyncGame() = SyncGame(gameId, name, coverImageId, GameLibraryStatus.BACKLOG)

    /** A wishlist push: the games the store confirmed, the names it did not, and the games it was asked to take. */
    private class WishlistPush(
        val count: Int,
        val missed: List<String>,
        val attempted: List<LibrarySync.OutgoingGame>
    ) {
        /** The [attempted] games that went through: the ones [skipped] doesn't name. */
        fun sent(skipped: List<SkippedWrite>): List<LibrarySync.OutgoingGame> {
            val left = skipped.mapTo(mutableSetOf()) { it.name }
            return attempted.filterNot { it.name in left }
        }
    }

    /** The [games] none of the [written] refs confirms, matched on any of their [keys] (IGDB id, store uid, name). */
    private fun <T> unconfirmed(games: List<T>, written: List<ExternalGameRef>, keys: (T) -> List<String>): List<T> {
        val confirmed = written.flatMap { listOfNotNull(it.gameId?.toString(), it.uid, it.name) }
            .filter { it.isNotBlank() }.toSet()
        return games.filterNot { game -> keys(game).any { it in confirmed } }
    }

    /**
     * Backloggery refuses to add a game its library already has (any ownership), so flag those for the review
     * like games already on the wishlist. Matched on the IGDB marker, else the title.
     *
     * ponytail: title-only match, ignoring the platform the push would pick; a same-title remake on another
     * platform gets flagged too. Match the platform as well if that turns out to hide real candidates.
     */
    private suspend fun markPresent(
        connector: ServiceConnector,
        session: WebSession,
        games: List<LibrarySync.OutgoingGame>,
    ): List<LibrarySync.OutgoingGame> {
        if (games.isEmpty()) return games
        val library = session.read(connector.ownedGames, SyncPhase.READING)
        val ids = library.mapNotNull { it.gameId }.toSet()
        val titles = library.map { it.name.trim().lowercase() }.toSet()
        return games.map { game ->
            val present = game.gameId in ids || game.name.trim().lowercase() in titles
            if (present) game.copy(alreadyOnWishlist = true) else game
        }
    }

    // The connector's [ServiceConnector.wishlist] source decides Web vs API internally; null = no wishlist.
    private suspend fun wishlistRefs(connector: ServiceConnector, session: WebSession): List<ExternalGameRef> =
        connector.wishlist?.let { session.read(it, SyncPhase.READING) }.orEmpty()

    /**
     * Pushes [games] to the store wishlist and returns how many the store confirmed (by the write results) and
     * the names of those it could push it didn't confirm.
     */
    private suspend fun pushWishlist(
        connector: ServiceConnector,
        session: WebSession,
        games: List<LibrarySync.OutgoingGame>,
    ): WishlistPush {
        val write = connector.wishlistWrite
        if (write == null || games.isEmpty()) return WishlistPush(0, emptyList(), emptyList())
        // Only games on this platform and not already wishlisted; URL-based strategies also need a store page.
        val onPlatform = games.filter { it.onPlatform && !it.alreadyOnWishlist }
        val withStorePage = onPlatform.filter { it.storeUrl != null }
        val missed = mutableListOf<LibrarySync.OutgoingGame>()
        val pushed = writeWishlist(write, session, onPlatform, withStorePage, missed)
        val attempted = if (write.needsStorePage) withStorePage else onPlatform
        return WishlistPush(pushed, missed.map { it.name }, attempted)
    }

    // One linear branch per WishlistWrite variant; splitting it would only scatter the dispatch.
    @Suppress("CyclomaticComplexMethod")
    private suspend fun writeWishlist(
        write: WishlistWrite,
        session: WebSession,
        onPlatform: List<LibrarySync.OutgoingGame>,
        withStorePage: List<LibrarySync.OutgoingGame>,
        missed: MutableList<LibrarySync.OutgoingGame>,
    ): Int {
        val record = { batch: List<LibrarySync.OutgoingGame>, written: List<ExternalGameRef> ->
            missed += unconfirmed(batch, written) { listOf(it.gameId.toString(), it.uid, it.name) }
        }
        return when (write) {
            is WishlistWrite.Batch ->
                if (withStorePage.isEmpty()) {
                    0
                } else {
                    session.run(write.step(withStorePage.map { ExternalGameRef(it.uid, it.name) }))
                        .also { record(withStorePage, it) }.size
                }
            // Per-game write: open each store page and click its add-to-wishlist button (e.g. PSN).
            is WishlistWrite.PerGame -> session.writeEach(withStorePage, missed) { g ->
                write.step(g.storeUrl!!)?.let { session.run(it).size } ?: 0
            }
            // Resolve the real product URL from an intermediate page first, then act on it (Nintendo).
            is WishlistWrite.PerGameResolved -> session.writeEach(withStorePage, missed) { g ->
                val resolved = session.run(write.resolve(g.storeUrl!!)).firstOrNull()?.uid
                if (resolved.isNullOrBlank()) {
                    session.reportSkip(SkippedWrite(g.name, SkipReason.NOT_FOUND))
                    0
                } else {
                    write.step(resolved)?.let { session.run(it).size } ?: 0
                }
            }
            // Search-driven write: no store URL needed, the game's name drives the search (Ubisoft).
            is WishlistWrite.SearchByName -> session.writeEach(onPlatform, missed) { g ->
                session.run(write.step(g.name)).size
            }
            // IGDB-id write: the tracker's game ids are IGDB ids, so no store page is needed (Backloggd).
            is WishlistWrite.ById ->
                session.writeBatches(onPlatform, { batch -> write.step(batch.map { it.gameId }) }, record)
            is WishlistWrite.ByGame -> session.writeBatches(onPlatform, write.step, record)
        }
    }

    /** [writeEach] that adds to [missed] every game whose write confirmed nothing. */
    private suspend fun WebSession.writeEach(
        games: List<LibrarySync.OutgoingGame>,
        missed: MutableList<LibrarySync.OutgoingGame>,
        action: suspend (LibrarySync.OutgoingGame) -> Int,
    ): Int = writeEach(games) { g -> action(g).also { if (it == 0) missed += g } }

    companion object {
        const val LAST_SYNC_AT = "lastSyncAt"
    }
}
