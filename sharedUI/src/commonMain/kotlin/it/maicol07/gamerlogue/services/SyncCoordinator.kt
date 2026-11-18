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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

/**
 * Runs the enabled syncs with no one watching and records every sync in the [SyncHistory].
 *
 * While [watch] is collected (the app is open), a library change the user made pushes it, after [PUSH_DELAY] of
 * quiet: the library sync of every tracker that has it on and, for a backlog game, the wishlist sync of every service
 * that has it on. Every [PERIOD_KEY] hours (default [DEFAULT_PERIOD_HOURS], 0 = off) both kinds run for all of them.
 * A tracker that never went through the reviewed first sync is left out: nothing is written unattended before it.
 * Background runs go one at a time through the [BackgroundWebHost]; a service never runs two syncs at once.
 */
@Single
class SyncCoordinator(
    private val settings: ObservableSettings,
    private val runner: SyncRunner,
    private val history: SyncHistory,
    private val host: BackgroundWebHost,
    private val libraryChanges: LibraryChanges,
) {
    // Outlives the screens: a run started while the app is open finishes even if the user leaves.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val locks = ExternalService.entries.associateWith { Mutex() }
    private val background = Mutex()

    /** The services syncing right now. */
    val running: StateFlow<Set<ExternalService>>
        field = MutableStateFlow(emptySet())

    /** A sync the user started on the sync screen, recorded as already seen. Null when it failed or can't run. */
    suspend fun runManual(service: ExternalService, kind: SyncKind, session: WebSession): SyncOutcome? =
        run(service, kind, SyncTrigger.MANUAL, session)

    /** Pushes library changes and runs the periodic sync until cancelled. */
    @OptIn(FlowPreview::class, ExperimentalSettingsApi::class)
    suspend fun watch() {
        if (!isServiceSyncSupported) return
        coroutineScope {
            launch {
                var wishlistDue = false
                libraryChanges.changes.filterIsInstance<LibraryChange.Saved>()
                    .onEach { if (it.status == GameLibraryStatus.BACKLOG) wishlistDue = true }
                    .debounce(PUSH_DELAY)
                    .collect {
                        runInBackground(SyncTrigger.CHANGE, wishlist = wishlistDue)
                        wishlistDue = false
                    }
            }
            launch {
                settings.getIntFlow(PERIOD_KEY, DEFAULT_PERIOD_HOURS).collectLatest { hours ->
                    if (hours <= 0) return@collectLatest
                    while (true) {
                        val next = settings.getLong(LAST_PERIODIC_KEY, 0L).milliseconds + hours.hours
                        delay(next - Clock.System.now().toEpochMilliseconds().milliseconds)
                        settings.putLong(LAST_PERIODIC_KEY, Clock.System.now().toEpochMilliseconds())
                        runInBackground(SyncTrigger.PERIODIC, wishlist = true)
                    }
                }
            }
        }
    }

    private fun runInBackground(trigger: SyncTrigger, wishlist: Boolean) = scope.launch {
        val due = ExternalService.entries.filter { it.flag(CONNECTED) }.flatMap { service ->
            listOfNotNull(
                SyncKind.LIBRARY.takeIf { service.flag(LIBRARY_SYNC) },
                SyncKind.WISHLIST.takeIf { wishlist && service.flag(WISHLIST_SYNC) },
            ).filter { runner.runsUnattended(service, it) }.map { service to it }
        }
        if (due.isEmpty()) return@launch
        background.withLock {
            host.withSession { session ->
                due.forEach { (service, kind) -> run(service, kind, trigger, UnattendedSession(session)) }
            }
        }
    }

    private fun ExternalService.flag(name: String) = settings.getBoolean(settingsKey(name), false)

    private suspend fun run(service: ExternalService, kind: SyncKind, trigger: SyncTrigger, session: WebSession): SyncOutcome? =
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
                outcome?.also { history.add(record.finished(it)) }
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
            )
            else -> run(SyncResult.OK)
        }

        /** Records the failure; the run's caller gets null. */
        fun failed(result: SyncResult, error: Exception): SyncOutcome? {
            Logger.e(error) { "$kind sync of $service failed" }
            history.add(run(result).copy(error = error.message))
            return null
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
        private const val LAST_PERIODIC_KEY = "sync.lastPeriodicAt"
        private val PUSH_DELAY = 90.seconds
        private val RUN_TIMEOUT = 10.minutes
    }
}
