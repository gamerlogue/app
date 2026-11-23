package it.maicol07.gamerlogue.services

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getLongFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/** When the background syncs run: told by [SyncCoordinator.watch], it calls [SyncCoordinator.runDue] when one is due. */
interface SyncScheduler {
    /** The user changed the library: push it after [SyncCoordinator.PUSH_DELAY] of quiet, the wishlists too if [wishlist]. */
    fun changed(wishlist: Boolean)

    /** Sync everything every [hours]; 0 = never. */
    fun period(hours: Int)

    /** Runs every sync that runs unattended now, as the user asked, even with the app left. */
    fun runNow()

    /** When the next periodic sync is due (epoch millis), as the platform plans it; null when there's none. */
    val nextPeriodicAt: Flow<Long?>

    /**
     * When the library changes waiting to be pushed are due (epoch millis); null when none waits. Past it, the push
     * waits for the platform to run it (Android: network, battery saving).
     */
    val pendingPushAt: Flow<Long?>

    /** [SyncCoordinator.watch] stopped (the app went away or the user signed out): drop what lives only in this process. */
    fun stop() = Unit
}

/**
 * Runs the background syncs in this process, so only while the app runs: the desktop, which keeps running in the tray.
 * A run started here finishes even if the screen that triggered it goes away.
 */
class InProcessSyncScheduler(
    private val coordinator: SyncCoordinator,
    private val settings: ObservableSettings,
) : SyncScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var push: Job? = null
    private var periodic: Job? = null
    private var wishlistDue = false

    override val pendingPushAt: StateFlow<Long?>
        field = MutableStateFlow(null)

    override fun changed(wishlist: Boolean) {
        wishlistDue = wishlistDue || wishlist
        push?.cancel()
        pendingPushAt.value = (Clock.System.now() + SyncCoordinator.PUSH_DELAY).toEpochMilliseconds()
        push = scope.launch {
            delay(SyncCoordinator.PUSH_DELAY)
            pendingPushAt.value = null
            val withWishlist = wishlistDue
            wishlistDue = false
            // Its own job: a later change restarts the wait, never a run already going.
            scope.launch { coordinator.runDue(SyncTrigger.CHANGE, withWishlist) }
        }
    }

    override fun runNow() {
        scope.launch { coordinator.runDue(SyncTrigger.MANUAL, wishlist = true) }
    }

    @OptIn(ExperimentalSettingsApi::class)
    override val nextPeriodicAt: Flow<Long?> = combine(
        settings.getLongFlow(SyncCoordinator.LAST_PERIODIC_KEY, 0L),
        settings.getIntFlow(SyncCoordinator.PERIOD_KEY, SyncCoordinator.DEFAULT_PERIOD_HOURS),
    ) { last, hours -> if (hours <= 0) null else (last.milliseconds + hours.hours).inWholeMilliseconds }

    // A change waiting to be pushed still goes: the app is still running.
    override fun stop() {
        periodic?.cancel()
    }

    override fun period(hours: Int) {
        periodic?.cancel()
        if (hours <= 0) return
        periodic = scope.launch {
            while (true) {
                val next = settings.getLong(SyncCoordinator.LAST_PERIODIC_KEY, 0L).milliseconds + hours.hours
                delay(next - Clock.System.now().toEpochMilliseconds().milliseconds)
                settings.putLong(SyncCoordinator.LAST_PERIODIC_KEY, Clock.System.now().toEpochMilliseconds())
                // Its own job, joined: stopping the period leaves a run already going to finish.
                scope.launch { coordinator.runDue(SyncTrigger.PERIODIC, wishlist = true) }.join()
            }
        }
    }
}
