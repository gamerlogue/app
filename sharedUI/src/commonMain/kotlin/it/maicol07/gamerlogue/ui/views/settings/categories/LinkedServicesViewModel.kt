package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.parkwoocheol.composewebview.PlatformCookieManager
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.ServiceProfile
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.ui.components.WebSession
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** A store flow that requires the WebView; carried by the [it.maicol07.gamerlogue.NavKeys.ServiceSync] key. */
@Serializable
enum class ServiceSyncAction { CONNECT, REFRESH_PROFILE, SYNC_WISHLIST, PREVIEW_WISHLIST, IMPORT_LIBRARY }

/**
 * State + actions for the Linked Services list.
 *
 * The store flows that need the WebView ([ServiceSyncAction]) run on the [ServiceSyncScreen], which
 * shares this ViewModel type (its own instance) and calls the `run*` suspend functions here; they
 * combine the WebView session (read/write the store) with IGDB matching and Gamerlogue persistence
 * ([LibrarySync]). Library import only reads owned games here — matching and confirmation happen on
 * the preview screen. Per-service link flags are kept in [settings] (the actual session lives in the
 * WebView cookies); [refreshAll] re-reads them when the list returns to the foreground.
 */
@OptIn(ExperimentalTime::class, ExperimentalSettingsApi::class)
@KoinViewModel
class LinkedServicesViewModel(
    private val connectors: Map<ExternalService, ServiceConnector>,
    private val settings: ObservableSettings,
    private val librarySync: LibrarySync,
) : StateViewModel<LinkedServicesViewModel.UiState>(UiState()) {

    data class ServiceState(
        val connected: Boolean = false,
        val wishlistSync: Boolean = false,
        val lastSyncAt: Long? = null,
        val busy: Boolean = false,
        val profile: ServiceProfile? = null,
    )

    data class UiState(
        val services: Map<ExternalService, ServiceState> = emptyMap(),
        val message: String? = null,
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

    fun disconnect(service: ExternalService) {
        setConnected(service, false)
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

    fun consumeMessage() = update { copy(message = null) }

    // --- WebView flows (called by the ServiceSyncScreen's WebView) ---

    suspend fun runConnect(service: ExternalService, session: WebSession) {
        val connector = connector(service)
        session.awaitLogin(connector)
        if (session.currentUrl?.let { connector.isLoggedIn(it) } == true) {
            setConnected(service, true)
            fetchProfile(connector, session)?.let { setProfile(service, it) }
            refresh(service)
        }
    }

    /** Re-fetch the profile of an already-connected service (manual refresh from the list). */
    suspend fun runRefreshProfile(service: ExternalService, session: WebSession) {
        val connector = connector(service)
        setBusy(service, true)
        try {
            session.awaitLogin(connector)
            fetchProfile(connector, session)?.let { setProfile(service, it) }
        } finally {
            setBusy(service, false)
            refresh(service)
        }
    }

    // Best-effort: a connector with no profile source, or one whose read fails, just leaves the service
    // without a profile. Web vs API is the connector's choice ([ServiceConnector.profile]), not ours.
    private suspend fun fetchProfile(connector: ServiceConnector, session: WebSession): ServiceProfile? =
        connector.profile?.let { session.read(it) }

    suspend fun runWishlistSync(service: ExternalService, session: WebSession) {
        val connector = connector(service)
        setBusy(service, true)
        try {
            session.awaitLogin(connector)
            // Some stores keep the wishlist on a different origin than the login (Xbox: Microsoft Store
            // vs login.live.com) — let the user sign into it before any DOM read/write.
            connector.storeLoginUrl?.let { session.awaitStoreLogin(it) }
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
            session.read(connector.ownedGames)
        }

    /**
     * Manual wishlist run: reads the store wishlist, pushes the missing backlog games to the store
     * (automatic, outgoing), and returns the incoming refs for the editable preview (store → backlog).
     */
    suspend fun runWishlistPreview(service: ExternalService, session: WebSession): List<ExternalGameRef> {
        val connector = connector(service)
        session.awaitLogin(connector)
        connector.storeLoginUrl?.let { session.awaitStoreLogin(it) }
        val wishlist = wishlistRefs(connector, session)
        // Outgoing preview: let the user pick which backlog games to push to the store, then push them.
        val selected = session.confirmPush(librarySync.computeWishlistPush(connector, wishlist))
        if (selected.isNotEmpty()) pushWishlist(connector, session, selected)
        return wishlist
    }

    // The connector's [ServiceConnector.wishlist] source decides Web vs API internally; null = no wishlist.
    private suspend fun wishlistRefs(connector: ServiceConnector, session: WebSession): List<ExternalGameRef> =
        connector.wishlist?.let { session.read(it) }.orEmpty()

    private suspend fun pushWishlist(connector: ServiceConnector, session: WebSession, games: List<LibrarySync.OutgoingGame>) {
        val write = connector.wishlistWrite ?: return
        // Only games that release on this platform and aren't already wishlisted; a store page (storeUrl)
        // is required for every strategy except SearchByName, which searches by name instead.
        val onPlatform = games.filter { it.onPlatform && !it.alreadyOnWishlist }
        when (write) {
            is WishlistWrite.Batch -> onPlatform.filter { it.storeUrl != null }.let { pushable ->
                if (pushable.isNotEmpty()) session.run(write.step(pushable.map { ExternalGameRef(it.uid, it.name) }))
            }
            // Per-game write: open each store page and click its add-to-wishlist button (e.g. PSN).
            is WishlistWrite.PerGame -> onPlatform.filter { it.storeUrl != null }
                .forEach { g -> write.step(g.storeUrl!!)?.let { session.run(it) } }
            // Resolve the real product URL from an intermediate page first, then act on it (Nintendo).
            is WishlistWrite.PerGameResolved -> onPlatform.filter { it.storeUrl != null }.forEach { g ->
                val resolved = session.run(write.resolve(g.storeUrl!!)).firstOrNull()?.uid
                if (!resolved.isNullOrBlank()) write.step(resolved)?.let { session.run(it) }
            }
            // Search-driven write: no store URL needed, the game's name drives the search (Ubisoft).
            is WishlistWrite.SearchByName -> onPlatform.forEach { g -> session.run(write.step(g.name)) }
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
            settings.remove(key(service, "lastSyncAt"))
            settings.remove(key(service, "profile"))
        }
    }

    private fun setWishlistSync(service: ExternalService, enabled: Boolean) =
        settings.putBoolean(key(service, "wishlistSync"), enabled)

    private fun setLastSyncAt(service: ExternalService, epochMillis: Long) =
        settings.putLong(key(service, "lastSyncAt"), epochMillis)

    private fun key(service: ExternalService, suffix: String) = "service.${service.name}.$suffix"
}
