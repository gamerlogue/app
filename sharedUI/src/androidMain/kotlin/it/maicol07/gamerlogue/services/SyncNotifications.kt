package it.maicol07.gamerlogue.services

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.sync__notification_channel
import gamerlogue.sharedui.generated.resources.sync__notification_progress
import gamerlogue.sharedui.generated.resources.sync__notification_running
import it.maicol07.gamerlogue.core.NotificationChannel
import it.maicol07.gamerlogue.core.canPostNotifications
import it.maicol07.gamerlogue.core.createNotificationChannels
import it.maicol07.gamerlogue.core.openAppIntent
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.views.settings.categories.labelRes
import org.jetbrains.compose.resources.getString

/**
 * The progress of the background syncs, on the silent sync channel, while they run (a Live Update where the system
 * promotes it); it opens the sync history.
 */
class SyncNotifications(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    /** The notification while a run starts, before it knows its first service. */
    suspend fun starting(): Notification = builder(getString(Res.string.sync__notification_channel))
        .setProgress(0, 0, true)
        .build()

    /** The notification while [service] syncs, the [index]-th of [total]. */
    suspend fun progress(service: ExternalService, index: Int, total: Int): Notification {
        val step = index + 1
        return builder(getString(Res.string.sync__notification_running, getString(service.labelRes())))
            .setContentText(getString(Res.string.sync__notification_progress, step, total))
            .setShortCriticalText("$step/$total")
            .setStyle(
                NotificationCompat.ProgressStyle()
                    .setProgressSegments(List(total) { NotificationCompat.ProgressStyle.Segment(1) })
                    .setProgress(index),
            )
            .build()
    }

    fun show(notification: Notification) {
        if (canPostNotifications(context)) manager.notify(PROGRESS_ID, notification)
    }

    /** The runs ended: the progress goes; what needs a look is [SyncAlerts]'. */
    fun finished() = manager.cancel(PROGRESS_ID)

    private suspend fun builder(title: String): NotificationCompat.Builder {
        createNotificationChannels(context)
        return NotificationCompat.Builder(context, NotificationChannel.SYNC.id)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setRequestPromotedOngoing(true)
            .setContentIntent(openAppIntent(context, RootNavTree.SyncHistory, PROGRESS_ID))
    }

    companion object {
        const val PROGRESS_ID = 4401
    }
}
