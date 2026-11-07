package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.parkwoocheol.composewebview.PlatformCookieManager
import com.russhwolf.settings.ObservableSettings
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.LibraryLoadException
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ScriptWriteException
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.ServiceProfile
import it.maicol07.gamerlogue.services.ServiceReadException
import it.maicol07.gamerlogue.services.SkipReason
import it.maicol07.gamerlogue.services.SkippedWrite
import it.maicol07.gamerlogue.services.StatusChange
import it.maicol07.gamerlogue.services.SyncSide
import it.maicol07.gamerlogue.services.TrackerSync
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.services.connectors.BackloggeryConnector
import it.maicol07.gamerlogue.services.settingsKey
import it.maicol07.gamerlogue.ui.components.LoginTimeoutException
import it.maicol07.gamerlogue.ui.components.SyncPhase
import it.maicol07.gamerlogue.ui.components.WebSession
import it.maicol07.gamerlogue.ui.components.confirmLibraryAdditions
import it.maicol07.gamerlogue.ui.components.writeBatches
import it.maicol07.gamerlogue.ui.components.writeEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock

/** A store flow that requires the WebView; carried by the [it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree.ServiceSync] key. */
@Serializable
enum class ServiceSyncAction {
    CONNECT,
    REFRESH_PROFILE,
    SYNC_WISHLIST,
    PREVIEW_WISHLIST,
    PUSH_WISHLIST,
    IMPORT_LIBRARY,
    RESYNC_IDS,
    APPLY_RESYNC_IDS,

    /** Tracker library sync, unattended: applies only the changes with no conflict. */
    SYNC_LIBRARY,

    /** Tracker library sync, reviewed: reads the tracker and hands off to the preview. */
    PREVIEW_LIBRARY_SYNC,

    /** Writes the tracker side of a reviewed library sync (handed over by the preview). */
    APPLY_LIBRARY_SYNC,
}

/**
 * State + actions for the Linked Services list.
 *
 * The store flows that need the WebView ([ServiceSyncAction]) run on the [ServiceSyncView], which
 * shares this ViewModel type (its own instance) and calls the `run*` suspend functions here; they
 * combine the WebView session (read/write the store) with IGDB matching and Gamerlogue persistence
 * ([LibrarySync]). Library import only reads owned games here — matching and confirmation happen on
 * the preview screen. Per-service link flags are kept in [settings] (the actual session lives in the
 * WebView cookies); [refreshAll] re-reads them when the list returns to the foreground.
 */
