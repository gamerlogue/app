package it.maicol07.gamerlogue.core

import android.content.Context
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.notification.PayloadData
import com.mmk.kmpnotifier.push.PushListener
import com.mmk.kmpnotifier.push.firebase.addPushListener
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * The pushes, on the channel of their type. In the background the system shows them on the channel the backend
 * names; in the foreground FCM hands them to the app, and KMPNotifier would put them all on its one channel, so
 * KMPNotifier must be initialized with `showPushNotification = false` and this shows them instead. Also creates the
 * channels, so the system settings list them all from the first start.
 */
fun showForegroundPushes(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    scope.launch { createNotificationChannels(context) }
    KMPNotifier.addPushListener(object : PushListener {
        override fun onPushNotificationWithPayloadData(title: String?, body: String?, data: PayloadData) {
            // Also called, with no title and body, when the user taps a push the system showed: nothing to show.
            if (title == null && body == null) return
            scope.launch {
                KoinPlatform.getKoin().get<LocalNotifier>().notify(
                    NotificationChannel.ofNotificationType(data["type"]?.toString()),
                    tag = "push.${data["notification_id"] ?: System.currentTimeMillis()}",
                    title = title.orEmpty(),
                    text = body.orEmpty(),
                    target = RootNavTree.Notifications,
                )
            }
        }
    })
}
