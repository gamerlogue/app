package it.maicol07.gamerlogue.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.sync__notification_attention
import gamerlogue.sharedui.generated.resources.sync__notification_channel
import gamerlogue.sharedui.generated.resources.sync__notification_conflicts
import gamerlogue.sharedui.generated.resources.sync__notification_failed
import gamerlogue.sharedui.generated.resources.sync__notification_login
import gamerlogue.sharedui.generated.resources.sync__notification_progress
import gamerlogue.sharedui.generated.resources.sync__notification_removed
import gamerlogue.sharedui.generated.resources.sync__notification_running
import gamerlogue.sharedui.generated.resources.sync__notification_to_confirm
import it.maicol07.gamerlogue.ui.views.settings.categories.labelRes
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/** Extra of the launch intent that opens the sync history, set by the sync notifications. */
const val EXTRA_OPEN_SYNC_HISTORY = "it.maicol07.gamerlogue.OPEN_SYNC_HISTORY"

/**
 * The notifications of the background syncs, on a silent channel: one while they run (a Live Update where the system
 * promotes it), and one at the end only when a result needs a look. Both open the sync history.
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

    fun show(notification: Notification) = notify(PROGRESS_ID, notification)

    /**
     * The runs ended: the progress goes, and the ones that need a look get a notification of their own. Games left to
     * the user count only when the [previous] run of the same sync didn't list them: they'd be notified every time.
     */
    suspend fun finished(runs: List<SyncRun>, previous: (SyncRun) -> SyncRun?) {
        manager.cancel(PROGRESS_ID)
        val lines = runs.flatMap { it.attention(previous(it)) }
        if (lines.isEmpty()) return
        val style = NotificationCompat.InboxStyle()
        lines.forEach(style::addLine)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(getString(Res.string.sync__notification_attention))
            .setContentText(lines.first())
            .setStyle(style)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(openHistory())
            .build()
        notify(ATTENTION_ID, notification)
    }

    /**
     * What of [this] run needs a look, after the [before] one, as notification lines: a failed run has only its failure;
     * else one line for each kind of thing to look at, none when nothing is.
     */
    private suspend fun SyncRun.attention(before: SyncRun?): List<String> {
        val name = getString(service.labelRes())
        val removed = (removedOnService + removedHere).map { it.gameId } -
            before?.let { (it.removedOnService + it.removedHere).map { g -> g.gameId }.toSet() }.orEmpty()
        val toConfirm = toConfirm.map { it.uid } - before?.toConfirm?.map { it.uid }.orEmpty().toSet()
        suspend fun count(resource: PluralStringResource, n: Int) =
            if (n > 0) getPluralString(resource, n, name, n) else null
        return when (result) {
            SyncResult.LOGIN_REQUIRED -> listOf(getString(Res.string.sync__notification_login, name))
            SyncResult.FAILED -> listOf(getString(Res.string.sync__notification_failed, name))
            else -> listOfNotNull(
                count(Res.plurals.sync__notification_removed, removed.size),
                count(Res.plurals.sync__notification_to_confirm, toConfirm.size),
                count(Res.plurals.sync__notification_conflicts, conflicts),
            )
        }
    }

    private suspend fun builder(title: String): NotificationCompat.Builder {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setRequestPromotedOngoing(true)
            .setContentIntent(openHistory())
    }

    private suspend fun ensureChannel() = manager.createNotificationChannel(
        NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(getString(Res.string.sync__notification_channel))
            .build(),
    )

    private fun openHistory(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.putExtra(EXTRA_OPEN_SYNC_HISTORY, true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    // Without the permission (Android 13+, not granted yet) the syncs run anyway, just without notifications.
    private fun notify(id: Int, notification: Notification) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (allowed) manager.notify(id, notification)
    }

    companion object {
        const val PROGRESS_ID = 4401
        private const val ATTENTION_ID = 4402
        private const val CHANNEL = "sync"
    }
}