@KoinViewModel
class LinkedServicesViewModel(
    private val connectors: Map<ExternalService, ServiceConnector>,
    private val settings: ObservableSettings,
    private val librarySync: LibrarySync,
    private val trackerSync: TrackerSync,
) : StateViewModel<LinkedServicesViewModel.UiState>(UiState()) {


    data class ServiceState(
        val connected: Boolean = false,
        val wishlistSync: Boolean = false,
        val librarySync: Boolean = false,
        val lastSyncAt: Long? = null,
        val busy: Boolean = false,
        val profile: ServiceProfile? = null,
    )

    /** How the last WebView flow ended, for the sync screen's closing message. */
    sealed interface SyncOutcome {
        /**
         * [added] entries created in Gamerlogue, [pushed] backlog games sent to the store wishlist, [skipped]
         * selected games the store didn't take.
         */
        data class WishlistSynced(val added: Int, val pushed: Int, val skipped: List<SkippedWrite>) : SyncOutcome {
            /** False when the two libraries already agreed — nothing worth stopping to report. */
            val changedAnything: Boolean get() = added > 0 || pushed > 0 || skipped.isNotEmpty()
        }

        /**
         * [pulled] games updated from the tracker, [pushed] written to it, [skipped] selected writes the tracker
         * didn't take, [conflicts] left for a manual sync.
         */
        data class LibrarySynced(
            val pulled: Int,
            val pushed: Int,
            val skipped: List<SkippedWrite>,
            val conflicts: Int,
        ) : SyncOutcome {
            val changedAnything: Boolean get() = pulled > 0 || pushed > 0 || skipped.isNotEmpty() || conflicts > 0
        }

        data object Failed : SyncOutcome
        data class IdsResynced(val updated: Int, val unmatched: Int) : SyncOutcome
    }

    data class UiState(
        val services: Map<ExternalService, ServiceState> = emptyMap(),
        val outcome: SyncOutcome? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    init {
        refreshAll()
    }

    fun connector(service: ExternalService): ServiceConnector = connectors.getValue(service)

    /** Re-read every service's link flags from [settings] (e.g. after a sync flow on another entry). */
    fun refreshAll() = update {
        copy(services = ExternalService.entries.associateWith { readServiceState(it) })
    }

    // --- UI intents ---

    fun toggleWishlistSync(service: ExternalService, enabled: Boolean) {
        setWishlistSync(service, enabled)
        refresh(service)
    }

    /** Store the public username of a [ServiceConnector.needsHandle] service before its connect flow. */
    fun setHandle(service: ExternalService, handle: String) =
        settings.putString(key(service, ServiceConnector.HANDLE_KEY), handle.trim().removePrefix("@"))

    fun toggleLibrarySync(service: ExternalService, enabled: Boolean) {
        settings.putBoolean(key(service, "librarySync"), enabled)
        refresh(service)
    }

    /** Whether [service] was ever synced: the first sync must go through the preview, never unattended. */
    fun hasSyncedLibrary(service: ExternalService) = trackerSync.base(service).isNotEmpty()

    fun disconnect(service: ExternalService) {
        setConnected(service, false)
        trackerSync.forget(service)
        refresh(service)
        clearSession(service)
    }


    /**
     * Clear the store's WebView cookies so the next connect starts logged out.
     *
     * ponytail: best-effort — reliable on Android (per-URL cookie removal). On JCEF desktop the
     * library's cookie manager is a stub and on the JS browser it can only reach same-origin,
     * non-HttpOnly cookies, so those platforms may stay signed in until the store cookies expire.
     */
    private fun clearSession(service: ExternalService) = viewModelScope.launch {
        connector(service).sessionUrls.forEach { url ->
            runCatching { PlatformCookieManager.removeCookies(url) }
                .onFailure { Logger.w(throwable = it) { "Cookie clear failed for service=$service url=$url" } }
        }
    }


    // --- WebView flows (called by the ServiceSyncView's WebView) ---

    /**
     * Runs [block]; a failed read, library load, write or login aborts it, records [SyncOutcome.Failed] and
     * returns null. Cancellation and any other exception propagate.
     */
    private suspend fun <T> abortOnFailure(what: String, service: ExternalService, block: suspend () -> T): T? = try {
        block()
    } catch (error: ServiceReadException) {
        abort(what, service, error)
    } catch (error: LibraryLoadException) {
        abort(what, service, error)
    } catch (error: ScriptWriteException) {
        abort(what, service, error)
    } catch (error: LoginTimeoutException) {
        abort(what, service, error)
    }

    private fun abort(what: String, service: ExternalService, error: IllegalStateException): Nothing? {
        Logger.e(error) { "$what failed for $service" }
        update { copy(outcome = SyncOutcome.Failed) }
        return null
    }

    suspend fun runConnect(service: ExternalService, session: WebSession) {
        val connector = connector(service)
        abortOnFailure("Connect", service) {
            session.awaitLogin(connector)
            if (session.currentUrl?.let { connector.isLoggedIn(it) } == true) {
                setConnected(service, true)
                fetchProfile(connector, session)?.let { setProfile(service, it) }
                refresh(service)
            }
        }
    }

    /** Re-fetch the profile of an already-connected service (manual refresh from the list). */
    suspend fun runRefreshProfile(service: ExternalService, session: WebSession) {
        val connector = connector(service)
        setBusy(service, true)
        try {
            abortOnFailure("Profile refresh", service) {
                session.awaitLogin(connector)
                fetchProfile(connector, session)?.let { setProfile(service, it) }
            }
        } finally {
            setBusy(service, false)
            refresh(service)
        }
    }

    // Best-effort: a connector with no profile source, or one whose read fails, just leaves the service
    // without a profile. Web vs API is the connector's choice ([ServiceConnector.profile]), not ours.
    private suspend fun fetchProfile(connector: ServiceConnector, session: WebSession): ServiceProfile? =
        connector.profile?.let { session.read(it, SyncPhase.READING_PROFILE) }

    suspend fun runReadForIdSync(service: ExternalService, session: WebSession): List<ExternalGameRef>? {
        val connector = connector(service) as BackloggeryConnector
        return abortOnFailure("ID resync read", service) {
            session.awaitLogin(connector)
            session.read(connector.gamesForIdSync, SyncPhase.READING)
        }
    }

    suspend fun runApplyResyncIds(service: ExternalService, session: WebSession) {
        val connector = connector(service) as BackloggeryConnector
        val refs = ImportHandoff.take(service)
        abortOnFailure("ID resync", service) {
            session.awaitLogin(connector)
            val confirmed = refs.filter { (it.gameId ?: 0) > 0 }
            val updated = session.writeBatches(confirmed, connector::resyncIds) { _, _ -> }
            update { copy(outcome = SyncOutcome.IdsResynced(updated, refs.size - confirmed.size)) }
        }
    }

    suspend fun runWishlistSync(service: ExternalService, session: WebSession) {
        val connector = connector(service)
        setBusy(service, true)
        try {
            abortOnFailure("Wishlist sync", service) {
                session.awaitLogin(connector)
                val wishlist = wishlistRefs(connector, session)
                val result = librarySync.pullWishlist(connector, wishlist)
                val selected = when (service) {
                    ExternalService.BACKLOGGERY -> session.confirmPush(result.toPush)
                    else -> result.toPush
                }
                val (pushed, missed) = pushWishlist(connector, session, selected)
                setLastSyncAt(service, Clock.System.now().toEpochMilliseconds())
                update { copy(outcome = SyncOutcome.WishlistSynced(result.added, pushed, session.leftOut(missed))) }
            }
        } finally {
            setBusy(service, false)
            refresh(service)
        }
    }

    /**
     * Unattended tracker library sync: synced mappings and confident matches only, and only the changes one
     * side made since the last sync — conflicts are counted and left for the reviewed sync.
     */
    suspend fun runLibrarySync(service: ExternalService, session: WebSession) {
        val connector = connector(service)
        val write = connector.statusWrite ?: return
        setBusy(service, true)
        try {
            abortOnFailure("Library sync", service) {
                session.awaitLogin(connector)
                val refs = session.read(connector.ownedGames, SyncPhase.READING)
                val entries = librarySync.allEntries().toMutableList()
                val (remote, onTracker) = trackerSync.resolve(connector, refs)
                val plan = trackerSync.plan(connector, remote, entries, onTracker)
                trackerSync.record(service, plan.agreed)
                val pulled = trackerSync.pull(service, plan.changes.filter { it.winner == SyncSide.REMOTE }, entries, remote)
                val outgoing = trackerSync.outgoing(plan.changes.filter { it.winner == SyncSide.LOCAL }, entries)
                val (pushed, missed) = writeStatuses(service, session, write, outgoing)
                val conflicts = plan.changes.count { it.winner == null }
                setLastSyncAt(service, Clock.System.now().toEpochMilliseconds())
                val outcome = SyncOutcome.LibrarySynced(pulled, pushed, session.leftOut(missed), conflicts)
                update { copy(outcome = outcome) }
            }
        } finally {
            setBusy(service, false)
            refresh(service)
        }
    }

    /**
     * Writes the tracker side of a reviewed library sync, handed over by the preview screen. The plan stays
     * pending until the push succeeds, so a failed write or login can be retried.
     */
    suspend fun runApplyLibrarySync(service: ExternalService, session: WebSession) {
        val connector = connector(service)
        val write = connector.statusWrite ?: return
        abortOnFailure("Library push", service) {
            session.awaitLogin(connector)
            val (pushed, missed) = writeStatuses(service, session, write, trackerSync.pendingWrites[service].orEmpty())
            trackerSync.pendingWrites.remove(service)
            setLastSyncAt(service, Clock.System.now().toEpochMilliseconds())
            val outcome = SyncOutcome.LibrarySynced(pulled = 0, pushed, session.leftOut(missed), conflicts = 0)
            update { copy(outcome = outcome) }
        }
    }

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
            trackerSync.pendingWrites[service]?.let { pending -> trackerSync.pendingWrites[service] = pending.filterNot { it.gameId in done } }
        }
        return written to missed
    }

    /** The [missed] games of this flow's write: with the reason its script reported, else just unconfirmed. */
    private fun WebSession.leftOut(missed: List<String>): List<SkippedWrite> {
        val explained = skips.map { it.name }.toSet()
        return skips + missed.filterNot { it in explained }.map { SkippedWrite(it, SkipReason.UNCONFIRMED) }
    }

    /** The [games] none of the [written] refs confirms, matched on any of their [keys] (IGDB id, store uid, name). */
    private fun <T> unconfirmed(games: List<T>, written: List<ExternalGameRef>, keys: (T) -> List<String>): List<T> {
        val confirmed = written.flatMap { listOfNotNull(it.gameId?.toString(), it.uid, it.name) }
            .filter { it.isNotBlank() }.toSet()
        return games.filterNot { game -> keys(game).any { it in confirmed } }
    }


    /** Reads owned games for the import preview; matching/persisting happen on the preview screen. */
    suspend fun runReadOwned(service: ExternalService, session: WebSession): List<ExternalGameRef>? =
        connector(service).let { connector ->
            abortOnFailure("Owned games read", service) {
                session.awaitLogin(connector)
                session.read(connector.ownedGames, SyncPhase.READING)
            }
        }

    /** Read the incoming wishlist for the editable preview before the outgoing step. */
    suspend fun runWishlistPreview(service: ExternalService, session: WebSession): List<ExternalGameRef>? {
        val connector = connector(service)
        return abortOnFailure("Wishlist read", service) {
            session.awaitLogin(connector)
            wishlistRefs(connector, session)
        }
    }

    /**
     * Review and push backlog games after the incoming wishlist has been confirmed. The reviewed import is
     * only held in memory, so after a screen recreation or process death there is nothing to push: the flow
     * just closes. It is dropped once the push succeeds, so a failed one can be retried.
     */
    suspend fun runWishlistPush(service: ExternalService, session: WebSession) {
        val connector = connector(service)
        val pending = librarySync.pendingWishlistImports[service]
        if (pending == null) {
            Logger.w { "No reviewed wishlist import to push for $service" }
            return
        }
        abortOnFailure("Wishlist push", service) {
            session.awaitLogin(connector)
            // Outgoing preview: let the user pick which backlog games to push to the store, then push them. A store
            // that can't be written to gets no checklist: every row would just read "no match".
            val selected = if (connector.wishlistWrite == null) {
                emptyList()
            } else {
                session.confirmPush(pending.toPush)
            }
            val (pushed, missed) = when {
                selected.isEmpty() -> 0 to emptyList()
                else -> pushWishlist(connector, session, selected)
            }
            librarySync.pendingWishlistImports.remove(service)
            update { copy(outcome = SyncOutcome.WishlistSynced(pending.added, pushed, session.leftOut(missed))) }
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
    ): Pair<Int, List<String>> {
        val write = connector.wishlistWrite ?: return 0 to emptyList()
        // Only games on this platform and not already wishlisted; URL-based strategies also need a store page.
        val onPlatform = games.filter { it.onPlatform && !it.alreadyOnWishlist }
        val withStorePage = onPlatform.filter { it.storeUrl != null }
        val missed = mutableListOf<LibrarySync.OutgoingGame>()
        val pushed = writeWishlist(write, session, onPlatform, withStorePage, missed)
        return pushed to missed.map { it.name }
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

    private fun setBusy(service: ExternalService, busy: Boolean) = update {
        copy(services = services + (service to (services[service] ?: ServiceState()).copy(busy = busy)))
    }

    private fun refresh(service: ExternalService) = update {
        copy(services = services + (service to readServiceState(service)))
    }

    private fun readServiceState(service: ExternalService) = ServiceState(
        connected = isConnected(service),
        wishlistSync = isWishlistSyncEnabled(service),
        librarySync = settings.getBoolean(key(service, "librarySync"), false),
        lastSyncAt = lastSyncAt(service),
        profile = profile(service),
    )

    // --- Per-service link flags (the actual session lives in the WebView cookies) ---

    private fun isConnected(service: ExternalService) = settings.getBoolean(key(service, "connected"), false)
    private fun isWishlistSyncEnabled(service: ExternalService) = settings.getBoolean(key(service, "wishlistSync"), false)
    private fun lastSyncAt(service: ExternalService) = settings.getLong(key(service, "lastSyncAt"), 0L).takeIf { it > 0L }

    private fun profile(service: ExternalService): ServiceProfile? =
        settings.getStringOrNull(key(service, "profile"))
            ?.let { runCatching { json.decodeFromString(ServiceProfile.serializer(), it) }.getOrNull() }

    private fun setProfile(service: ExternalService, profile: ServiceProfile) =
        settings.putString(key(service, "profile"), json.encodeToString(ServiceProfile.serializer(), profile))

    private fun setConnected(service: ExternalService, connected: Boolean) {
        if (connected) {
            settings.putBoolean(key(service, "connected"), true)
        } else {
            // Disconnect: drop every flag for the service.
            settings.remove(key(service, "connected"))

            settings.remove(key(service, "wishlistSync"))
            settings.remove(key(service, "librarySync"))
            settings.remove(key(service, "lastSyncAt"))
            settings.remove(key(service, "profile"))
            settings.remove(key(service, ServiceConnector.HANDLE_KEY))
        }
    }

    private fun setWishlistSync(service: ExternalService, enabled: Boolean) =
        settings.putBoolean(key(service, "wishlistSync"), enabled)

    private fun setLastSyncAt(service: ExternalService, epochMillis: Long) =
        settings.putLong(key(service, "lastSyncAt"), epochMillis)

    private fun key(service: ExternalService, suffix: String) = service.settingsKey(suffix)
}

