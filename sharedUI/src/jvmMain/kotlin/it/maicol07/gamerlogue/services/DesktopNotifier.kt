package it.maicol07.gamerlogue.services

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import androidx.navigation3.runtime.NavKey
import it.maicol07.gamerlogue.core.LocalNotifier
import it.maicol07.gamerlogue.core.NotificationChannel

/**
 * Shows notifications as messages of the [SyncTray] icon, the system's own on every desktop with a tray. The tray
 * tells no tap apart, so they open nothing; and there are no channels: the system mutes the app as a whole.
 */
object DesktopNotifier : LocalNotifier {
    val tray = TrayState()

    override val isSupported = isTraySupported

    override suspend fun notify(channel: NotificationChannel, tag: String, title: String, text: String, target: NavKey?) =
        tray.sendNotification(Notification(title, text, Notification.Type.Info))
}
