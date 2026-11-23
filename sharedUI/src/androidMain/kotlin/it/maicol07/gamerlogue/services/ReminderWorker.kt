package it.maicol07.gamerlogue.services

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import it.maicol07.gamerlogue.di.startKoinIfNeeded
import org.koin.mp.KoinPlatform
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/** Notifies the reminders due, with the app open or closed; a failed release check is retried by WorkManager. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        startKoinIfNeeded(applicationContext)
        return if (KoinPlatform.getKoin().get<Reminders>().check()) Result.success() else Result.retry()
    }
}

/** The daily check as unique periodic work (online), each event's as a delayed one-time work. */
class WorkManagerReminderScheduler(context: Context) : ReminderScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun schedule(eventChecks: Collection<Long>) {
        val daily = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.KEEP, daily)
        val now = Clock.System.now().epochSeconds
        // ponytail: an unfollowed event's work stays and finds nothing due; cancelling would need its id here.
        for (at in eventChecks.filter { it > now }) {
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(at - now, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork("$EVENT_WORK.$at", ExistingWorkPolicy.KEEP, request)
        }
    }
}

private const val DAILY_WORK = "reminders.daily"
private const val EVENT_WORK = "reminders.event"
