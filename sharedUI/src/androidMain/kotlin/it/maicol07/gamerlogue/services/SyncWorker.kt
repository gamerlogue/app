package it.maicol07.gamerlogue.services

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.di.startKoinIfNeeded
import org.koin.mp.KoinPlatform
import java.util.concurrent.TimeUnit
import kotlin.time.toJavaDuration

/**
 * Runs the background syncs due for its trigger, with the app open or closed. Started while the app is on screen,
 * it turns into a foreground service, so the process isn't frozen mid-sync when the user leaves; started in the
 * background (Android forbids a foreground service then), it's a plain job, which the system bounds to about ten
 * minutes. Nothing runs for a signed-out user.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val notifications = SyncNotifications(context)

    override suspend fun doWork(): Result {
        startKoinIfNeeded(applicationContext)
        val koin = KoinPlatform.getKoin()
        if (!koin.get<AuthTokenProvider>().session.value.isAuthenticated) return Result.success()
        val trigger = inputData.getString(KEY_TRIGGER)?.let(SyncTrigger::valueOf) ?: SyncTrigger.PERIODIC
        val wishlist = inputData.getBoolean(KEY_WISHLIST, true)
        val runs = koin.get<SyncCoordinator>().runDue(trigger, wishlist) { service, index, total ->
            val notification = notifications.progress(service, index, total)
            if (index == 0 && appOnScreen()) keepAlive(notification) else notifications.show(notification)
        }
        notifications.finished(runs)
        return Result.success()
    }

    // Asked for an expedited run before Android 12, which shows it as a foreground service.
    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(notifications.starting())

    /** Becomes a foreground service, allowed only while the app is on screen; else stays a job, notified as usual. */
    private suspend fun keepAlive(notification: android.app.Notification) {
        try {
            setForeground(foregroundInfo(notification))
        } catch (error: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException: the app left the screen in the meantime.
            Logger.w(error) { "Sync can't run as a foreground service" }
            notifications.show(notification)
        }
    }

    private fun foregroundInfo(notification: android.app.Notification) =
        ForegroundInfo(SyncNotifications.PROGRESS_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

    private fun appOnScreen(): Boolean {
        val state = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(state)
        return state.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }

    companion object {
        const val KEY_TRIGGER = "trigger"
        const val KEY_WISHLIST = "wishlist"
    }
}

/**
 * Fires [SyncCoordinator.PUSH_DELAY] after the last library change (each change puts it off again) and hands the push
 * to [SyncWorker]. A job of its own, so the wait survives the app leaving the screen, when its process gets frozen.
 */
class SyncTriggerWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val wishlist = inputData.getBoolean(SyncWorker.KEY_WISHLIST, false)
        wishlistDue = false
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(
                workDataOf(SyncWorker.KEY_TRIGGER to SyncTrigger.CHANGE.name, SyncWorker.KEY_WISHLIST to wishlist),
            )
            .build()
        // After a run still going, never in its place: replacing it would stop it mid-write.
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(PUSH_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        return Result.success()
    }
}

// Whether a change waiting to be pushed touched the wishlist: replacing the wait must not forget it.
// ponytail: lost if the process dies before the push, which then skips the wishlists until the next periodic sync.
@Volatile
private var wishlistDue = false

/**
 * Hands the background syncs to WorkManager, so they run with the app closed too: the periodic one as unique periodic
 * work (online, battery not low), a library change through [SyncTriggerWorker].
 */
class WorkManagerSyncScheduler(context: Context) : SyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun changed(wishlist: Boolean) {
        wishlistDue = wishlistDue || wishlist
        val request = OneTimeWorkRequestBuilder<SyncTriggerWorker>()
            .setInitialDelay(SyncCoordinator.PUSH_DELAY.toJavaDuration())
            .setInputData(workDataOf(SyncWorker.KEY_WISHLIST to wishlistDue))
            .build()
        workManager.enqueueUniqueWork(TRIGGER_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    override fun period(hours: Int) {
        if (hours <= 0) {
            workManager.cancelUniqueWork(PERIODIC_WORK)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(hours.toLong(), TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInputData(
                workDataOf(SyncWorker.KEY_TRIGGER to SyncTrigger.PERIODIC.name, SyncWorker.KEY_WISHLIST to true),
            )
            .build()
        // UPDATE keeps the schedule of the work already there, so reopening the app doesn't push the next sync back.
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

private const val TRIGGER_WORK = "sync.change.wait"
private const val PUSH_WORK = "sync.change"
private const val PERIODIC_WORK = "sync.periodic"
