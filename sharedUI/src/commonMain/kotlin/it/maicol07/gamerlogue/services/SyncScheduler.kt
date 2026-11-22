package it.maicol07.gamerlogue.services

import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    override fun changed(wishlist: Boolean) {
        wishlistDue = wishlistDue || wishlist
        push?.cancel()
        push = scope.launch {
            delay(SyncCoordinator.PUSH_DELAY)
            val withWishlist = wishlistDue
            wishlistDue = false
            // Its own job: a later change restarts the wait, never a run already going.
            scope.launch { coordinator.runDue(SyncTrigger.CHANGE, withWishlist) }
        }
    }

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
