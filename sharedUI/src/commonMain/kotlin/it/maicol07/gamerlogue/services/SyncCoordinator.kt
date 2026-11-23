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
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.koin.core.annotation.Single
import kotlin.coroutines.cancellation.CancellationException
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
// Runs the syncs and holds their queue, with the controls on it.
@Suppress("TooManyFunctions")
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

    private val currentJob = MutableStateFlow<Job?>(null)

    // The manual syncs and the removals going on, by service, for cancelManual.
    private val manualJobs = MutableStateFlow(emptyMap<Job, ExternalService>())

    // The ids of the syncs cancelCurrent stopped, until their run ends.
    private val cancelled = MutableStateFlow(emptySet<String>())

    /**
     * A sync the user started on the sync screen, recorded as already seen. Null when it failed, can't run or the user
     * cancelled it ([cancelManual]).
     */
    suspend fun runManual(service: ExternalService, kind: SyncKind, session: WebSession): SyncOutcome? =
        cancellableBy(service) { run(service, kind, SyncTrigger.MANUAL, session).first }

    /**
     * Removes [game] from the [kind] side of [service] (its library or wishlist), as the user removed it from
     * Gamerlogue and confirmed, through the background session like the syncs; settled in the history once the
     * service confirmed it. Returns whether it did; null when the user cancelled it ([cancelManual]).
     */
    suspend fun removeFromService(service: ExternalService, kind: SyncKind, game: SyncGame): Boolean? {
        val removed = cancellableBy(service) { removeThere(service, kind, game) } ?: return null
        if (game.gameId in removed) history.settleRemoved(service, kind, game.gameId, keep = false)
        return game.gameId in removed
    }

    /** Stops the manual syncs and the removals of [service], like [cancelCurrent] does a background one. */
    fun cancelManual(service: ExternalService) =
        manualJobs.value.filterValues { it == service }.keys.forEach { it.cancel(UserCancellation()) }

    /** Runs [block] so that [cancelManual] on [service] stops it: it returns null then. */
    private suspend fun <T : Any> cancellableBy(service: ExternalService, block: suspend () -> T?): T? =
        coroutineScope {
            val job = async { block() }
            manualJobs.update { it + (job to service) }
            try {
                job.await()
            } catch (@Suppress("SwallowedException") error: CancellationException) {
                // Rethrown when this coroutine is cancelled too, not just the job.
                ensureActive()
                null
            } finally {
                manualJobs.update { it - job }
            }
        }

    /** The removal of [removeFromService]: the ids the service confirmed removed, none when it failed. */
    private suspend fun removeThere(service: ExternalService, kind: SyncKind, game: SyncGame): Set<Int> =
        try {
            background.withLock {
                locks.getValue(service).withLock {
                    running.update { it + service }
                    try {
                        withTimeout(RUN_TIMEOUT) {
                            host.withSession {
                                // Not in the queue, so no step to show.
                                val session = UnattendedSession(it) {}
                                runner.removeFromService(service, kind, session, listOf(game))
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
                    .collect { schedulePush(scheduler, wishlist = it.status == GameLibraryStatus.BACKLOG) }
            }
            launch {
                settings.getIntFlow(PERIOD_KEY, DEFAULT_PERIOD_HOURS).collect { scheduler.period(it) }
            }
        }
    }

    /** Hands [scheduler] a change to push, the wishlists too if [wishlist], when a service will take it. */
    fun schedulePush(scheduler: SyncScheduler, wishlist: Boolean) {
        if (dueSyncs(wishlist).isNotEmpty()) scheduler.changed(wishlist)
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
        val due = dueSyncs(wishlist)
        if (due.isEmpty()) return emptyList()
        queue.update { it.copy(waiting = it.waiting + due) }
        try {
            return background.withLock { runQueued(due, trigger, onProgress) }
        } finally {
            // What a failure, a cancellation or a skip left waiting.
            val ids = due.mapTo(mutableSetOf()) { it.id }
            queue.update { state -> state.copy(waiting = state.waiting.filterNot { it.id in ids }).settled() }
        }
    }

    /** The syncs that run unattended: the library ones, plus the wishlist ones when [wishlist]. */
    private fun dueSyncs(wishlist: Boolean): List<DueSync> =
        ExternalService.entries.filter { isServiceSyncSupported && it.flag(CONNECTED) }.flatMap { service ->
            listOfNotNull(
                SyncKind.LIBRARY.takeIf { service.flag(LIBRARY_SYNC) },
                SyncKind.WISHLIST.takeIf { wishlist && service.flag(WISHLIST_SYNC) },
            ).filter { runner.runsUnattended(service, it) }.map { DueSync(service, it) }
        }

    /** Stops the current background sync, recorded as cancelled; the next one in the [queue] starts. */
    fun cancelCurrent() {
        val id = queue.value.current?.id ?: return
        cancelled.update { it + id }
        currentJob.value?.cancel(UserCancellation())
    }

    /** Takes [sync] out of the [queue] before it starts. */
    fun skip(sync: DueSync) = queue.update { state -> state.copy(waiting = state.waiting - sync).settled() }

    /** Empties the [queue] and stops the current background sync. */
    fun cancelAll() {
        queue.update { it.copy(waiting = emptyList()).settled() }
        cancelCurrent()
    }

    /** The [due] syncs still in the [queue] (none [skip]ped them), in one session; holds the background lock. */
    private suspend fun runQueued(
        due: List<DueSync>,
        trigger: SyncTrigger,
        onProgress: suspend (service: ExternalService, index: Int, total: Int) -> Unit,
    ): List<SyncRun> {
        // Current from here, so the session starting up already shows as its run.
        val first = due.firstOrNull { take(it) } ?: return emptyList()
        try {
            return host.withSession { session ->
                due.dropWhile { it != first }.mapNotNull { sync ->
                    if (sync != first && !take(sync)) return@mapNotNull null
                    val state = queue.value
                    onProgress(sync.service, state.done, state.total)
                    runCancellable(sync, trigger, session)
                }
            }
        } finally {
            queue.update { it.next(null) }
        }
    }

    /** Makes [sync] the current one, if it's still waiting. */
    private fun take(sync: DueSync): Boolean =
        queue.updateAndGet { state -> if (sync in state.waiting) state.next(sync) else state }.current == sync

    /** Runs [sync] so that [cancelCurrent] stops it alone: null then, and the caller goes on with the next one. */
    private suspend fun runCancellable(sync: DueSync, trigger: SyncTrigger, session: WebSession): SyncRun? =
        coroutineScope {
            val job = async {
                val unattended = UnattendedSession(session) { step -> queue.update { it.copy(step = step) } }
                run(sync.service, sync.kind, trigger, unattended).second
            }
            currentJob.value = job
            // Cancelled while its session started up, before there was a job to cancel.
            if (sync.id in cancelled.value) job.cancel(UserCancellation())
            try {
                job.await()
            } catch (@Suppress("SwallowedException") error: CancellationException) {
                // Rethrown when this coroutine is cancelled too, not just the sync.
                ensureActive()
                null
            } finally {
                currentJob.value = null
                cancelled.update { it - sync.id }
            }
        }

    private fun ExternalService.flag(name: String) = settings.getBoolean(settingsKey(name), false)

    /** Runs one sync and records it; the outcome is null when it failed or can't run. */
    // One linear catch per way a run can fail; splitting them would only scatter how each is recorded.
    @Suppress("CyclomaticComplexMethod")
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
                // long anyway. A run on the sync screen has the user there to leave it.
                val outcome = if (session is UnattendedSession) withTimeout(RUN_TIMEOUT) { sync() } else sync()
                outcome to outcome?.let { history.add(record.finished(it)) }
            } catch (error: LoginRequiredException) {
                record.failed(SyncResult.LOGIN_REQUIRED, error)
            } catch (error: LoginTimeoutException) {
                record.failed(SyncResult.LOGIN_REQUIRED, error)
            } catch (error: ServiceReadException) {
                record.failed(SyncResult.FAILED, error)
            } catch (error: LibraryLoadException) {
                record.failed(SyncResult.FAILED, error)
            } catch (error: IgdbUnavailableException) {
                record.failed(SyncResult.FAILED, error)
            } catch (error: ScriptWriteException) {
                record.failed(SyncResult.FAILED, error)
            } catch (error: TimeoutCancellationException) {
                record.failed(SyncResult.FAILED, error)
            } catch (error: UserCancellation) {
                record.failed(SyncResult.CANCELLED, error)
                throw error
            } catch (error: CancellationException) {
                // Leaving the sync screen records nothing; a background run the system stops does.
                if (session is UnattendedSession) record.failed(SyncResult.INTERRUPTED, error)
                throw error
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

/** The user stopped a sync ([SyncCoordinator.cancelCurrent], [SyncCoordinator.cancelManual]), not the system. */
class UserCancellation : CancellationException("Cancelled by the user")

/** One sync of [SyncCoordinator.runDue]; the [id] tells apart the same service and kind queued twice. */
data class DueSync(val service: ExternalService, val kind: SyncKind, val id: String = Uuid.random().toString())

/** Where a background sync is: signing in, reading the service, or writing to it ([done] of [total], 0 = unknown). */
data class SyncStep(val stage: SyncStage, val done: Int = 0, val total: Int = 0)

enum class SyncStage { SIGNING_IN, READING, WRITING }

/**
 * The background syncs: the [current] one (its session starting up included) with when it [startedAt] (epoch millis)
 * and its [step], the ones [waiting] after it in order, and how many are [done] since the queue was last empty, to
 * count them as X of [total].
 */
data class SyncQueue(
    val current: DueSync? = null,
    val waiting: List<DueSync> = emptyList(),
    val done: Int = 0,
    val startedAt: Long? = null,
    val step: SyncStep? = null,
) {
    val total: Int get() = done + waiting.size + if (current != null) 1 else 0
    val isEmpty: Boolean get() = current == null && waiting.isEmpty()

    /** Ends the current sync, if any, and starts [sync], taken from the waiting ones; null when none follows yet. */
    internal fun next(sync: DueSync?): SyncQueue = copy(
        current = sync,
        waiting = if (sync == null) waiting else waiting - sync,
        done = done + if (current != null) 1 else 0,
        startedAt = sync?.let { Clock.System.now().toEpochMilliseconds() },
        step = null,
    ).settled()

    /** Starts the count over once nothing is left. */
    internal fun settled(): SyncQueue = if (isEmpty) SyncQueue() else this
}
