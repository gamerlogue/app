package it.maicol07.gamerlogue.services

import co.touchlab.kermit.Logger
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getIntFlow
import it.maicol07.gamerlogue.core.LibraryChange
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.ui.components.LoginTimeoutException
import it.maicol07.gamerlogue.ui.components.WebSession
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

/**
 * Runs the enabled syncs with no one watching and records every sync in the [SyncHistory].
 *
 * While [watch] is collected (the app is open and signed in), it hands the [SyncScheduler] of the platform what to
 * schedule: a library change the user made, to push after [PUSH_DELAY] of quiet (the library sync of every tracker that
 * has it on and, for a backlog game, the wishlist sync of every service that has it on), and the period of the sync of
 * both kinds for all of them ([PERIOD_KEY] hours, default [DEFAULT_PERIOD_HOURS], 0 = off). The scheduler calls
 * [runDue] when one is due. A tracker that never went through the reviewed first sync is left out: nothing is written
 * unattended before it. Background runs go one at a time through the [BackgroundWebHost]; a service never runs two
 * syncs at once.
 */
@Single
class SyncCoordinator(
    private val settings: ObservableSettings,
    private val runner: SyncRunner,
    private val history: SyncHistory,
    private val host: BackgroundWebHost,
    private val libraryChanges: LibraryChanges,
) {
    private val locks = ExternalService.entries.associateWith { Mutex() }
    private val background = Mutex()

    /** The services syncing right now. */
    val running: StateFlow<Set<ExternalService>>
        field = MutableStateFlow(emptySet())

    /** The background syncs asked for and not ended yet; empty when none is. */
    val queue: StateFlow<SyncQueue>
        field = MutableStateFlow(SyncQueue())

    /** A sync the user started on the sync screen, recorded as already seen. Null when it failed or can't run. */
    suspend fun runManual(service: ExternalService, kind: SyncKind, session: WebSession): SyncOutcome? =
        run(service, kind, SyncTrigger.MANUAL, session).first

    /**
     * Removes [game] from the [kind] side of [service] (its library or wishlist), as the user removed it from
     * Gamerlogue and confirmed, through the background session like the syncs; settled in the history once the
     * service confirmed it. Returns whether it did.
     */
    suspend fun removeFromService(service: ExternalService, kind: SyncKind, game: SyncGame): Boolean {
        val removed = try {
            background.withLock {
                locks.getValue(service).withLock {
                    running.update { it + service }
                    try {
                        withTimeout(RUN_TIMEOUT) {
                            host.withSession {
                                runner.removeFromService(service, kind, UnattendedSession(it), listOf(game))
                            }
                        }
                    } finally {
                        running.update { it - service }
                    }
                }
            }
        } catch (error: LoginRequiredException) {
            Logger.e(error) { "Removal from $service failed" }
            emptySet()
        } catch (error: ScriptWriteException) {
            Logger.e(error) { "Removal from $service failed" }
            emptySet()
        } catch (error: TimeoutCancellationException) {
            Logger.e(error) { "Removal from $service failed" }
            emptySet()
        }
        if (game.gameId in removed) history.settleRemoved(service, kind, game.gameId, keep = false)
        return game.gameId in removed
    }

    /** Hands [scheduler] the library changes to push and the period to sync at, until cancelled. */
    @OptIn(ExperimentalSettingsApi::class)
    suspend fun watch(scheduler: SyncScheduler) {
        if (!isServiceSyncSupported) return
        try {
            watchChanges(scheduler)
        } finally {
            scheduler.stop()
        }
    }

    @OptIn(ExperimentalSettingsApi::class)
    private suspend fun watchChanges(scheduler: SyncScheduler) {
        coroutineScope {
            launch {
                libraryChanges.changes.filterIsInstance<LibraryChange.Saved>()
                    .collect { scheduler.changed(wishlist = it.status == GameLibraryStatus.BACKLOG) }
            }
            launch {
                settings.getIntFlow(PERIOD_KEY, DEFAULT_PERIOD_HOURS).collect { scheduler.period(it) }
            }
        }
    }

    /**
     * Runs the syncs due for [trigger], one after the other in a single session: the library syncs, plus the
     * wishlist ones when [wishlist]. They wait in the [queue] from the call on. [onProgress] hears of each service
     * before it starts (its index among [total], counted over the whole [queue]). Returns the runs it recorded.
     */
    suspend fun runDue(
        trigger: SyncTrigger,
        wishlist: Boolean,
        onProgress: suspend (service: ExternalService, index: Int, total: Int) -> Unit = { _, _, _ -> },
    ): List<SyncRun> {
        val due = ExternalService.entries.filter { isServiceSyncSupported && it.flag(CONNECTED) }.flatMap { service ->
            listOfNotNull(
                SyncKind.LIBRARY.takeIf { service.flag(LIBRARY_SYNC) },
                SyncKind.WISHLIST.takeIf { wishlist && service.flag(WISHLIST_SYNC) },
            ).filter { runner.runsUnattended(service, it) }.map { DueSync(service, it) }
        }
        if (due.isEmpty()) return emptyList()
        queue.update { it.copy(waiting = it.waiting + due) }
        var started = 0
        try {
            return background.withLock {
                try {
                    // Current from here, so the session starting up already shows as its run.
                    queue.update { it.next(due.first()) }
                    started = 1
                    host.withSession { session ->
                        due.mapIndexedNotNull { i, sync ->
                            if (i > 0) {
                                queue.update { it.next(sync) }
                                started++
                            }
                            val state = queue.value
                            onProgress(sync.service, state.done, state.total)
                            run(sync.service, sync.kind, trigger, UnattendedSession(session)).second
                        }
                    }
                } finally {
                    queue.update { it.next(null) }
                }
            }
        } finally {
            // What a failure or cancellation left waiting.
            queue.update { state -> state.copy(waiting = due.drop(started).fold(state.waiting) { w, s -> w - s }).settled() }
        }
    }

    private fun ExternalService.flag(name: String) = settings.getBoolean(settingsKey(name), false)

    /** Runs one sync and records it; the outcome is null when it failed or can't run. */
    private suspend fun run(
        service: ExternalService,
        kind: SyncKind,
        trigger: SyncTrigger,
        session: WebSession,
    ): Pair<SyncOutcome?, SyncRun?> =
        locks.getValue(service).withLock {
            running.update { it + service }
            val record = RunRecord(service, kind, trigger)
            try {
                val sync = suspend {
                    when (kind) {
                        SyncKind.WISHLIST -> runner.syncWishlist(service, session)
                        SyncKind.LIBRARY -> runner.syncLibrary(service, session)
                    }
                }
                // ponytail: one bound for a stuck script or page nobody watches; a background job gets about this
                // long anyway. A manual run has the user there to leave it.
                val outcome = if (trigger == SyncTrigger.MANUAL) sync() else withTimeout(RUN_TIMEOUT) { sync() }
                outcome to outcome?.let { history.add(record.finished(it)) }
            } catch (error: LoginRequiredException) {
                record.failed(SyncResult.LOGIN_REQUIRED, error)
            } catch (error: LoginTimeoutException) {
                record.failed(SyncResult.LOGIN_REQUIRED, error)
            } catch (error: ServiceReadException) {
                record.failed(SyncResult.FAILED, error)
            } catch (error: LibraryLoadException) {
                record.failed(SyncResult.FAILED, error)
            } catch (error: ScriptWriteException) {
                record.failed(SyncResult.FAILED, error)
            } catch (error: TimeoutCancellationException) {
                record.failed(SyncResult.FAILED, error)
            } finally {
                running.update { it - service }
            }
        }

    /** The start of a run, turned into its [SyncRun] when it ends. */
    private inner class RunRecord(val service: ExternalService, val kind: SyncKind, val trigger: SyncTrigger) {
        private val startedAt = Clock.System.now().toEpochMilliseconds()

        fun finished(outcome: SyncOutcome): SyncRun = when (outcome) {
            is SyncOutcome.WishlistSynced -> run(
                if (outcome.skipped.isEmpty()) SyncResult.OK else SyncResult.PARTIAL,
            ).copy(
                added = outcome.added,
                pushed = outcome.pushed,
                skipped = outcome.skipped,
                removedOnService = outcome.removedOnService,
                received = outcome.received,
                sent = outcome.sent,
                toConfirm = outcome.toConfirm,
                removedHere = outcome.removedHere,
            )
            is SyncOutcome.LibrarySynced -> run(
                if (outcome.skipped.isEmpty() && outcome.conflicts == 0) SyncResult.OK else SyncResult.PARTIAL,
            ).copy(
                added = outcome.pulled,
                pushed = outcome.pushed,
                skipped = outcome.skipped,
                conflicts = outcome.conflicts,
                removedOnService = outcome.removedOnService,
                received = outcome.received,
                sent = outcome.sent,
                toConfirm = outcome.toConfirm,
                removedHere = outcome.removedHere,
            )
            else -> run(SyncResult.OK)
        }

        /** Records the failure: the run's caller gets no outcome, only the recorded run. */
        fun failed(result: SyncResult, error: Exception): Pair<SyncOutcome?, SyncRun?> {
            Logger.e(error) { "$kind sync of $service failed" }
            return null to history.add(run(result).copy(error = error.message))
        }

        private fun run(result: SyncResult) = SyncRun(
            id = Uuid.random().toString(),
            service = service,
            kind = kind,
            trigger = trigger,
            startedAt = startedAt,
            endedAt = Clock.System.now().toEpochMilliseconds(),
            result = result,
            seen = trigger == SyncTrigger.MANUAL,
        )
    }

    companion object {
        const val CONNECTED = "connected"
        const val WISHLIST_SYNC = "wishlistSync"
        const val LIBRARY_SYNC = "librarySync"

        /** Hours between periodic syncs; 0 turns them off. */
        const val PERIOD_KEY = "sync.periodHours"
        const val DEFAULT_PERIOD_HOURS = 6

        /** When the last periodic sync started, for a scheduler that keeps the period itself. */
        const val LAST_PERIODIC_KEY = "sync.lastPeriodicAt"

        /** How long a library change waits for the next one before it's pushed. */
        val PUSH_DELAY = 90.seconds
        private val RUN_TIMEOUT = 10.minutes
    }
}

/** One sync of [SyncCoordinator.runDue]. */
data class DueSync(val service: ExternalService, val kind: SyncKind)

/**
 * The background syncs: the [current] one (its session starting up included), the ones [waiting] after it in order,
 * and how many are [done] since the queue was last empty, to count them as X of [total].
 */
data class SyncQueue(val current: DueSync? = null, val waiting: List<DueSync> = emptyList(), val done: Int = 0) {
    val total: Int get() = done + waiting.size + if (current != null) 1 else 0
    val isEmpty: Boolean get() = current == null && waiting.isEmpty()

    /** Ends the current sync, if any, and starts [sync], taken from the waiting ones; null when none follows yet. */
    internal fun next(sync: DueSync?): SyncQueue = copy(
        current = sync,
        waiting = if (sync == null) waiting else waiting - sync,
        done = done + if (current != null) 1 else 0,
    ).settled()

    /** Starts the count over once nothing is left. */
    internal fun settled(): SyncQueue = if (isEmpty) SyncQueue() else this
}
