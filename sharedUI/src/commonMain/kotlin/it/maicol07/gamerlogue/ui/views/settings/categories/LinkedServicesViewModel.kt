package it.maicol07.gamerlogue.ui.views.settings.categories

import co.touchlab.kermit.Logger
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.ui.components.WebSession
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * State + actions for the Linked Services screen.
 *
 * The screen renders the WebView while [UiState.action] is non-null; its flow lambda calls the `run*`
 * suspend functions here, which combine the WebView session (read/write the store) with IGDB matching
 * and Gamerlogue persistence ([LibrarySync]). Library import only reads owned games here — matching
 * and confirmation happen on the preview screen. Per-service link flags are kept in [settings] (the
 * actual session lives in the WebView cookies).
 */
@OptIn(ExperimentalTime::class, ExperimentalSettingsApi::class)
class LinkedServicesViewModel(
    private val connectors: Map<ExternalService, ServiceConnector>,
    private val settings: ObservableSettings,
    private val librarySync: LibrarySync,
) : StateViewModel<LinkedServicesViewModel.UiState>(UiState()) {

    /** A flow that requires the WebView, for [service]. */
    sealed interface Action {
        val service: ExternalService
        data class Connect(override val service: ExternalService) : Action
        data class SyncWishlist(override val service: ExternalService) : Action
        data class PreviewWishlist(override val service: ExternalService) : Action
        data class ImportLibrary(override val service: ExternalService) : Action
    }

    data class ServiceState(
        val connected: Boolean = false,
        val wishlistSync: Boolean = false,
        val lastSyncAt: Long? = null,
        val busy: Boolean = false,
    )

    data class UiState(
        val services: Map<ExternalService, ServiceState> = emptyMap(),
        val action: Action? = null,
        val message: String? = null,
    )

    init {
        update {
            copy(services = ExternalService.entries.associateWith { readServiceState(it) })
        }
    }

    fun connector(service: ExternalService): ServiceConnector = connectors.getValue(service)

    // --- UI intents (set the active WebView action; the screen renders it) ---

    fun connect(service: ExternalService) = update { copy(action = Action.Connect(service)) }

    fun syncWishlistNow(service: ExternalService) {
        if (isConnected(service)) update { copy(action = Action.SyncWishlist(service)) }
    }

    /** Manual wishlist sync with preview (vs the toggle's automatic background sync). */
    fun previewWishlist(service: ExternalService) {
        if (isConnected(service)) update { copy(action = Action.PreviewWishlist(service)) }
    }

    fun importLibrary(service: ExternalService) {
        if (isConnected(service)) update { copy(action = Action.ImportLibrary(service)) }
    }

    fun toggleWishlistSync(service: ExternalService, enabled: Boolean) {
        setWishlistSync(service, enabled)
        refresh(service)
        if (enabled && isConnected(service)) syncWishlistNow(service)
    }

    fun disconnect(service: ExternalService) {
        setConnected(service, false)
        refresh(service)
    }

    fun clearAction() = update { copy(action = null) }

    fun consumeMessage() = update { copy(message = null) }

    // --- WebView flows (called by the screen's ServiceWebView) ---

    suspend fun runConnect(service: ExternalService, session: WebSession) {
        session.awaitLogin(connector(service))
        if (session.currentUrl?.let { connector(service).isLoggedIn(it) } == true) {
            setConnected(service, true)
            refresh(service)
        }
    }

    suspend fun runWishlistSync(service: ExternalService, session: WebSession) {
        val connector = connector(service)
        setBusy(service, true)
        try {
            session.awaitLogin(connector)
            val wishlist = wishlistRefs(connector, session)
            val result = librarySync.pullWishlist(connector, wishlist)
            if (result.toPush.isNotEmpty()) pushWishlist(connector, session, result.toPush)
            setLastSyncAt(service, Clock.System.now().toEpochMilliseconds())
            update { copy(message = "wishlist:${result.added}:${result.toPush.size}") }
        } catch (e: Exception) {
            Logger.e(e) { "Wishlist sync failed for $service" }
            update { copy(message = "error") }
        } finally {
            setBusy(service, false)
            refresh(service)
        }
    }

    /** Reads owned games for the import preview; matching/persisting happen on the preview screen. */
    suspend fun runReadOwned(service: ExternalService, session: WebSession): List<ExternalGameRef> =
        connector(service).let { connector ->
            session.awaitLogin(connector)
            ownedRefs(connector, session)
        }

    /**
     * Manual wishlist run: reads the store wishlist, pushes the missing backlog games to the store
     * (automatic, outgoing), and returns the incoming refs for the editable preview (store → backlog).
     */
    suspend fun runWishlistPreview(service: ExternalService, session: WebSession): List<ExternalGameRef> {
        val connector = connector(service)
        session.awaitLogin(connector)
        val wishlist = wishlistRefs(connector, session)
        // Outgoing preview: let the user pick which backlog games to push to the store, then push them.
        val selected = session.confirmPush(librarySync.computeWishlistPush(connector, wishlist))
        if (selected.isNotEmpty()) pushWishlist(connector, session, selected)
        return wishlist
    }

    // Each operation independently uses the API path (Kotlin, with a WebView-obtained credential) or
    // the JS path (a WebView script). PSN, e.g., reads owned games via API but the wishlist via JS.
    private suspend fun ownedRefs(connector: ServiceConnector, session: WebSession): List<ExternalGameRef> =
        if (connector.ownedViaApi()) connector.apiOwned(credential(connector, session))
        else session.run(connector.readOwned())

    private suspend fun wishlistRefs(connector: ServiceConnector, session: WebSession): List<ExternalGameRef> =
        if (connector.wishlistViaApi()) connector.apiWishlist(credential(connector, session))
        else session.run(connector.readWishlist())

    private suspend fun pushWishlist(connector: ServiceConnector, session: WebSession, games: List<LibrarySync.OutgoingGame>) {
        // Only games that release on this platform, have a store page, and aren't already wishlisted.
        val pushable = games.filter { it.onPlatform && it.storeUrl != null && !it.alreadyOnWishlist }
        if (pushable.isEmpty()) return
        when {
            connector.wishlistViaApi() ->
                connector.apiAddToWishlist(credential(connector, session), pushable.map { ExternalGameRef(it.uid, it.name) })
            // Per-game write: open each store page and click its add-to-wishlist button (e.g. PSN).
            connector.pushesPerGame() ->
                pushable.forEach { g -> connector.wishlistPushStep(g.storeUrl!!)?.let { step -> session.run(step) } }
            else -> session.run(connector.addToWishlist(pushable.map { ExternalGameRef(it.uid, it.name) }))
        }
    }

    private suspend fun credential(connector: ServiceConnector, session: WebSession): String =
        connector.credentialStep()?.let { session.run(it).firstOrNull()?.uid.orEmpty() }.orEmpty()

    private fun setBusy(service: ExternalService, busy: Boolean) = update {
        copy(services = services + (service to (services[service] ?: ServiceState()).copy(busy = busy)))
    }

    private fun refresh(service: ExternalService) = update {
        copy(services = services + (service to readServiceState(service)))
    }

    private fun readServiceState(service: ExternalService) = ServiceState(
        connected = isConnected(service),
        wishlistSync = isWishlistSyncEnabled(service),
        lastSyncAt = lastSyncAt(service),
    )

    // --- Per-service link flags (the actual session lives in the WebView cookies) ---

    private fun isConnected(service: ExternalService) = settings.getBoolean(key(service, "connected"), false)
    private fun isWishlistSyncEnabled(service: ExternalService) = settings.getBoolean(key(service, "wishlistSync"), false)
    private fun lastSyncAt(service: ExternalService) = settings.getLong(key(service, "lastSyncAt"), 0L).takeIf { it > 0L }

    private fun setConnected(service: ExternalService, connected: Boolean) {
        if (connected) {
            settings.putBoolean(key(service, "connected"), true)
        } else {
            // Disconnect: drop every flag for the service.
            settings.remove(key(service, "connected"))
            settings.remove(key(service, "wishlistSync"))
            settings.remove(key(service, "lastSyncAt"))
        }
    }

    private fun setWishlistSync(service: ExternalService, enabled: Boolean) =
        settings.putBoolean(key(service, "wishlistSync"), enabled)

    private fun setLastSyncAt(service: ExternalService, epochMillis: Long) =
        settings.putLong(key(service, "lastSyncAt"), epochMillis)

    private fun key(service: ExternalService, suffix: String) = "service.${service.name}.$suffix"
}
