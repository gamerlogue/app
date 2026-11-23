package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.parkwoocheol.composewebview.PlatformCookieManager
import com.russhwolf.settings.ObservableSettings
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.IgdbUnavailableException
import it.maicol07.gamerlogue.services.LibraryLoadException
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ScriptWriteException
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.ServiceProfile
import it.maicol07.gamerlogue.services.ServiceReadException
import it.maicol07.gamerlogue.services.SyncCoordinator
import it.maicol07.gamerlogue.services.SyncKind
import it.maicol07.gamerlogue.services.SyncOutcome
import it.maicol07.gamerlogue.services.SyncRunner
import it.maicol07.gamerlogue.services.TrackerSync
import it.maicol07.gamerlogue.services.connectors.BackloggeryConnector
import it.maicol07.gamerlogue.services.settingsKey
import it.maicol07.gamerlogue.ui.components.LoginTimeoutException
import it.maicol07.gamerlogue.ui.components.SyncPhase
import it.maicol07.gamerlogue.ui.components.WebSession
import it.maicol07.gamerlogue.ui.components.writeBatches
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.KoinViewModel

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
    private val runner: SyncRunner,
    private val coordinator: SyncCoordinator,
) : StateViewModel<LinkedServicesViewModel.UiState>(UiState()) {


    data class ServiceState(
        val connected: Boolean = false,
        val wishlistSync: Boolean = false,
        val librarySync: Boolean = false,
        val lastSyncAt: Long? = null,
        val busy: Boolean = false,
        val profile: ServiceProfile? = null,
    )

    data class UiState(
        val services: Map<ExternalService, ServiceState> = emptyMap(),
        val outcome: SyncOutcome? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    init {
        refreshAll()
    }

    /** Hours between the periodic syncs, 0 = off (see [SyncCoordinator]); changed only here. */
    val syncPeriodHours: StateFlow<Int>
        field = MutableStateFlow(settings.getInt(SyncCoordinator.PERIOD_KEY, SyncCoordinator.DEFAULT_PERIOD_HOURS))

    fun setSyncPeriodHours(hours: Int) {
        settings.putInt(SyncCoordinator.PERIOD_KEY, hours)
        syncPeriodHours.value = hours
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
        settings.putBoolean(key(service, SyncCoordinator.LIBRARY_SYNC), enabled)
        refresh(service)
    }

    /** Whether [service] was ever synced: the first sync must go through the preview, never unattended. */
    suspend fun hasSyncedLibrary(service: ExternalService) = trackerSync.base(service).isNotEmpty()

    fun disconnect(service: ExternalService) {
        setConnected(service, false)
        viewModelScope.launch {
            trackerSync.forget(service)
            librarySync.wishlistState.forget(service)
        }
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
     * Runs [block]; a failed read, library load, IGDB search, write or login aborts it, records [SyncOutcome.Failed] and
     * returns null. Cancellation and any other exception propagate.
     */
    private suspend fun <T> abortOnFailure(what: String, service: ExternalService, block: suspend () -> T): T? = try {
        block()
    } catch (error: ServiceReadException) {
        abort(what, service, error)
    } catch (error: LibraryLoadException) {
        abort(what, service, error)
    } catch (error: IgdbUnavailableException) {
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

    suspend fun runWishlistSync(service: ExternalService, session: WebSession) =
        runManual(service, SyncKind.WISHLIST, session)

    suspend fun runLibrarySync(service: ExternalService, session: WebSession) =
        runManual(service, SyncKind.LIBRARY, session)

    /** A sync the [coordinator] records in the history; a failure ends in [SyncOutcome.Failed]. */
    private suspend fun runManual(service: ExternalService, kind: SyncKind, session: WebSession) {
        setBusy(service, true)
        try {
            val outcome = coordinator.runManual(service, kind, session) ?: SyncOutcome.Failed
            update { copy(outcome = outcome) }
        } finally {
            setBusy(service, false)
            refresh(service)
        }
    }

    /** Writes the tracker side of a reviewed library sync, handed over by the preview screen. */
    suspend fun runApplyLibrarySync(service: ExternalService, session: WebSession) {
        abortOnFailure("Library push", service) {
            runner.applyReviewedLibrary(service, session)?.let { update { copy(outcome = it) } }
        }
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
            connector.wishlist?.let { session.read(it, SyncPhase.READING) }.orEmpty()
        }
    }

    /** Review and push backlog games after the incoming wishlist has been confirmed (see [SyncRunner.pushReviewedWishlist]). */
    suspend fun runWishlistPush(service: ExternalService, session: WebSession) {
        abortOnFailure("Wishlist push", service) {
            runner.pushReviewedWishlist(service, session)?.let { update { copy(outcome = it) } }
        }
    }

    private fun setBusy(service: ExternalService, busy: Boolean) = update {
        copy(services = services + (service to (services[service] ?: ServiceState()).copy(busy = busy)))
    }

    private fun refresh(service: ExternalService) = update {
        copy(services = services + (service to readServiceState(service)))
    }

    private fun readServiceState(service: ExternalService) = ServiceState(
        connected = isConnected(service),
        wishlistSync = isWishlistSyncEnabled(service),
        librarySync = settings.getBoolean(key(service, SyncCoordinator.LIBRARY_SYNC), false),
        lastSyncAt = lastSyncAt(service),
        profile = profile(service),
    )

    // --- Per-service link flags (the actual session lives in the WebView cookies) ---

    private fun isConnected(service: ExternalService) =
        settings.getBoolean(key(service, SyncCoordinator.CONNECTED), false)
    private fun isWishlistSyncEnabled(service: ExternalService) =
        settings.getBoolean(key(service, SyncCoordinator.WISHLIST_SYNC), false)
    private fun lastSyncAt(service: ExternalService) =
        settings.getLong(key(service, SyncRunner.LAST_SYNC_AT), 0L).takeIf { it > 0L }

    private fun profile(service: ExternalService): ServiceProfile? =
        settings.getStringOrNull(key(service, "profile"))
            ?.let { runCatching { json.decodeFromString(ServiceProfile.serializer(), it) }.getOrNull() }

    private fun setProfile(service: ExternalService, profile: ServiceProfile) =
        settings.putString(key(service, "profile"), json.encodeToString(ServiceProfile.serializer(), profile))

    private fun setConnected(service: ExternalService, connected: Boolean) {
        if (connected) {
            settings.putBoolean(key(service, SyncCoordinator.CONNECTED), true)
        } else {
            // Disconnect: drop every flag for the service.
            settings.remove(key(service, SyncCoordinator.CONNECTED))

            settings.remove(key(service, SyncCoordinator.WISHLIST_SYNC))
            settings.remove(key(service, SyncCoordinator.LIBRARY_SYNC))
            settings.remove(key(service, SyncRunner.LAST_SYNC_AT))
            settings.remove(key(service, "profile"))
            settings.remove(key(service, ServiceConnector.HANDLE_KEY))
        }
    }

    private fun setWishlistSync(service: ExternalService, enabled: Boolean) =
        settings.putBoolean(key(service, SyncCoordinator.WISHLIST_SYNC), enabled)

    private fun key(service: ExternalService, suffix: String) = service.settingsKey(suffix)
}

